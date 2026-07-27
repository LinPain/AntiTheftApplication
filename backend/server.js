const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const http = require('http');
const { Server } = require('socket.io');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const nodemailer = require('nodemailer');
require('dotenv').config();

const User = require('./models/User');
const OTPLog = require('./models/OTPLog');
const locationRoutes = require('./routes/locationRoutes');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: { origin: "*", methods: ["GET", "POST"] }
});
app.set('io', io);

const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'your_super_secret_key';
const MOCK_OTP = process.env.MOCK_OTP === 'true';

// --- EMAIL CONFIG ---
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: { user: process.env.EMAIL_USER, pass: process.env.EMAIL_PASS }
});

const sendOTPEmail = async (email, code, type, username = 'User') => {
    let subject = 'Anti-Theft System - Authentication';
    let msgText = `Code: ${code}`;
    if (type === 'LOGIN') subject = 'Mã xác thực ĐĂNG NHẬP', msgText = `Mã: ${code}. Hiệu lực 10 phút.`;
    if (type === 'REGISTRATION') subject = 'Xác minh ĐĂNG KÝ', msgText = `Chào ${username}! Mã: ${code}`;
    if (type === 'RESET') subject = 'Đặt lại MẬT KHẨU', msgText = `Mã reset: ${code}`;

    await transporter.sendMail({
        from: process.env.EMAIL_USER, to: email,
        subject: `Anti-Theft System - ${subject}`, text: msgText
    });
};

// --- STATE & UTILS ---
let userStates = {};
function getUserState(username) {
    const key = username.toLowerCase().trim();
    if (!userStates[key]) userStates[key] = {
        alarm: { active: false, timestamp: 0 },
        lockdown: { active: false, timestamp: 0 },
        lostMode: { active: false, message: "LOST", phoneNumber: "", timestamp: 0 },
        trackRequest: { active: false, timestamp: 0 }
    };
    return userStates[key];
}

const createOTP = async (username, type, email) => {
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const identifier = username.toLowerCase().trim();
    await OTPLog.deleteMany({ identifier, type });
    await new OTPLog({ identifier, code, type, expiresAt: new Date(Date.now() + 600000) }).save();
    await sendOTPEmail(email, code, type, username);
    return code;
};

// --- MIDDLEWARE ---
app.use(cors());
app.use(express.json());

const authGuard = (req, res, next) => {
    const token = req.headers['authorization']?.split(' ')[1];
    if (!token) return res.status(401).json({ error: 'No token' });
    jwt.verify(token, JWT_SECRET, (err, decoded) => {
        if (err) return res.status(403).json({ error: 'Expired' });
        const reqUser = (req.params.username || "").toLowerCase().trim();
        const tokenUser = (decoded.username || "").toLowerCase().trim();
        if (reqUser && reqUser !== tokenUser) return res.status(403).json({ error: 'Forbidden' });
        req.user = decoded;
        next();
    });
};

// --- DIRECT AUTH ENDPOINTS (Flattened to avoid 404s) ---

app.post('/api/auth/register', async (req, res) => {
    try {
        let { username, password, email } = req.body;
        username = username.toLowerCase().trim(); email = email.toLowerCase().trim();
        if (await User.findOne({ $or: [{ username }, { email }] })) return res.status(400).json({ error: 'Exists' });
        const user = new User({ username, password: await bcrypt.hash(password, 10), email, isVerified: false });
        await user.save();
        const code = await createOTP(username, 'REGISTRATION', email);
        res.status(201).json({ verificationRequired: true, username, mockCode: MOCK_OTP ? code : undefined });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/auth/login', async (req, res) => {
    try {
        let { username, password, riskScore } = req.body;
        username = username.toLowerCase().trim();
        const user = await User.findOne({ $or: [{ username }, { email: username }] });
        if (!user || !(await bcrypt.compare(password, user.password))) return res.status(401).json({ error: 'Invalid' });
        if (!user.isVerified) return res.status(403).json({ error: 'Unverified', verificationRequired: true, username: user.username });
        if (riskScore > 70) return res.json({ lockdownRequired: true });
        const code = await createOTP(user.username, 'LOGIN', user.email);
        res.json({ mfaRequired: true, username: user.username, mockCode: MOCK_OTP ? code : undefined });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/auth/verify-otp', async (req, res) => {
    try {
        let { username, otp } = req.body;
        username = username.toLowerCase().trim();
        const user = await User.findOne({ $or: [{ username }, { email: username }] });
        const log = await OTPLog.findOne({ identifier: user.username.toLowerCase(), code: otp, type: 'LOGIN' });
        if (!log || log.expiresAt < new Date()) return res.status(401).json({ error: 'Invalid OTP' });
        const token = jwt.sign({ id: user._id, username: user.username }, JWT_SECRET, { expiresIn: '1h' });
        await OTPLog.deleteOne({ _id: log._id });
        res.json({ token, username: user.username });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/auth/verify-registration', async (req, res) => {
    try {
        let { username, otp } = req.body;
        username = username.toLowerCase().trim();
        const user = await User.findOne({ $or: [{ username }, { email: username }] });
        const log = await OTPLog.findOne({ identifier: user.username.toLowerCase(), code: otp, type: 'REGISTRATION' });
        if (!log || log.expiresAt < new Date()) return res.status(401).json({ error: 'Invalid' });
        user.isVerified = true; await user.save();
        await OTPLog.deleteOne({ _id: log._id });
        res.json({ message: 'Success' });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/auth/resend-otp', async (req, res) => {
    try {
        let { username, type } = req.body;
        const user = await User.findOne({ $or: [{ username }, { email: username }] });
        const code = await createOTP(user.username, type, user.email);
        res.json({ mockCode: MOCK_OTP ? code : undefined });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/auth/forgot-password', async (req, res) => {
    try {
        let { identifier } = req.body;
        if (!identifier) return res.status(400).json({ error: 'Identifier required' });
        identifier = identifier.toLowerCase().trim();
        const user = await User.findOne({ $or: [{ username: identifier }, { email: identifier }] });
        if (!user) return res.status(404).json({ error: 'User not found' });
        const code = await createOTP(user.username, 'RESET', user.email);
        res.json({ username: user.username, mockCode: MOCK_OTP ? code : undefined });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/auth/verify-reset', async (req, res) => {
    try {
        let { username, otp } = req.body;
        username = username.toLowerCase().trim();
        const user = await User.findOne({ $or: [{ username }, { email: username }] });
        const log = await OTPLog.findOne({ identifier: user.username.toLowerCase(), code: otp, type: 'RESET' });
        if (!log || log.expiresAt < new Date()) return res.status(401).json({ error: 'Invalid' });
        const resetToken = jwt.sign({ id: user._id, type: 'password_reset' }, JWT_SECRET, { expiresIn: '15m' });
        await OTPLog.deleteOne({ _id: log._id });
        res.json({ resetToken });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/auth/reset-password', async (req, res) => {
    try {
        const { resetToken, newPassword } = req.body;
        const decoded = jwt.verify(resetToken, JWT_SECRET);
        if (decoded.type !== 'password_reset') throw new Error('Invalid');
        const user = await User.findById(decoded.id);
        user.password = await bcrypt.hash(newPassword, 10);
        await user.save();
        res.json({ message: 'Success' });
    } catch (e) { res.status(401).json({ error: 'Invalid or expired' }); }
});

app.post('/api/auth/alert-risk', async (req, res) => {
    try {
        let { username, riskScore } = req.body;
        username = username.toLowerCase().trim();
        const user = await User.findOne({ username });
        if (!user) return res.status(404).json({ error: 'User not found' });
        await transporter.sendMail({
            from: process.env.EMAIL_USER, to: user.email,
            subject: 'Anti-Theft System - RISK ALERT',
            text: `High risk detected. Score: ${riskScore}`
        });
        res.json({ message: 'OK' });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/auth/alert-sim', async (req, res) => {
    try {
        let { username, operatorName } = req.body;
        username = username.toLowerCase().trim();
        const user = await User.findOne({ username });
        if (!user) return res.status(404).json({ error: 'User not found' });
        await transporter.sendMail({
            from: process.env.EMAIL_USER, to: user.email,
            subject: 'Anti-Theft System - SIM CHANGE ALERT',
            text: `SIM change detected. Carrier: ${operatorName}`
        });
        res.json({ message: 'OK' });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// --- PROTECTED API ---
app.use('/api/:username/location', (req, res, next) => {
    req.username = (req.params.username || "").toLowerCase().trim();
    authGuard(req, res, next);
}, locationRoutes);

app.get('/api/:username/status', authGuard, (req, res) => res.json(getUserState(req.params.username)));

app.post('/api/:username/lost-mode', authGuard, (req, res) => {
    const state = getUserState(req.params.username);
    state.lostMode = { active: !!req.body.active, message: req.body.message, phoneNumber: req.body.phoneNumber, timestamp: Date.now() };
    io.to(req.params.username.toLowerCase()).emit('statusUpdate');
    res.json({ message: 'OK' });
});

app.post('/api/:username/alarm', authGuard, (req, res) => {
    const state = getUserState(req.params.username);
    state.alarm = { active: !!req.body.active, timestamp: Date.now() };
    io.to(req.params.username.toLowerCase()).emit('statusUpdate');
    res.json({ message: 'OK' });
});

app.post('/api/:username/track', authGuard, (req, res) => {
    getUserState(req.params.username).trackRequest = { active: true, timestamp: Date.now() };
    io.to(req.params.username.toLowerCase()).emit('trackRequested');
    res.json({ message: 'OK' });
});

// --- STATIC & SOCKET ---
app.use(express.static('public'));
app.get('/', (req, res) => res.send('Active'));

io.use((socket, next) => {
    const token = socket.handshake.auth.token;
    if (!token) return next(new Error("Auth"));
    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) return next(new Error("Auth"));
        socket.user = user; next();
    });
});

io.on('connection', (socket) => {
    socket.on('join', (user) => {
        if (user.toLowerCase() === socket.user.username.toLowerCase()) socket.join(user.toLowerCase());
    });
});

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/zerotrustauth';
mongoose.connect(MONGODB_URI).then(() => {
    console.log('DB Connected');
    server.listen(PORT, () => console.log(`Listening on ${PORT}`));
});
