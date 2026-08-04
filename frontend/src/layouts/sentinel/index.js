/* eslint-disable react/prop-types, prettier/prettier */
import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import GlobalStyles from "@mui/material/GlobalStyles";
import Icon from "@mui/material/Icon";
import IconButton from "@mui/material/IconButton";
import LinearProgress from "@mui/material/LinearProgress";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import { keyframes, styled } from "@mui/material/styles";
import { MapContainer, TileLayer, Marker, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

// --- CUSTOM COMPONENTS ---

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

import sentinelTokens, {
  sentinelNavItems,
  sentinelProofEvents,
  sentinelTelemetry,
} from "layouts/sentinel/sentinelTokens";
import auth, { getCurrentUser } from "services/auth";
import deviceService from "services/device";
import LanguageSwitcher from "components/LanguageSwitcher";
import { useLanguage, t } from "utils/i18n";

const routeDraw = keyframes`
  from { stroke-dashoffset: 320; }
  to { stroke-dashoffset: 0; }
`;

const reveal = keyframes`
  from { opacity: 0; transform: translate3d(0, 16px, 0); }
  to { opacity: 1; transform: translate3d(0, 0, 0); }
`;

const radarPulse = keyframes`
  0%, 100% { transform: scale(0.84); opacity: 0.24; }
  50% { transform: scale(1.08); opacity: 0.72; }
`;

const markerPulse = keyframes`
  0% { transform: translate(-50%, -50%) scale(0.55); opacity: 0.65; }
  100% { transform: translate(-50%, -50%) scale(1.5); opacity: 0; }
`;

const livePulse = keyframes`
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.36); opacity: 0.52; }
`;

const gridDrift = keyframes`
  from { background-position: 0 0, 0 0; }
  to { background-position: 44px 44px, 44px 44px; }
`;

const SentinelSurface = styled(Paper)(() => ({
  borderRadius: "0px",
  backgroundColor: sentinelTokens.colors.panelTranslucent,
  border: `1px solid ${sentinelTokens.colors.line}`,
  boxShadow: sentinelTokens.shadows.insetGlow,
  color: sentinelTokens.colors.textPrimary,
}));

const IconRailButton = styled(IconButton)(() => ({
  width: "44px",
  height: "44px",
  borderRadius: "0px",
  color: sentinelTokens.colors.textMuted,
  transition: `transform ${sentinelTokens.motion.fast} ease, color ${sentinelTokens.motion.fast} ease, border-color ${sentinelTokens.motion.fast} ease, background-color ${sentinelTokens.motion.fast} ease`,
  "&:hover, &:focus-visible": {
    color: sentinelTokens.colors.cyan,
    borderColor: `${sentinelTokens.colors.cyan}80`,
    backgroundColor: sentinelTokens.colors.cyanFaint,
    transform: "translateY(-2px) scale(1.04)",
  },
}));

const textStyles = sentinelTokens.textStyles;
const { colors, fonts, shadows } = sentinelTokens;

const panelSx = {
  borderRadius: "0px",
  backgroundColor: colors.panelTranslucent,
  border: `1px solid ${colors.line}`,
  boxShadow: shadows.insetGlow,
};

const monoLabelSx = {
  ...textStyles.label,
  color: colors.textMuted,
};

const IconGlyph = ({ name, size = 18, color = "inherit" }) => (
  <Icon aria-hidden="true" sx={{ color, fontSize: `${size}px`, lineHeight: 1 }}>
    {name}
  </Icon>
);

function SectionLabel({ children, color = colors.cyan }) {
  return (
    <Typography component="span" variant="caption" sx={{ ...textStyles.label, color }}>
      {children}
    </Typography>
  );
}

function Rail({ activePath }) {
  const navigate = useNavigate();
  const { language } = useLanguage();

  return (
    <Box
      component="aside"
      aria-label="Điều hướng chính"
      sx={{
        display: { xs: "none", md: "flex" },
        width: "76px",
        flexShrink: 0,
        flexDirection: "column",
        alignItems: "center",
        borderRight: `1px solid ${colors.line}`,
        backgroundColor: `${colors.void}F2`,
        py: 2.5,
      }}
    >
      <Box
        sx={{
          display: "flex",
          width: "40px",
          height: "40px",
          alignItems: "center",
          justifyContent: "center",
          border: `1px solid ${colors.cyan}99`,
          backgroundColor: colors.panel,
          color: colors.cyan,
          boxShadow: shadows.cyanGlow,
        }}
        title="Smart Anti-Theft"
      >
        <IconGlyph name="shield" size={22} />
      </Box>

      <Stack component="nav" spacing={1.25} sx={{ flex: 1, mt: 4, alignItems: "center" }}>
        {sentinelNavItems.map((item, index) => {
          const isActive = activePath === item.path || (index === 0 && activePath === "/");
          return (
            <Tooltip key={item.path} title={item.label} placement="right">
              <IconRailButton
                component={Link}
                to={item.path}
                aria-label={item.label}
                aria-current={isActive ? "page" : undefined}
                sx={
                  isActive
                    ? {
                        color: colors.cyan,
                        border: `1px solid ${colors.cyan}`,
                        backgroundColor: colors.cyanFaint,
                        boxShadow: shadows.cyanGlow,
                      }
                    : { border: "1px solid transparent" }
                }
              >
                <IconGlyph
                  name={
                    item.icon === "Radar"
                      ? "radar"
                      : item.icon === "Device"
                      ? "phone_android"
                      : item.icon === "Map"
                      ? "map"
                      : "route"
                  }
                  size={20}
                />
              </IconRailButton>
            </Tooltip>
          );
        })}
      </Stack>

      <Tooltip title={t("securitySettings")} placement="right">
        <IconRailButton
          aria-label={t("securitySettings")}
          onClick={() => navigate("/account")}
          sx={{ border: "1px solid transparent" }}
        >
          <IconGlyph name="settings" size={20} />
        </IconRailButton>
      </Tooltip>
    </Box>
  );
}

function TopBar() {
  const navigate = useNavigate();
  const { language } = useLanguage();
  const [currentUser, setCurrentUser] = useState(() => auth.getCurrentUser());

  useEffect(() => {
    const syncUser = () => setCurrentUser(auth.getCurrentUser());
    syncUser();
    window.addEventListener("storage", syncUser);
    return () => window.removeEventListener("storage", syncUser);
  }, []);

  const accountName = currentUser?.name || "Khách";

  return (
    <Box
      component="header"
      data-reveal
      sx={{
        display: "flex",
        flexDirection: { xs: "column", lg: "row" },
        alignItems: { xs: "stretch", lg: "flex-end" },
        justifyContent: "space-between",
        gap: 2,
        borderBottom: `1px solid ${colors.line}`,
        pb: 2.5,
      }}
    >
      <Box>
        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
          <Box
            className="sentinel-live-dot"
            sx={{
              width: "6px",
              height: "6px",
              borderRadius: "50%",
              backgroundColor: colors.cyan,
              boxShadow: `0 0 10px ${colors.cyan}`,
            }}
          />
          <SectionLabel>{t("commandCenterBadge")}</SectionLabel>
        </Stack>
        <Stack
          direction={{ xs: "column", sm: "row" }}
          spacing={1.5}
          alignItems={{ xs: "flex-start", sm: "center" }}
        >
          <Typography
            component="h1"
            variant="h3"
            sx={{
              fontFamily: fonts.mono,
              color: colors.white,
              fontWeight: 600,
              letterSpacing: "-0.03em",
            }}
          >
            Smart Anti-Theft
          </Typography>
          <Chip
            label={t("liveSecure")}
            size="small"
            variant="outlined"
            sx={{
              height: "26px",
              borderRadius: "0px",
              borderColor: `${colors.cyan}59`,
              color: colors.cyan,
              fontFamily: fonts.mono,
              fontSize: "10px",
              fontWeight: 600,
              letterSpacing: "0.18em",
              textTransform: "uppercase",
            }}
          />
        </Stack>
        <Typography variant="body2" sx={{ mt: 1, color: colors.textSecondary, fontFamily: fonts.ui }}>
          {t("dashboardUser")} <Box component="span" sx={{ color: colors.textPrimary }}>{accountName}</Box> {t("realTimeSignalUpdates")}
        </Typography>
      </Box>

      <Stack
        direction="row"
        spacing={1.25}
        alignItems="center"
        sx={{ alignSelf: { xs: "flex-start", lg: "auto" } }}
      >
        <Box
          sx={{
            border: `1px solid ${colors.line}`,
            backgroundColor: `${colors.panel}8C`,
            px: 1.5,
            py: 1,
            textAlign: "right",
          }}
        >
          <Typography variant="caption" sx={{ ...monoLabelSx, display: "block" }}>
            UTC+07 / LAST SYNC
          </Typography>
          <Typography variant="body2" sx={{ mt: 0.25, color: colors.cyan, fontFamily: fonts.mono }}>
            14:24:08{" "}
            <Box component="span" sx={{ color: colors.textMuted }}>
              · 12s
            </Box>
          </Typography>
        </Box>
        <LanguageSwitcher />
        <Tooltip title={t("notifications")}>
          <IconButton
            aria-label={t("notifications")}
            sx={{
              border: `1px solid ${colors.line}`,
              borderRadius: "0px",
              color: colors.textSecondary,
              backgroundColor: `${colors.panel}8C`,
              "&:hover": { color: colors.cyan, borderColor: colors.cyan },
            }}
          >
            <IconGlyph name="notifications_none" />
          </IconButton>
        </Tooltip>
        <Tooltip title={currentUser ? `${t("account")} ${accountName}` : t("accountNotLoggedIn")}>
          <IconButton
            aria-label={currentUser ? `${t("account")} ${accountName}` : t("accountNotLoggedIn")}
            onClick={() => navigate("/profile-account")}
            sx={{
              border: `1px solid ${colors.line}`,
              borderRadius: "0px",
              color: colors.textSecondary,
              backgroundColor: `${colors.panel}8C`,
              "&:hover": { color: colors.cyan, borderColor: colors.cyan },
            }}
          >
            <IconGlyph name="account_circle" />
          </IconButton>
        </Tooltip>
      </Stack>
    </Box>
  );
}

function TelemetryPanel({ data }) {
  const { language, t } = useLanguage();
  const battery = data?.batteryLevel || 0;

  return (
    <SentinelSurface
      component="section"
      aria-labelledby="telemetry-title"
      data-reveal
      sx={{ ...panelSx, minHeight: { md: "650px" }, p: 2 }}
      elevation={0}
    >
      <Stack direction="row" alignItems="center" justifyContent="space-between">
        <Typography
          id="telemetry-title"
          variant="caption"
          sx={{ ...textStyles.label, color: colors.cyan, fontWeight: 600 }}
        >
          01 / TELEMETRY
        </Typography>
        <IconGlyph name="more_horiz" size={16} color={colors.textMuted} />
      </Stack>

      <Box sx={{ mt: 2.5, borderBottom: `1px solid ${colors.line}`, pb: 2.5 }}>
        <Stack direction="row" spacing={1.5} alignItems="center">
          <Box
            sx={{
              display: "flex",
              width: "48px",
              height: "48px",
              alignItems: "center",
              justifyContent: "center",
              border: `1px solid ${colors.cyan}80`,
              backgroundColor: colors.void,
              color: colors.cyan,
            }}
          >
            <IconGlyph name="phone_android" size={26} />
          </Box>
          <Box sx={{ minWidth: 0 }}>
            <Typography
              variant="body2"
              sx={{ color: colors.white, fontWeight: 600, fontFamily: fonts.ui }}
            >
              {data?.deviceName || "No Device Connected"}
            </Typography>
            <Typography
              variant="caption"
              sx={{ ...textStyles.mono, display: "block", mt: 0.5, color: colors.textMuted }}
            >
              {data?.manufacturer || "N/A"} {data?.model || ""} · ANDROID {data?.androidVersion || ""}
            </Typography>
          </Box>
        </Stack>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mt: 2 }}>
          <Stack direction="row" spacing={1} alignItems="center">
            <Box
              className="sentinel-live-dot"
              sx={{
                width: "8px",
                height: "8px",
                borderRadius: "50%",
                backgroundColor: data ? colors.cyan : colors.textMuted,
                boxShadow: data ? `0 0 10px ${colors.cyan}` : "none",
              }}
            />
            <Typography variant="caption" sx={{ color: data ? colors.cyanSoft : colors.textMuted, fontFamily: fonts.ui }}>
              {data ? t("trackingActive") : "Offline"}
            </Typography>
          </Stack>
          <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.textMuted }}>
            {battery}%
          </Typography>
        </Stack>
        <LinearProgress
          variant="determinate"
          value={battery}
          aria-label="Pin còn lại"
          sx={{
            mt: 1,
            height: "6px",
            borderRadius: "0px",
            backgroundColor: colors.void,
            "& .MuiLinearProgress-bar": {
              borderRadius: "0px",
              backgroundColor: colors.cyan,
              boxShadow: `0 0 12px ${colors.cyanGlowBar}`,
            },
          }}
        />
      </Box>

      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
          columnGap: 2,
          rowGap: 2.5,
          borderBottom: `1px solid ${colors.line}`,
          py: 2.5,
        }}
      >
        <Metric label="LAST PING" value={data ? new Date(data.lastTimestamp).toLocaleTimeString() : "N/A"} />
        <Metric label="ACCURACY" value={data ? "HIGH PRECISE" : "N/A"} />
        <Box sx={{ gridColumn: "1 / -1" }}>
          <Typography variant="caption" sx={monoLabelSx}>
            {t("currentZone")}
          </Typography>
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
            <IconGlyph name="location_on" size={15} color={colors.cyan} />
            <Typography variant="body2" sx={{ color: colors.white, fontFamily: fonts.ui }}>
              {data ? `${data.lastLatitude.toFixed(4)}, ${data.lastLongitude.toFixed(4)}` : "Location Unknown"}
            </Typography>
          </Stack>
        </Box>
      </Box>

      <Box sx={{ mt: 2.5 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1.5 }}>
          <Typography variant="caption" sx={monoLabelSx}>
            {t("deviceHealth")}
          </Typography>
          <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.cyan }}>
            NOMINAL
          </Typography>
        </Stack>
        <Stack spacing={1.5}>
          <HealthRow icon="wifi" label={t("cellular")} value={sentinelTelemetry.cellular} />
          <HealthRow icon="lock" label={t("screenLock")} value={sentinelTelemetry.screenLock} />
          <HealthRow icon="satellite" label={t("gpsLock")} value={sentinelTelemetry.gpsLock} />
        </Stack>
      </Box>

      <Box
        sx={{
          mt: "auto",
          border: `1px solid ${colors.line}`,
          backgroundColor: `${colors.void}8C`,
          p: 1.5,
        }}
      >
        <Stack direction="row" spacing={1} alignItems="center">
          <IconGlyph name="info_outline" size={14} color={colors.cyan} />
          <Typography variant="caption" sx={monoLabelSx}>
            {t("systemStates")}
          </Typography>
        </Stack>
        <Typography
          variant="body2"
          sx={{
            mt: 1,
            color: colors.textSecondary,
            fontSize: "12px",
            lineHeight: 1.6,
            fontFamily: fonts.ui,
          }}
        >
          {t("noCriticalError")}
        </Typography>
      </Box>
    </SentinelSurface>
  );
}

function Metric({ label, value }) {
  return (
    <Box>
      <Typography variant="caption" sx={monoLabelSx}>
        {label}
      </Typography>
      <Typography variant="body2" sx={{ mt: 0.5, color: colors.white, fontFamily: fonts.ui }}>
        {value}
      </Typography>
    </Box>
  );
}

function HealthRow({ icon, label, value }) {
  return (
    <Stack direction="row" alignItems="center" justifyContent="space-between">
      <Stack direction="row" spacing={1} alignItems="center">
        <IconGlyph name={icon} size={15} color={colors.cyan} />
        <Typography variant="caption" sx={{ color: colors.textSecondary, fontFamily: fonts.ui }}>
          {label}
        </Typography>
      </Stack>
      <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.textPrimary }}>
        {value}
      </Typography>
    </Stack>
  );
}

function MapPanel({ zoom, onZoomChange, device }) {
  const { language, t } = useLanguage();
  const [lastUpdateTime, setLastUpdateTime] = useState("00:00:00");
  const center = device ? [device.lastLatitude, device.lastLongitude] : [10.762622, 106.660172];

  useEffect(() => {
      setLastUpdateTime(new Date().toLocaleTimeString());
  }, [device]);

  return (
    <SentinelSurface
      component="section"
      aria-labelledby="map-title"
      data-reveal
      sx={{ ...panelSx, minHeight: { md: "650px" }, p: 2 }}
      elevation={0}
    >
      <Stack
        direction="row"
        alignItems="center"
        justifyContent="space-between"
        flexWrap="wrap"
        gap={1.5}
      >
        <Box>
          <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>
            02 / {t("orbitalTracking")}
          </Typography>
          <Typography
            id="map-title"
            variant="h6"
            sx={{ mt: 0.5, color: colors.white, fontWeight: 600 }}
          >
            {t("liveLocation")}{" "}
            <Box
              component="span"
              sx={{
                ...textStyles.mono,
                fontSize: "12px",
                color: colors.textMuted,
                fontWeight: 400,
              }}
            >
              · {lastUpdateTime}
            </Box>
          </Typography>
        </Box>
        <Stack direction="row" spacing={1.5} alignItems="center">
          <LegendDot color={colors.cyan} label={t("current")} />
          <LegendDot color={colors.teal} label={t("breadcrumb")} />
        </Stack>
      </Stack>

      <Box
        sx={{
          position: "relative",
          mt: 2,
          minHeight: { xs: "420px", sm: "490px" },
          overflow: "hidden",
          border: `1px solid ${colors.line}`,
          backgroundColor: colors.panelDeep,
        }}
      >
          {/* REPLACE SVG MAP WITH REAL LEAFLET MAP */}
          <Box sx={{ position: 'absolute', inset: 0 }}>
            <MapContainer
                center={center}
                zoom={15}
                scrollWheelZoom
                style={{ height: "100%", width: "100%", background: "#0e1721" }}
            >
                <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
                <MapController center={center} zoom={15} />

                {device && (
                    <Marker position={center} icon={new L.DivIcon({
                        className: "sentinel-live-marker",
                        html: `<div style="width: 12px; height: 12px; border-radius: 50%; background: ${colors.cyan}; border: 2px solid #fff; box-shadow: 0 0 18px ${colors.cyan};"></div>`,
                        iconSize: [12, 12],
                        iconAnchor: [6, 6],
                    })} />
                )}
            </MapContainer>
          </Box>

        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center"
          sx={{ position: "absolute", zIndex: 1000, left: 2, right: 2, bottom: 2 }}
        >
          <Stack
            direction="row"
            spacing={1}
            alignItems="center"
            sx={{
              border: `1px solid ${colors.line}`,
              backgroundColor: `${colors.void}BF`,
              px: 1.5,
              py: 1,
            }}
          >
            <Box
              sx={{
                width: "6px",
                height: "6px",
                borderRadius: "50%",
                backgroundColor: colors.cyan,
              }}
            />
            <Typography variant="caption" sx={{ ...textStyles.label, color: colors.textSecondary }}>
              {t("gpsAccuracy")}
            </Typography>
          </Stack>
          <Stack direction="row" spacing={0.5}>
            <Tooltip title={t("zoomIn")}>
              <IconButton
                aria-label={t("zoomIn")}
                onClick={() => onZoomChange(Math.min(1.18, zoom + 0.06))}
                sx={{
                  width: "32px",
                  height: "32px",
                  border: `1px solid ${colors.cyan}4D`,
                  borderRadius: "0px",
                  backgroundColor: `${colors.void}CC`,
                  color: colors.cyan,
                  "&:hover": { backgroundColor: `${colors.cyan}26` },
                }}
              >
                <IconGlyph name="add" />
              </IconButton>
            </Tooltip>
            <Tooltip title={t("zoomOut")}>
              <IconButton
                aria-label={t("zoomOut")}
                onClick={() => onZoomChange(Math.max(0.9, zoom - 0.06))}
                sx={{
                  width: "32px",
                  height: "32px",
                  border: `1px solid ${colors.cyan}4D`,
                  borderRadius: "0px",
                  backgroundColor: `${colors.void}CC`,
                  color: colors.cyan,
                  "&:hover": { backgroundColor: `${colors.cyan}26` },
                }}
              >
                <IconGlyph name="remove" />
              </IconButton>
            </Tooltip>
          </Stack>
        </Stack>
      </Box>

      <Stack
        direction={{ xs: "column", sm: "row" }}
        justifyContent="space-between"
        alignItems={{ xs: "flex-start", sm: "center" }}
        spacing={1.5}
        sx={{ mt: 2, borderTop: `1px solid ${colors.line}`, pt: 2 }}
      >
        <Typography variant="caption" sx={{ ...textStyles.label, color: colors.textMuted }}>
          {t("breadcrumbPath")}
        </Typography>
        <Button
          component={Link}
          to="/map"
          variant="outlined"
          endIcon={<IconGlyph name="arrow_outward" size={13} />}
          sx={{
            borderRadius: "0px",
            borderColor: `${colors.cyan}66`,
            color: colors.cyan,
            fontSize: "10px",
            fontWeight: 600,
            letterSpacing: "0.15em",
            textTransform: "uppercase",
            "&:hover": { borderColor: colors.cyan, backgroundColor: `${colors.cyan}1A` },
          }}
        >
          {t("openFullMap")}
        </Button>
      </Stack>
    </SentinelSurface>
  );
}

function LegendDot({ color, label }) {
  return (
    <Stack direction="row" spacing={1} alignItems="center">
      <Box sx={{ width: "8px", height: "8px", borderRadius: "50%", backgroundColor: color }} />
      <Typography variant="caption" sx={{ ...textStyles.label, color: colors.textMuted }}>
        {label}
      </Typography>
    </Stack>
  );
}

function MapBlock({ sx }) {
  return (
    <Box
      sx={{
        position: "absolute",
        border: `1px solid ${colors.cyan}20`,
        backgroundColor: `${colors.panel}6B`,
        ...sx,
      }}
    />
  );
}

function MapRoad({ sx }) {
  return (
    <Box sx={{ position: "absolute", height: "1px", backgroundColor: `${colors.cyan}40`, ...sx }} />
  );
}

function MapTag({ children, sx }) {
  return (
    <Typography
      variant="caption"
      sx={{
        position: "absolute",
        border: `1px solid ${colors.cyan}40`,
        backgroundColor: `${colors.void}CC`,
        px: 1,
        py: 0.75,
        color: colors.textSecondary,
        ...textStyles.mono,
        ...sx,
      }}
    >
      {children}
    </Typography>
  );
}

function ThreatPanel({ data, reviewed, onReview }) {
  const { language, t } = useLanguage();
  const latestEvent = data && data.length > 0 ? data[0] : null;

  return (
    <Stack
      component="section"
      aria-label="Cảnh báo và bằng chứng"
      spacing={2}
      data-reveal
      sx={{ minHeight: { md: "650px" } }}
    >
      <Paper
        elevation={0}
        sx={{
          borderRadius: "0px",
          border: `1px solid ${latestEvent?.eventType.includes('DANGER') || latestEvent?.eventType.includes('LOST') ? colors.danger : colors.cyan}80`,
          backgroundColor: latestEvent?.eventType.includes('DANGER') || latestEvent?.eventType.includes('LOST') ? colors.dangerPanel : colors.panelTranslucent,
          p: 2,
          boxShadow: latestEvent?.eventType.includes('DANGER') || latestEvent?.eventType.includes('LOST') ? shadows.dangerGlow : shadows.insetGlow,
        }}
      >
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Stack direction="row" spacing={1} alignItems="center" sx={{ color: latestEvent?.eventType.includes('DANGER') || latestEvent?.eventType.includes('LOST') ? colors.dangerSoft : colors.cyan }}>
            <IconGlyph name="shield" size={18} />
            <Typography
              variant="caption"
              sx={{ ...textStyles.label, color: latestEvent?.eventType.includes('DANGER') || latestEvent?.eventType.includes('LOST') ? colors.dangerSoft : colors.cyan, fontWeight: 600 }}
            >
              03 / THREAT STACK
            </Typography>
          </Stack>
          <Chip
            label={reviewed ? "REVIEWED" : data?.length > 0 ? "1 OPEN" : "CLEAR"}
            size="small"
            variant="outlined"
            sx={{
              height: "23px",
              borderRadius: "0px",
              borderColor: `${colors.danger}66`,
              color: colors.dangerSoft,
              fontFamily: fonts.mono,
              fontSize: "10px",
            }}
          />
        </Stack>

        {latestEvent ? (
            <Box sx={{ mt: 2, borderLeft: `2px solid ${colors.danger}`, pl: 1.5 }}>
              <Stack direction="row" justifyContent="space-between" spacing={1}>
                <Typography variant="body2" sx={{ color: colors.white, fontWeight: 600 }}>
                  {latestEvent.eventType.replace(/_/g, ' ')}
                </Typography>
                <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.dangerSoft }}>
                  {new Date(latestEvent.timestamp).toLocaleTimeString()}
                </Typography>
              </Stack>
              <Typography
                variant="body2"
                sx={{ mt: 0.5, color: colors.textSecondary, fontSize: "12px", lineHeight: 1.6 }}
              >
                {latestEvent.details || "Activity detected on your device."}
              </Typography>
            </Box>
        ) : (
            <Typography variant="body2" sx={{ mt: 2, color: colors.textSecondary }}>No threats detected.</Typography>
        )}
      </Paper>

      <Paper elevation={0} sx={{ ...panelSx, p: 2 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography
            variant="caption"
            sx={{ ...textStyles.label, color: colors.cyan, fontWeight: 600 }}
          >
            04 / PROOF TICKER
          </Typography>
        </Stack>
        <Stack spacing={1.5} sx={{ mt: 1.5 }}>
          {data && data.map((event) => (
            <Box
              key={event._id}
              sx={{
                borderLeft: `1px solid ${colors.cyan}73`,
                pl: 1.5,
              }}
            >
              <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.textMuted }}>
                {new Date(event.timestamp).toLocaleTimeString()} · {event.eventType}
              </Typography>
              <Typography variant="body2" sx={{ mt: 0.5, color: colors.textPrimary, fontSize: "12px" }}>
                {event.details || "Automated check-in"}
              </Typography>
            </Box>
          ))}
          {!data || data.length === 0 && <Typography variant="caption" color="textSecondary">Waiting for signals...</Typography>}
        </Stack>
      </Paper>

      <Paper elevation={0} sx={{ ...panelSx, mt: "auto", p: 2 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography
            variant="caption"
            sx={{ ...textStyles.label, color: colors.textSecondary, fontWeight: 600 }}
          >
            {t("systemHealth")}
          </Typography>
          <IconGlyph name="monitor_heart" size={17} color={colors.cyan} />
        </Stack>
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
            gap: 1.25,
            mt: 2,
          }}
        >
          <HealthStat value={data?.length || 0} label="ALERTS" />
          <HealthStat value={latestEvent ? "78%" : "N/A"} label="SIGNAL" />
          <HealthStat value={latestEvent ? "B+" : "A"} label="TRUST" />
        </Box>
      </Paper>
    </Stack>
  );
}

function HealthStat({ value, label }) {
  return (
    <Box sx={{ border: `1px solid ${colors.line}`, py: 1, textAlign: "center" }}>
      <Typography variant="body2" sx={{ ...textStyles.mono, color: colors.cyan }}>
        {value}
      </Typography>
      <Typography
        variant="caption"
        sx={{
          display: "block",
          mt: 0.5,
          ...textStyles.label,
          fontSize: "9px",
          color: colors.textMuted,
        }}
      >
        {label}
      </Typography>
    </Box>
  );
}

function RemoteDock({ onAction, status }) {
  const { language, t } = useLanguage();
  return (
    <SentinelSurface
      component="section"
      aria-label="Hành động từ xa"
      data-reveal
      sx={{ ...panelSx, mt: 2, p: 1.5 }}
      elevation={0}
    >
      <Stack
        direction={{ xs: "column", lg: "row" }}
        alignItems={{ xs: "flex-start", lg: "center" }}
        justifyContent="space-between"
        spacing={1.5}
      >
        <Stack direction="row" spacing={1.5} alignItems="center">
          <Box
            sx={{
              display: "flex",
              width: "36px",
              height: "36px",
              alignItems: "center",
              justifyContent: "center",
              border: `1px solid ${colors.cyan}59`,
              backgroundColor: colors.void,
              color: colors.cyan,
            }}
          >
            <IconGlyph name="lock" size={18} />
          </Box>
          <Box>
            <Typography variant="caption" sx={{ ...textStyles.label, color: colors.textMuted }}>
              {t("remoteDock")}
            </Typography>
            <Typography
              variant="body2"
              sx={{ mt: 0.25, color: colors.textSecondary, fontSize: "12px" }}
            >
              {status || t("remoteStatus")}
            </Typography>
          </Box>
        </Stack>
        <Stack direction="row" flexWrap="wrap" gap={1}>
          <RemoteButton icon="lock" label={t("lockDevice")} onClick={() => onAction("lock")} />
          <RemoteButton icon="volume_up" label={t("ringDevice")} onClick={() => onAction("ring")} />
          <RemoteButton
            icon="delete_forever"
            label={t("removeData")}
            danger
            onClick={() => onAction("wipe")}
          />
        </Stack>
      </Stack>
      <Stack
        direction="row"
        spacing={1}
        justifyContent="flex-end"
        alignItems="center"
        sx={{ mt: 1.5, borderTop: `1px solid ${colors.line}`, pt: 1.5 }}
      >
        <IconGlyph name="warning_amber" size={13} color={colors.dangerSoft} />
        <Typography variant="caption" sx={{ ...textStyles.label, color: colors.textMuted }}>
          {t("dataRemovalWarning")}
        </Typography>
      </Stack>
    </SentinelSurface>
  );
}

function RemoteButton({ icon, label, danger = false, onClick }) {
  const color = danger ? colors.dangerSoft : colors.cyan;
  const borderColor = danger ? `${colors.danger}B3` : `${colors.cyan}80`;
  return (
    <Button
      className="sentinel-action"
      aria-label={label}
      onClick={onClick}
      variant="outlined"
      startIcon={<IconGlyph name={icon} size={17} />}
      sx={{
        borderRadius: "0px",
        borderColor,
        backgroundColor: danger ? `${colors.danger}1A` : `${colors.cyan}1A`,
        color,
        px: 2,
        py: 1.1,
        fontSize: "12px",
        fontWeight: 600,
        textTransform: "none",
        transition: "transform 160ms ease, background-color 160ms ease, color 160ms ease",
        "&:hover": {
          borderColor: danger ? colors.danger : colors.cyan,
          backgroundColor: danger ? colors.danger : colors.cyan,
          color: danger ? colors.white : colors.void,
          transform: "translateY(-2px)",
        },
        "&:active": { transform: "scale(.96)" },
      }}
    >
      {label}
    </Button>
  );
}

function SentinelConsole() {
  const { pathname } = useLocation();
  const { language, t } = useLanguage();
  const pageRef = useRef(null);
  const user = getCurrentUser();
  const [zoom, setZoom] = useState(1);
  const [reviewed, setReviewed] = useState(false);
  const [wipeArmed, setWipeArmed] = useState(false);
  const [status, setStatus] = useState("");
  const [motionReady, setMotionReady] = useState(false);
  const [reducedMotion, setReducedMotion] = useState(false);

  const [telemetry, setTelemetry] = useState(null);
  const [events, setEvents] = useState([]);

  const fetchData = async () => {
    if (!user) return;
    try {
      const devices = await deviceService.getDeviceList(user.username, user.token);
      // Filter out Web Portal entries
      const mobileDevices = devices.filter(d =>
          !d.deviceName?.includes("Web") &&
          !d._id?.startsWith("web-")
      );

      if (mobileDevices.length > 0) {
        setTelemetry(mobileDevices[0]);
      }
      const activity = await deviceService.getSecurityEvents(user.username, user.token);
      setEvents(activity.slice(0, 5));
    } catch (e) { console.error(e); }
  };

  useEffect(() => {
    fetchData();
    const mediaQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
    const updateMotionPreference = () => setReducedMotion(mediaQuery.matches);
    updateMotionPreference();
    mediaQuery.addEventListener("change", updateMotionPreference);
    const frame = window.requestAnimationFrame(() => setMotionReady(true));
    return () => {
      window.cancelAnimationFrame(frame);
      mediaQuery.removeEventListener("change", updateMotionPreference);
    };
  }, []);

  const handleRemoteAction = async (action) => {
    if (!user) return;
    if (action === "wipe" && !wipeArmed) {
      setWipeArmed(true);
      setStatus(t("remoteActionConfirm"));
      return;
    }

    try {
        if (action === "lock") {
            await deviceService.toggleLostMode(user.username, true, "Remote lock", user.phone, user.token);
            setStatus("Lock command sent");
        } else if (action === "ring") {
            await deviceService.toggleAlarm(user.username, true, user.token);
            setStatus("Siren activated");
        } else if (action === "wipe") {
            await fetch(`${process.env.REACT_APP_API_BASE}/api/${user.username}/wipe`, {
                method: "POST",
                headers: { "Authorization": `Bearer ${user.token}`, "ngrok-skip-browser-warning": "true" }
            });
            setStatus("Wipe command sent");
        }
    } catch (e) { setStatus("Error: " + e.message); }

    setWipeArmed(false);
  };

  return (
    <>
      <GlobalStyles
        styles={{
          body: { backgroundColor: colors.void },
          "#app": { minHeight: "100vh" },
          ".sentinel-console": { fontFamily: fonts.ui },
          ".sentinel-console .sentinel-mono": { fontFamily: fonts.mono },
          ".sentinel-motion [data-reveal]": {
            opacity: 0,
            animation: `${reveal} 600ms cubic-bezier(.22,1,.36,1) forwards`,
          },
          ".sentinel-motion .sentinel-route-path": {
            animation: `${routeDraw} 1800ms ease 350ms forwards`,
          },
          ".sentinel-motion .sentinel-radar-ring": {
            animation: `${radarPulse} 1900ms ease-in-out infinite`,
          },
          ".sentinel-motion .sentinel-marker-ping": {
            animation: `${markerPulse} 1350ms ease-out infinite`,
          },
          ".sentinel-motion .sentinel-live-dot": {
            animation: `${livePulse} 800ms ease-in-out infinite`,
          },
          ".sentinel-motion .sentinel-map-grid": {
            animation: `${gridDrift} 8000ms linear infinite`,
          },
          "@media (prefers-reduced-motion: reduce)": {
            ".sentinel-console *": {
              animationDuration: "0.001ms !important",
              animationIterationCount: "1 !important",
              scrollBehavior: "auto !important",
              transitionDuration: "0.001ms !important",
            },
          },
        }}
      />
      <Box
        ref={pageRef}
        className={`sentinel-console ${
          motionReady && !reducedMotion ? "sentinel-motion" : "sentinel-static"
        }`}
        sx={{
          minHeight: "100vh",
          overflow: "hidden",
          backgroundColor: colors.void,
          color: colors.textPrimary,
          backgroundImage: `radial-gradient(circle at 78% 12%, rgba(0,49,83,.48), transparent 32%), radial-gradient(circle at 12% 96%, rgba(0,255,255,.06), transparent 28%)`,
        }}
      >
        <Box sx={{ display: "flex", minHeight: "100vh" }}>
          <Rail activePath={pathname} />
          <Box component="main" sx={{ minWidth: 0, flex: 1, p: { xs: 2, sm: 3, xl: 3.5 } }}>
            <TopBar />
            <Box
              component="section"
              aria-label="Trung tâm điều hành"
              sx={{
                display: "grid",
                gridTemplateColumns: {
                  xs: "1fr",
                  lg: "minmax(220px, 246px) minmax(0, 1fr)",
                  xl: "246px minmax(0, 1fr) 302px",
                },
                gap: 2,
                mt: 3,
              }}
            >
              <TelemetryPanel data={telemetry} />
              <MapPanel zoom={zoom} onZoomChange={setZoom} device={telemetry} />
              <ThreatPanel data={events} reviewed={reviewed} onReview={() => setReviewed((value) => !value)} />
            </Box>
            <RemoteDock
              status={wipeArmed ? t("remoteActionConfirm") : status}
              onAction={handleRemoteAction}
            />
            <Box
              component="footer"
              data-reveal
              sx={{
                display: "flex",
                flexDirection: { xs: "column", sm: "row" },
                alignItems: { xs: "flex-start", sm: "center" },
                justifyContent: "space-between",
                gap: 1,
                mt: 2.5,
                borderTop: `1px solid ${colors.line}`,
                pt: 2,
              }}
            >
              <Typography variant="caption" sx={{ ...textStyles.label, color: colors.textMuted }}>
                {t("privacyFirst")}
              </Typography>
              <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.textMuted }}>
                Encrypted telemetry · v2.4.1 · © 2026
              </Typography>
            </Box>
          </Box>
        </Box>
      </Box>
    </>
  );
}

export {
  SentinelSurface,
  IconGlyph,
  SectionLabel,
  Rail,
  TopBar,
  panelSx,
  monoLabelSx,
  colors,
  fonts,
  shadows,
  textStyles,
};

export default SentinelConsole;
