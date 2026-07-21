const CURRENT_KEY = "sat_current_user";
const API_BASE = process.env.REACT_APP_API_BASE || "";

async function request(path, body) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  const text = await response.text();
  let data;
  try {
    data = JSON.parse(text);
  } catch (error) {
    const message = text.trim().startsWith("<")
      ? "Server returned HTML instead of JSON. Check that the backend is running and the API path is correct."
      : `Invalid JSON response: ${text}`;
    throw new Error(message);
  }

  if (!response.ok) {
    throw new Error(data.message || "Request failed");
  }
  return data;
}

export async function startRegistration({ name, email, phone, password, method }) {
  const result = await request("/api/auth/register", {
    name,
    email,
    phone,
    password,
    method,
  });
  return result;
}

export async function verifyRegistrationOtp({ email, otp }) {
  const result = await request("/api/auth/verify-otp", { email, otp });
  return result;
}

export async function resendRegistrationOtp(email) {
  const result = await request("/api/auth/resend-otp", { email });
  return result;
}

export async function login({ email, password }) {
  const result = await request("/api/auth/login", { email, password });
  const user = result.user;
  localStorage.setItem(CURRENT_KEY, JSON.stringify(user));
  return user;
}

export function logout() {
  localStorage.removeItem(CURRENT_KEY);
}

export function getCurrentUser() {
  try {
    const raw = localStorage.getItem(CURRENT_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
}

export function changePassword() {
  throw new Error("Change password must be implemented with backend support");
}

export function sendPasswordReset() {
  throw new Error("Password reset must be implemented with backend support");
}

export default {
  startRegistration,
  verifyRegistrationOtp,
  resendRegistrationOtp,
  login,
  logout,
  getCurrentUser,
};
