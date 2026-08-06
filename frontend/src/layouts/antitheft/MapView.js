/* eslint-disable react/prop-types, prettier/prettier */
import { useMemo, useState, useEffect, useRef } from "react";

import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import { MapContainer, TileLayer, Marker, Popup, Circle, Polyline, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

import SentinelPageFrame from "layouts/sentinel/SentinelPageFrame";
import { IconGlyph, SentinelSurface, colors, fonts, textStyles } from "layouts/sentinel";
import { getCurrentUser } from "services/auth";
import deviceService from "services/device";
import { io } from "socket.io-client";
import { useLanguage } from "utils/i18n";

// --- CUSTOM COMPONENTS ---

/**
 * Component to handle map center and size invalidation
 */
function MapController({ center, zoom }) {
  const map = useMap();
  useEffect(() => {
    let timeout;
    if (center && center[0] !== 0) {
      map.setView(center, zoom);
    }
    // Fix rendering issue
    timeout = setTimeout(() => {
      if (map) map.invalidateSize();
    }, 500);

    return () => clearTimeout(timeout);
  }, [center, zoom, map]);
  return null;
}

const deviceIcon = new L.DivIcon({
  className: "device-map-pin",
  html: `<div style="width: 18px; height: 18px; border-radius: 50%; background: ${colors.cyan}; border: 2px solid #fff; box-shadow: 0 0 18px ${colors.cyan};"></div>`,
  iconSize: [18, 18],
  iconAnchor: [9, 9],
});

const safeIcon = new L.DivIcon({
  className: "safe-map-pin",
  html: `<div style="width: 14px; height: 14px; border-radius: 50%; background: ${colors.teal}; border: 2px solid #fff; box-shadow: 0 0 12px ${colors.teal};"></div>`,
  iconSize: [14, 14],
  iconAnchor: [7, 7],
});

function MapView() {
  const { t } = useLanguage();
  const [zoom, setZoom] = useState(16);
  const [layer, setLayer] = useState("street");
  const user = getCurrentUser();
  const socketRef = useRef(null);

  const [devices, setDevices] = useState({}); // deviceId -> data
  const [safeZone, setSafeZone] = useState(null);
  const [lastUpdate, setLastUpdate] = useState("Never");
  const [isLoading, setIsLoading] = useState(true);

  // Derive display values
  const deviceList = Object.values(devices);
  const mainDevice = deviceList[0] || null;
  const deviceLocation = mainDevice
    ? [mainDevice.lastLatitude, mainDevice.lastLongitude]
    : [10.762622, 106.660172];

  useEffect(() => {
    if (!user) return;

    // 1. Fetch initial state
    const init = async () => {
      try {
        const list = await deviceService.getDeviceList(user.username, user.token);
        // Filter out Web Portal entries
        const mobileList = list.filter(
          (d) => !d.deviceName?.includes("Web") && !d._id?.startsWith("web-")
        );

        const devMap = {};
        mobileList.forEach((d) => {
          if (d.lastLatitude !== 0) devMap[d._id] = d;
        });
        setDevices(devMap);

        const status = await fetch(
          `${process.env.REACT_APP_API_BASE}/api/${user.username}/status`,
          {
            headers: {
              Authorization: `Bearer ${user.token}`,
              "ngrok-skip-browser-warning": "true",
            },
          }
        ).then((r) => r.json());

        if (status.safeZone && status.safeZone.lat) {
          setSafeZone({
            lat: status.safeZone.lat,
            lon: status.safeZone.lon,
            radius: status.safeZone.radius,
          });
        }

        setIsLoading(false);
      } catch (e) {
        console.error("Map init failed", e);
        setIsLoading(false);
      }
    };

    init();

    // 2. Initialize Socket
    if (!socketRef.current) {
      const apiBase = process.env.REACT_APP_API_BASE || "";
      socketRef.current = io(apiBase, {
        auth: { token: user.token },
        extraHeaders: { "ngrok-skip-browser-warning": "true" },
      });

      socketRef.current.on("connect", () => {
        socketRef.current.emit("join", user.username.toLowerCase());
        deviceService.requestTracking(user.username, user.token);
      });

      socketRef.current.on("locationUpdate", (data) => {
        if (data.latitude === 0) return;
        setDevices((prev) => ({
          ...prev,
          [data.deviceId]: {
            ...prev[data.deviceId],
            lastLatitude: data.latitude,
            lastLongitude: data.longitude,
            deviceName: data.deviceName || prev[data.deviceId]?.deviceName,
            batteryLevel: data.batteryLevel,
            isCharging: data.isCharging,
            lastTimestamp: new Date().toISOString(),
          },
        }));
        setLastUpdate(new Date().toLocaleTimeString());
      });

      socketRef.current.on("statusUpdate", () => init());
    }

    return () => {
      if (socketRef.current) {
        socketRef.current.disconnect();
        socketRef.current = null;
      }
    };
  }, []);

  const handleMapClick = async (e) => {
    const { lat, lng } = e.latlng;
    if (
      window.confirm(t("geofenceConfirm", `Thiết lập Vùng an toàn tại vị trí này (Bán kính 200m)?`))
    ) {
      try {
        await deviceService.setGeofence(user.username, lat, lng, 200, user.token);
        setSafeZone({ lat, lon: lng, radius: 200 });
        alert(t("geofenceSuccess", "Vùng an toàn đã được cập nhật!"));
      } catch (err) {
        alert(err.message);
      }
    }
  };

  const mapTiles = useMemo(
    () =>
      layer === "street"
        ? {
            url: "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
            attribution:
              '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
          }
        : {
            url: "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
            attribution: "Tiles &copy; Esri",
          },
    [layer]
  );

  return (
    <SentinelPageFrame>
      <Stack spacing={2.5}>
        <Stack
          direction={{ xs: "column", sm: "row" }}
          justifyContent="space-between"
          alignItems={{ xs: "flex-start", sm: "flex-end" }}
          spacing={2}
        >
          <Stack spacing={0.75}>
            <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>
              07 / ORBITAL TRACKING
            </Typography>
            <Typography
              component="h1"
              variant="h4"
              sx={{ color: colors.white, fontFamily: fonts.mono, fontWeight: 600 }}
            >
              {t("map")}
            </Typography>
            <Typography variant="body2" sx={{ color: colors.textSecondary }}>
              {t("lastUpdate")}: {lastUpdate} •{" "}
              {t("mapInstruction", "Nhấp vào bản đồ để đặt Vùng an toàn.")}
            </Typography>
          </Stack>

          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              startIcon={<IconGlyph name="layers" size={17} />}
              onClick={() => setLayer(layer === "street" ? "satellite" : "street")}
              sx={{
                borderRadius: "0px",
                borderColor: `${colors.cyan}80`,
                color: colors.cyan,
                textTransform: "none",
                "&:hover": { borderColor: colors.cyan, backgroundColor: colors.cyanFaint },
              }}
            >
              {layer === "street"
                ? t("streetLayer", "Street layer")
                : t("satelliteLayer", "Satellite layer")}
            </Button>
            <Button
              component="a"
              href={`https://www.google.com/maps?q=${deviceLocation[0]},${deviceLocation[1]}`}
              target="_blank"
              rel="noreferrer"
              variant="outlined"
              disabled={!mainDevice}
              endIcon={<IconGlyph name="open_in_new" size={16} />}
              sx={{
                borderRadius: "0px",
                borderColor: colors.line,
                color: colors.textSecondary,
                textTransform: "none",
                "&:hover": { borderColor: colors.cyan, color: colors.cyan },
              }}
            >
              {t("openMaps", "Open maps")}
            </Button>
          </Stack>
        </Stack>

        <Stack direction={{ xs: "column", lg: "row" }} spacing={2} alignItems="stretch">
          <SentinelSurface
            component="section"
            aria-label={t("map")}
            elevation={0}
            sx={{ flex: 1, minHeight: "650px", p: 2 }}
          >
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Stack direction="row" spacing={1} alignItems="center">
                <Box
                  sx={{
                    width: "8px",
                    height: "8px",
                    borderRadius: "50%",
                    backgroundColor: mainDevice ? colors.cyan : colors.textMuted,
                    boxShadow: `0 0 10px ${mainDevice ? colors.cyan : "transparent"}`,
                  }}
                />
                <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>
                  {t("liveTelemetry", "LIVE TELEMETRY")} /{" "}
                  {deviceList.length.toString().padStart(2, "0")}
                </Typography>
              </Stack>
              <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.textMuted }}>
                {deviceLocation[0].toFixed(4)}° N · {deviceLocation[1].toFixed(4)}° E
              </Typography>
            </Stack>

            <Box
              sx={{
                mt: 2,
                border: `1px solid ${colors.line}`,
                overflow: "hidden",
                minHeight: "520px",
                position: "relative",
              }}
            >
              {isLoading && (
                <Box
                  sx={{
                    position: "absolute",
                    zIndex: 2000,
                    top: 0,
                    left: 0,
                    width: "100%",
                    height: "100%",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    bgcolor: `${colors.void}80`,
                  }}
                >
                  <CircularProgress color="info" />
                </Box>
              )}
              <MapContainer
                center={deviceLocation}
                zoom={zoom}
                scrollWheelZoom
                style={{ height: "520px", width: "100%", background: "#0e1721" }}
                whenCreated={(map) => {
                  map.on("click", handleMapClick);
                }}
              >
                <TileLayer url={mapTiles.url} attribution={mapTiles.attribution} />
                <MapController center={deviceLocation} zoom={zoom} />

                {safeZone && (
                  <>
                    <Circle
                      center={[safeZone.lat, safeZone.lon]}
                      radius={safeZone.radius}
                      pathOptions={{
                        color: colors.teal,
                        fillColor: colors.teal,
                        fillOpacity: 0.12,
                        weight: 1.5,
                      }}
                    />
                    <Marker position={[safeZone.lat, safeZone.lon]} icon={safeIcon}>
                      <Popup>
                        {t("safeZoneCenter", "Safe zone center")} · {safeZone.radius}m radius
                      </Popup>
                    </Marker>
                  </>
                )}

                {deviceList.map((dev) => {
                  if (!dev || !dev.lastLatitude || !dev.lastLongitude) return null;
                  const timestamp = dev.lastTimestamp
                    ? new Date(dev.lastTimestamp).toLocaleTimeString()
                    : "N/A";

                  return (
                    <Marker
                      key={dev._id}
                      position={[dev.lastLatitude, dev.lastLongitude]}
                      icon={deviceIcon}
                    >
                      <Popup>
                        <strong>{dev.deviceName || dev._id}</strong>
                        <br />
                        {t("battery")}: {dev.batteryLevel}% {dev.isCharging ? "⚡" : ""}
                        <br />
                        {t("lastSync", "Last sync")}: {timestamp}
                      </Popup>
                    </Marker>
                  );
                })}
              </MapContainer>
            </Box>
          </SentinelSurface>

          <SentinelSurface
            component="aside"
            aria-label={t("locationIntelligence", "Thông tin vị trí")}
            elevation={0}
            sx={{ width: { xs: "100%", lg: "320px" }, p: 2.5 }}
          >
            <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>
              {t("locationIntelligence", "LOCATION INTELLIGENCE")}
            </Typography>
            <Stack spacing={2} sx={{ mt: 2.5 }}>
              <InfoBlock
                icon="smartphone"
                label={t("primaryDevice", "PRIMARY DEVICE")}
                value={mainDevice?.deviceName || t("searching", "Searching...")}
              />
              <InfoBlock
                icon="battery_charging_full"
                label={t("powerLevel", "POWER LEVEL")}
                value={mainDevice?.batteryLevel ? `${mainDevice.batteryLevel}%` : "N/A"}
              />
              <InfoBlock
                icon="gps_fixed"
                label={t("coordinates", "COORDINATES")}
                value={`${deviceLocation[0].toFixed(4)}, ${deviceLocation[1].toFixed(4)}`}
              />
              <InfoBlock
                icon="shield"
                label={t("safeZone")}
                value={
                  safeZone
                    ? `${t("armed", "Armed")} / ${safeZone.radius}m`
                    : t("notConfigured", "Not configured")
                }
              />
            </Stack>
            <Button
              href="/history"
              variant="outlined"
              fullWidth
              endIcon={<IconGlyph name="arrow_forward" size={16} />}
              sx={{
                mt: 3,
                borderRadius: "0px",
                borderColor: `${colors.cyan}80`,
                color: colors.cyan,
                textTransform: "none",
                "&:hover": { borderColor: colors.cyan, backgroundColor: colors.cyanFaint },
              }}
            >
              {t("viewLocationHistory")}
            </Button>
          </SentinelSurface>
        </Stack>
      </Stack>
    </SentinelPageFrame>
  );
}

function InfoBlock({ icon, label, value }) {
  return (
    <Stack
      direction="row"
      spacing={1.25}
      alignItems="flex-start"
      sx={{ borderBottom: `1px solid ${colors.line}`, pb: 1.5 }}
    >
      <IconGlyph name={icon} size={17} color={colors.cyan} />
      <Box>
        <Typography variant="caption" sx={{ ...textStyles.label, color: colors.textMuted }}>
          {label}
        </Typography>
        <Typography variant="body2" sx={{ mt: 0.5, color: colors.textPrimary, lineHeight: 1.5 }}>
          {value}
        </Typography>
      </Box>
    </Stack>
  );
}

export default MapView;
