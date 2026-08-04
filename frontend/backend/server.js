const express = require("express");
const cors = require("cors");
const fs = require("fs");
const path = require("path");
const dotenv = require("dotenv");
const nodemailer = require("nodemailer");
const twilio = require("twilio");

dotenv.config({ path: path.resolve(__dirname, ".env") });

const DATA_PATH = path.resolve(__dirname, "data.json");
const PORT = process.env.PORT || 5000;

function loadData() {
  try {
    if (!fs.existsSync(DATA_PATH)) {
      const initial = { users: [], pending: {} };
      fs.writeFileSync(DATA_PATH, JSON.stringify(initial, null, 2));
      return initial;
    }
    const raw = fs.readFileSync(DATA_PATH, "utf-8");
    return JSON.parse(raw);
  } catch (error) {
    console.error("Could not load backend data", error);
    return { users: [], pending: {} };
  }
}

function saveData(data) {
  try {
    fs.writeFileSync(DATA_PATH, JSON.stringify(data, null, 2));
  } catch (error) {
    console.error("Could not save backend data", error);
  }
}

function generateOtp() {
  return String(Math.floor(100000 + Math.random() * 900000));
}

function createTransport() {
  if (process.env.SMTP_HOST && process.env.SMTP_USER && process.env.SMTP_PASS) {
    return nodemailer.createTransport({
      host: process.env.SMTP_HOST,
      port: Number(process.env.SMTP_PORT || 587),
      secure: process.env.SMTP_SECURE === "true",
      auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASS,
      },
    });
  }
  return null;
}

const emailTransport = createTransport();
const twilioClient =
  process.env.TWILIO_ACCOUNT_SID && process.env.TWILIO_AUTH_TOKEN
    ? twilio(process.env.TWILIO_ACCOUNT_SID, process.env.TWILIO_AUTH_TOKEN)
    : null;

async function sendEmailOtp(email, otp) {
  if (!emailTransport) {
    console.warn("SMTP not configured, skipping actual email send");
    return { debugOtp: otp, warning: "Email provider not configured" };
  }

  await emailTransport.sendMail({
    from: process.env.SMTP_FROM || process.env.SMTP_USER,
    to: email,
    subject: "Your Smart Anti-Theft OTP code",
    text: `Your Smart Anti-Theft verification code is ${otp}. It expires in 15 minutes.`,
    html: `<p>Your Smart Anti-Theft verification code is <strong>${otp}</strong>.</p><p>It expires in 15 minutes.</p>`,
  });
  return {};
}

async function sendSmsOtp(phone, otp) {
  if (!twilioClient || !process.env.TWILIO_PHONE_NUMBER) {
    console.warn("Twilio not configured, skipping actual SMS send");
    return { debugOtp: otp, warning: "SMS provider not configured" };
  }

  await twilioClient.messages.create({
    body: `Your Smart Anti-Theft verification code is ${otp}.`,
    from: process.env.TWILIO_PHONE_NUMBER,
    to: phone,
  });
  return {};
}

async function sendOtp(entry) {
  const { method, email, phone, otp } = entry;
  if (method === "sms") {
    return await sendSmsOtp(phone, otp);
  }
  return await sendEmailOtp(email, otp);
}

const app = express();
app.use(cors());
app.use(express.json());

app.post("/api/auth/register", async (req, res) => {
  try {
    const { name, email, phone, password, method } = req.body;
    if (!name || !email || !phone || !password || !method) {
      return res.status(400).json({ message: "Missing required fields" });
    }

    const normalizedEmail = String(email).trim().toLowerCase();
    const normalizedPhone = String(phone).trim();
    const data = loadData();

    if (data.users.some((user) => user.email === normalizedEmail)) {
      return res.status(409).json({ message: "Email already registered" });
    }

    const otp = generateOtp();
    data.pending[normalizedEmail] = {
      name: name.trim(),
      email: normalizedEmail,
      phone: normalizedPhone,
      password,
      method,
      otp,
      createdAt: Date.now(),
      expiresAt: Date.now() + 15 * 60 * 1000,
    };

    saveData(data);
    const sendResult = await sendOtp(data.pending[normalizedEmail]);

    return res.json({ message: "OTP sent", ...sendResult });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: "Could not complete registration" });
  }
});

app.post("/api/auth/verify-otp", (req, res) => {
  try {
    const { email, otp } = req.body;
    if (!email || !otp) {
      return res.status(400).json({ message: "Missing email or OTP" });
    }

    const normalizedEmail = String(email).trim().toLowerCase();
    const data = loadData();
    const entry = data.pending[normalizedEmail];

    if (!entry) {
      return res.status(404).json({ message: "OTP not found or expired" });
    }
    if (entry.expiresAt < Date.now()) {
      delete data.pending[normalizedEmail];
      saveData(data);
      return res.status(410).json({ message: "OTP expired" });
    }
    if (entry.otp !== String(otp).trim()) {
      return res.status(400).json({ message: "Invalid OTP" });
    }

    const user = {
      id: Date.now(),
      name: entry.name,
      email: entry.email,
      phone: entry.phone,
      password: entry.password,
    };
    data.users.push(user);
    delete data.pending[normalizedEmail];
    saveData(data);

    return res.json({ message: "Registration complete", user: { id: user.id, name: user.name, email: user.email, phone: user.phone } });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: "Could not verify OTP" });
  }
});

app.post("/api/auth/resend-otp", async (req, res) => {
  try {
    const { email } = req.body;
    if (!email) {
      return res.status(400).json({ message: "Missing email" });
    }

    const normalizedEmail = String(email).trim().toLowerCase();
    const data = loadData();
    const entry = data.pending[normalizedEmail];

    if (!entry) {
      return res.status(404).json({ message: "No pending registration found" });
    }

    entry.otp = generateOtp();
    entry.expiresAt = Date.now() + 15 * 60 * 1000;
    saveData(data);
    const sendResult = await sendOtp(entry);

    return res.json({ message: "OTP re-sent", ...sendResult });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: "Could not resend OTP" });
  }
});

app.post("/api/auth/login", (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ message: "Missing email or password" });
    }

    const normalizedEmail = String(email).trim().toLowerCase();
    const data = loadData();
    const user = data.users.find((u) => u.email === normalizedEmail && u.password === password);

    if (!user) {
      return res.status(401).json({ message: "Invalid email or password" });
    }

    return res.json({ user: { id: user.id, name: user.name, email: user.email, phone: user.phone } });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: "Could not log in" });
  }
});

app.post("/api/auth/reset-demo", (req, res) => {
  try {
    const emptyStore = { users: [], pending: {} };
    saveData(emptyStore);
    return res.json({ message: "Demo account and virtual device data cleared" });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: "Could not reset demo data" });
  }
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`Backend running on port ${PORT}`);
});
