const API_BASE = process.env.REACT_APP_API_BASE || "";

async function request(path, method = "GET", body = null, token = null) {
  const headers = {
    "Content-Type": "application/json",
    "ngrok-skip-browser-warning": "true",
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const options = {
    method,
    headers,
  };
  if (body) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(`${API_BASE}${path}`, options);
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.error || "Request failed");
  }
  return response.json();
}

export async function getDeviceList(username, token) {
  return request(`/api/${username}/status`, "GET", null, token);
}

export async function removeDevice(username, deviceId, token) {
  return request(`/api/${username}/location/${deviceId}`, "DELETE", null, token);
}

export async function triggerDiscoveryPulse(username, token) {
  // Website identifies as "Web Portal" to backend
  return request(`/api/${username}/location`, "POST", {
    deviceId: "web-browser-" + window.navigator.userAgent.slice(0, 15),
    deviceName: "Web Portal",
    latitude: 0,
    longitude: 0
  }, token);
}

export default {
  getDeviceList,
  removeDevice,
  triggerDiscoveryPulse,
};
