const sentinelTokens = {
  colors: {
    void: "#010B13",
    panel: "#003153",
    panelDeep: "#021925",
    cyan: "#0FFFFF",
    cyanSoft: "#7DFDFD",
    teal: "#0A6B84",
    danger: "#DC343B",
    dangerSoft: "#FF969B",
    textPrimary: "#F8FAFC",
    textSecondary: "#94A3B8",
    textMuted: "#64748B",
    line: "#0FFFFF26",
    lineStrong: "#0FFFFF40",
    cyanFaint: "#0FFFFF0D",
    cyanGlowBar: "#0FFFFFCC",
    panelSoft: "#003153B8",
    panelTranslucent: "#0031538C",
    voidSoft: "#010B138C",
    mapLine: "#0FFFFF1A",
    dangerPanel: "#240F18BF",
    dangerLine: "#DC343B80",
    white: "#FFFFFF",
  },
  fonts: {
    ui: '"IBM Plex Sans", "Roboto", "Helvetica", Arial, sans-serif',
    mono: '"Inconsolata", "Roboto Mono", monospace',
  },
  shadows: {
    insetGlow: "0 0 18px rgba(15, 255, 255, 0.12), inset 0 0 30px rgba(15, 255, 255, 0.035)",
    cyanGlow: "0 0 18px rgba(15, 255, 255, 0.22)",
    dangerGlow: "0 0 22px rgba(220, 52, 59, 0.1)",
  },
  motion: {
    fast: "160ms",
    standard: "220ms",
    slow: "900ms",
  },
  grid: {
    tech: "28px 28px",
    map: "44px 44px",
  },
  textStyles: {
    label: {
      fontFamily: '"Inconsolata", "Roboto Mono", monospace',
      fontSize: "10px",
      letterSpacing: "0.16em",
      textTransform: "uppercase",
    },
    mono: {
      fontFamily: '"Inconsolata", "Roboto Mono", monospace',
    },
    body: {
      fontFamily: '"IBM Plex Sans", "Roboto", "Helvetica", Arial, sans-serif',
    },
  },
};

export const sentinelNavItems = [
  { label: "Tổng quan", icon: "Radar", path: "/sentinel" },
  { label: "Thiết bị", icon: "Device", path: "/devices" },
  { label: "Bản đồ", icon: "Map", path: "/map" },
  { label: "Lịch sử vị trí", icon: "History", path: "/history" },
];

export const sentinelTelemetry = {
  deviceName: "Galaxy S24 Ultra",
  model: "SM-S928B",
  androidVersion: "ANDROID 14",
  battery: 72,
  lastPing: "12 sec ago",
  accuracy: "4.2 m",
  zone: "Quận 1 · TP. Hồ Chí Minh",
  cellular: "5G / 84%",
  screenLock: "ARMED",
  gpsLock: "3D / 11 sats",
};

export const sentinelProofEvents = [
  { time: "14:24:08", meta: "10.7756, 106.7004", message: "GPS handshake acknowledged" },
  { time: "14:23:41", meta: "CELL-HCM-17", message: "5G network heartbeat received" },
  { time: "14:18:02", meta: "10.7763, 106.6992", message: "Geofence exited · informational" },
  {
    time: "14:22:07",
    meta: "SIM-0x91A",
    message: "SIM identity changed · review required",
    tone: "danger",
  },
];

export const sentinelDevices = [
  {
    id: "s24-ultra",
    name: "Galaxy S24 Ultra",
    model: "SM-S928B",
    platform: "Android 14",
    battery: 72,
    status: "Tracking active",
    lastSeen: "12 sec ago",
    location: "Quận 1 · TP. Hồ Chí Minh",
    network: "5G / 84%",
  },
  {
    id: "pixel-8",
    name: "Pixel 8 Pro",
    model: "GPJ41",
    platform: "Android 14",
    battery: 48,
    status: "Standby",
    lastSeen: "8 min ago",
    location: "Thảo Điền · TP. Hồ Chí Minh",
    network: "4G / 61%",
  },
];

export const sentinelHistoryEvents = [
  {
    id: "gps-1424",
    deviceId: "s24-ultra",
    timestamp: "14:24:08",
    type: "GPS",
    location: "10.7756, 106.7004",
    detail: "GPS handshake acknowledged",
    tone: "success",
  },
  {
    id: "wifi-1402",
    deviceId: "s24-ultra",
    timestamp: "14:02:44",
    type: "WIFI",
    location: "Quận 1 · TP. Hồ Chí Minh",
    detail: "Known Wi-Fi network lost",
    tone: "danger",
  },
  {
    id: "geo-1418",
    deviceId: "s24-ultra",
    timestamp: "14:18:02",
    type: "GEOFENCE",
    location: "Phường Bến Nghé → Quận 1",
    detail: "Device exited safe zone",
    tone: "default",
  },
  {
    id: "gps-1312",
    deviceId: "pixel-8",
    timestamp: "13:12:16",
    type: "GPS",
    location: "Thảo Điền · TP. Hồ Chí Minh",
    detail: "Location ping confirmed",
    tone: "success",
  },
  {
    id: "sim-1222",
    deviceId: "s24-ultra",
    timestamp: "12:22:07",
    type: "SIM",
    location: "CELL-HCM-17",
    detail: "SIM identity changed · review required",
    tone: "danger",
  },
];

export default sentinelTokens;
