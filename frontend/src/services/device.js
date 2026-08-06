const API_BASE = process.env.REACT_APP_API_BASE || "";

async function secureRequest(path, method = "GET", body = null, token = "") {
  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
    "ngrok-skip-browser-warning": "true",
  };

  const options = {
    method,
    headers,
  };

  if (body) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(`${API_BASE}${path}`, options);
  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(data.message || data.error || "Request failed");
  }
  return data;
}

export async function getDeviceList(username, token) {
  return await secureRequest(`/api/${username}/location/devices/status`, "GET", null, token);
}

export async function removeDevice(username, deviceId, token) {
  return await secureRequest(`/api/${username}/location/${deviceId}`, "DELETE", null, token);
}

export async function triggerDiscoveryPulse(username, token) {
  return await secureRequest(
    `/api/${username}/location`,
    "POST",
    {
      deviceId: "Web-Portal-" + Math.random().toString(36).substring(7),
      deviceName: "Web Portal",
      latitude: 0,
      longitude: 0,
    },
    token
  );
}

export async function toggleLostMode(username, active, message, phone, token) {
  return await secureRequest(
    `/api/${username}/lost-mode`,
    "POST",
    { active, message, phoneNumber: phone },
    token
  );
}

export async function toggleAlarm(username, active, token) {
  return await secureRequest(`/api/${username}/alarm`, "POST", { active }, token);
}

export async function requestTracking(username, token) {
  return await secureRequest(`/api/${username}/track`, "POST", { active: true }, token);
}

export async function setGeofence(username, latitude, longitude, radius, token) {
  return await secureRequest(
    `/api/${username}/geofence`,
    "POST",
    { latitude, longitude, radius },
    token
  );
}

export async function getSecurityEvents(username, token) {
  return await secureRequest(`/api/${username}/security-events`, "GET", null, token);
}

export default {
  getDeviceList,
  removeDevice,
  triggerDiscoveryPulse,
  toggleLostMode,
  toggleAlarm,
  requestTracking,
  setGeofence,
  getSecurityEvents,
};
