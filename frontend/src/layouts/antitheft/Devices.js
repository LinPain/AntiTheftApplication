/* eslint-disable react/prop-types, prettier/prettier */
import { useMemo, useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";

import { useLanguage, t } from "utils/i18n";

import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import IconButton from "@mui/material/IconButton";

import SentinelPageFrame from "layouts/sentinel/SentinelPageFrame";
import { IconGlyph, SentinelSurface, colors, fonts, textStyles } from "layouts/sentinel";
import { getCurrentUser } from "services/auth";
import deviceService from "services/device";

function Devices() {
  const { t, language } = useLanguage();
  const navigate = useNavigate();
  const user = getCurrentUser();
  const [deviceList, setDeviceList] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [globalStatus, setGlobalStatus] = useState({});

  const fetchDevices = async () => {
    if (!user) return;
    try {
      setIsLoading(true);
      const status = await fetch(`${process.env.REACT_APP_API_BASE}/api/${user.username}/status`, {
        headers: { "Authorization": `Bearer ${user.token}`, "ngrok-skip-browser-warning": "true" }
      }).then(r => r.json());
      setGlobalStatus(status);

      const list = await deviceService.getDeviceList(user.username, user.token);
      // Filter out Web Portal entries
      const mobileList = list.filter(d =>
          !d.deviceName?.includes("Web") &&
          !d._id?.startsWith("web-")
      );

      setDeviceList(mobileList);
      if (mobileList.length > 0 && !selectedId) setSelectedId(mobileList[0]._id);
      setIsLoading(false);
    } catch (error) {
      console.error("Failed to fetch devices:", error);
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDevices();
  }, []);

  const selectedDevice = useMemo(() => deviceList.find((d) => d._id === selectedId) || null, [selectedId, deviceList]);

  const handleRemove = async (id) => {
    if (!window.confirm(t("removeDeviceConfirm", `Gỡ thiết bị ${id}?`))) return;
    try {
      await deviceService.removeDevice(user.username, id, user.token);
      fetchDevices();
    } catch (e) { alert(e.message); }
  };

  const handleLostMode = async (active) => {
    const msg = active ? prompt(t("lostModeMessagePrompt", "Nhập thông điệp báo mất:"), "Thiết bị này đã bị báo mất!") : "";
    const phone = active ? prompt(t("lostModePhonePrompt", "Nhập số điện thoại liên hệ:"), user.phone) : "";
    if (active && (!msg || !phone)) return;
    try {
      await deviceService.toggleLostMode(user.username, active, msg, phone, user.token);
      fetchDevices();
    } catch (e) { alert(e.message); }
  };

  const handleAlarm = async (active) => {
    try {
      await deviceService.toggleAlarm(user.username, active, user.token);
      fetchDevices();
    } catch (e) { alert(e.message); }
  };

  if (isLoading) return <SentinelPageFrame><Box sx={{ display: 'flex', justifyContent: 'center', p: 10 }}><CircularProgress color="info" /></Box></SentinelPageFrame>;

  return (
    <SentinelPageFrame>
      <Stack spacing={2.5}>
        <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ xs: "flex-start", sm: "flex-end" }} spacing={2}>
          <Stack spacing={0.75}>
            <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>06 / {t("deviceRegistry").toUpperCase()}</Typography>
            <Typography component="h1" variant="h4" sx={{ color: colors.white, fontFamily: fonts.mono, fontWeight: 600 }}>{t("linkedDevicesTitle")}</Typography>
            <Typography variant="body2" sx={{ color: colors.textSecondary }}>{t("deviceRegistrySubtitle")}</Typography>
          </Stack>
          <Button variant="outlined" startIcon={<IconGlyph name="refresh" size={17} />} onClick={fetchDevices} sx={{ borderRadius: "0px", borderColor: `${colors.cyan}80`, color: colors.cyan, textTransform: "none", fontWeight: 600, "&:hover": { borderColor: colors.cyan, backgroundColor: colors.cyanFaint } }}>{t("refreshStatus")}</Button>
        </Stack>

        <Stack direction={{ xs: "column", xl: "row" }} spacing={2} alignItems="stretch">
          <BoxlessDeviceList devices={deviceList} selectedId={selectedId} onSelect={setSelectedId} t={t} />

          {selectedDevice ? (
            <SentinelSurface component="section" elevation={0} sx={{ flex: 1, p: 2.5 }}>
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
                  <Stack direction="row" spacing={1.5} alignItems="center">
                    <BoxIcon name={selectedDevice.deviceName?.includes("Web") ? "laptop" : "phone_android"} />
                    <Stack spacing={0.5}>
                        <Typography variant="h6" sx={{ color: colors.white, fontWeight: 600 }}>{selectedDevice.deviceName || selectedDevice._id}</Typography>
                        <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.textMuted }}>{selectedDevice.manufacturer} {selectedDevice.model} · {selectedDevice.androidVersion || "Web"}</Typography>
                    </Stack>
                  </Stack>
                  <Chip
                    label={(new Date() - new Date(selectedDevice.lastTimestamp)) < 300000 ? "ONLINE" : "OFFLINE"}
                    size="small"
                    sx={{ borderRadius: "0px", backgroundColor: (new Date() - new Date(selectedDevice.lastTimestamp)) < 300000 ? `${colors.teal}1A` : `${colors.textMuted}1A`, color: (new Date() - new Date(selectedDevice.lastTimestamp)) < 300000 ? colors.teal : colors.textMuted, fontFamily: fonts.mono, fontSize: "10px" }}
                  />
                </Stack>
                <Divider sx={{ my: 2, borderColor: colors.line }} />

                <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                  <DeviceMetric label={t("battery").toUpperCase()} value={`${selectedDevice.batteryLevel || 0}% ${selectedDevice.isCharging ? '⚡' : ''}`} icon="battery_full" />
                  <DeviceMetric label="IP ADDRESS" value={selectedDevice.ipAddress || "N/A"} icon="lan" />
                  <DeviceMetric label={t("risk", "RỦI RO").toUpperCase()} value={`${selectedDevice.riskScore || 0} / 100`} icon="gpp_maybe" />
                </Stack>

                <Stack spacing={1.5} sx={{ mt: 3 }}>
                  <DetailRow label={t("status", "TRẠNG THÁI").toUpperCase()} value={globalStatus.lostMode?.active ? t("lostModeActive", "ĐÃ BÁO MẤT (LOCKED)") : t("statusVerified")} icon="lock" />
                  <DetailRow label={t("lastUpdate", "CẬP NHẬT CUỐI").toUpperCase()} value={new Date(selectedDevice.lastTimestamp).toLocaleString(language === "vi" ? "vi-VN" : "en-US")} icon="schedule" />
                  <DetailRow label={t("deviceId").toUpperCase()} value={selectedDevice._id} icon="fingerprint" />
                </Stack>

                <Divider sx={{ my: 3, borderColor: colors.line }} />

                <Stack direction="row" spacing={2}>
                    <Button
                        variant="contained"
                        color={globalStatus.lostMode?.active ? "success" : "error"}
                        onClick={() => handleLostMode(!globalStatus.lostMode?.active)}
                        startIcon={<IconGlyph name={globalStatus.lostMode?.active ? "lock_open" : "lock"} />}
                        sx={{ borderRadius: 0, flex: 1, color: "white" }}
                    >
                        {globalStatus.lostMode?.active ? t("unlockDevice", "Mở khoá từ xa") : t("lockDevice")}
                    </Button>
                    <Button
                        variant="outlined"
                        color="warning"
                        onClick={() => handleAlarm(!globalStatus.alarm?.active)}
                        startIcon={<IconGlyph name={globalStatus.alarm?.active ? "notifications_off" : "notifications_active"} />}
                        sx={{ borderRadius: 0, flex: 1 }}
                    >
                        {globalStatus.alarm?.active ? t("stopRing", "Tắt còi báo") : t("ringDevice")}
                    </Button>
                    <IconButton color="error" onClick={() => handleRemove(selectedDevice._id)} sx={{ border: `1px solid ${colors.dangerSoft}`, borderRadius: 0 }}>
                        <IconGlyph name="delete" />
                    </IconButton>
                </Stack>
            </SentinelSurface>
          ) : (
            <SentinelSurface sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Typography color="textSecondary">{t("selectToManage", "Chọn một thiết bị để xem chi tiết.")}</Typography>
            </SentinelSurface>
          )}
        </Stack>
      </Stack>
    </SentinelPageFrame>
  );
}

function BoxlessDeviceList({ devices, selectedId, onSelect, t }) {
  return (
    <SentinelSurface component="section" elevation={0} sx={{ width: { xs: "100%", xl: "360px" }, flexShrink: 0, p: 2 }}>
      <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>DATABASE / {devices.length.toString().padStart(2, '0')}</Typography>
      <Stack spacing={1} sx={{ mt: 2 }}>
        {devices.map((device) => (
          <Button
            key={device._id}
            onClick={() => onSelect(device._id)}
            aria-pressed={selectedId === device._id}
            sx={{
                justifyContent: "flex-start",
                borderRadius: "0px",
                border: `1px solid ${selectedId === device._id ? colors.cyan : colors.line}`,
                backgroundColor: selectedId === device._id ? colors.cyanFaint : "transparent",
                color: colors.textPrimary,
                p: 1.5,
                textAlign: "left",
                textTransform: "none",
                "&:hover": { borderColor: colors.cyan, backgroundColor: colors.cyanFaint }
            }}
          >
            <Stack direction="row" spacing={1.25} alignItems="center" sx={{ width: "100%" }}>
              <IconGlyph name={device.deviceName?.includes("Web") ? "laptop" : "phone_android"} color={selectedId === device._id ? colors.cyan : colors.textMuted} />
              <Stack spacing={0.35} sx={{ minWidth: 0 }}>
                <Typography variant="body2" noWrap sx={{ color: colors.textPrimary, fontWeight: 600 }}>{device.deviceName || device._id}</Typography>
                <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.textMuted }}>{new Date(device.lastTimestamp).toLocaleTimeString()} · {device.batteryLevel}%</Typography>
              </Stack>
            </Stack>
          </Button>
        ))}
      </Stack>
    </SentinelSurface>
  );
}

function BoxIcon({ name }) { return <Stack alignItems="center" justifyContent="center" sx={{ width: "48px", height: "48px", border: `1px solid ${colors.cyan}80`, backgroundColor: colors.void, color: colors.cyan }}><IconGlyph name={name} size={25} /></Stack>; }
function DeviceMetric({ label, value, icon }) { return <SentinelSurface elevation={0} sx={{ flex: 1, p: 1.5, backgroundColor: `${colors.void}8C` }}><Stack direction="row" spacing={1} alignItems="center"><IconGlyph name={icon} size={16} color={colors.cyan} /><Typography variant="caption" sx={{ ...textStyles.label, color: colors.textMuted }}>{label}</Typography></Stack><Typography variant="body2" sx={{ mt: 1, color: colors.white, fontFamily: fonts.mono }}>{value}</Typography></SentinelSurface>; }
function DetailRow({ label, value, icon }) { return <Stack direction={{ xs: "column", sm: "row" }} spacing={{ xs: 0.5, sm: 2 }} sx={{ borderBottom: `1px solid ${colors.line}`, pb: 1.25 }}><Stack direction="row" spacing={1} alignItems="center" sx={{ minWidth: "160px" }}><IconGlyph name={icon} size={15} color={colors.cyan} /><Typography variant="caption" sx={{ ...textStyles.label, color: colors.textMuted }}>{label}</Typography></Stack><Typography variant="body2" sx={{ color: colors.textPrimary }}>{value}</Typography></Stack>; }

export default Devices;
