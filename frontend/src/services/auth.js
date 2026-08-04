const CURRENT_KEY = "sat_current_user";
const API_BASE = process.env.REACT_APP_API_BASE || "";

async function request(path, body) {
  const headers = {
    "Content-Type": "application/json",
  };

  const rawUser = localStorage.getItem(CURRENT_KEY);
  if (rawUser) {
    try {
      const user = JSON.parse(rawUser);
      if (user.token) {
        headers["Authorization"] = `Bearer ${user.token}`;
      }
    } catch (e) {}
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers,
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
    throw new Error(data.message || data.error || "Request failed");
  }
  return data;
}

export async function startRegistration({ username, password, email }) {
  return await request("/api/auth/register", { username, password, email });
}

export async function verifyRegistrationOtp({ username, otp }) {
  return await request("/api/auth/verify-registration", { username, otp });
}

export async function resendRegistrationOtp(username, type = "REGISTRATION") {
  return await request("/api/auth/resend-otp", { username, type });
}

export async function login({ email, password }) {
  // Mapping email field to username for backend compatibility
  return await request("/api/auth/login", { username: email, password, riskScore: 0 });
}

export async function verifyLoginOtp({ username, otp }) {
  return await request("/api/auth/verify-otp", { username, otp });
}

export async function forgotPassword(identifier) {
  return await request("/api/auth/forgot-password", { identifier });
}

export async function verifyReset(username, otp) {
  return await request("/api/auth/verify-reset", { username, otp });
}

export async function resetPassword(resetToken, newPassword) {
  return await request("/api/auth/reset-password", { resetToken, newPassword });
}

export async function changePassword({ username, oldPassword, newPassword }) {
  return await request("/api/auth/change-password", { username, oldPassword, newPassword });
}

export function logout() {
  localStorage.removeItem(CURRENT_KEY);
  // Remove navigation and event logic from here to keep it a pure service.
  // The UI component will handle the redirection.
}

export function getCurrentUser() {
  try {
    const raw = localStorage.getItem(CURRENT_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
}

export default {
  startRegistration,
  verifyRegistrationOtp,
  resendRegistrationOtp,
  login,
  logout,
  getCurrentUser,
  changePassword,
  forgotPassword,
  verifyReset,
  resetPassword,
  verifyLoginOtp,
};
