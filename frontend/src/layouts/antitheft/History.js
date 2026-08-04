/* eslint-disable react/prop-types, prettier/prettier */
import { useMemo, useState, useEffect } from "react";

import Box from "@mui/material/Box";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import CircularProgress from "@mui/material/CircularProgress";

import SentinelPageFrame from "layouts/sentinel/SentinelPageFrame";
import { IconGlyph, SentinelSurface, colors, fonts, textStyles } from "layouts/sentinel";
import { getCurrentUser } from "services/auth";
import deviceService from "services/device";
import { useLanguage, t } from "utils/i18n";

function History() {
  const { t, language } = useLanguage();
  const user = getCurrentUser();
  const [deviceId, setDeviceId] = useState("all");
  const [deviceList, setDeviceList] = useState([]);
  const [history, setHistory] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    const fetchInit = async () => {
        try {
            const list = await deviceService.getDeviceList(user.username, user.token);
            // Filter out Web Portal entries
            const mobileList = (list || []).filter(d =>
                !d.deviceName?.includes("Web") &&
                !d._id?.startsWith("web-")
            );

            if (mobileList.length > 0) {
                setDeviceList(mobileList);
                setDeviceId(mobileList[0]._id);
                fetchHistory(mobileList[0]._id);
            } else {
                setIsLoading(false);
            }
        } catch (e) { console.error(e); setIsLoading(false); }
    };
    fetchInit();
  }, []);

  const fetchHistory = async (id) => {
    if (!user || id === "all" || !id) return;
    try {
        setIsLoading(true);
        const response = await fetch(`${process.env.REACT_APP_API_BASE}/api/${user.username}/location/${id}`, {
            headers: { "Authorization": `Bearer ${user.token}`, "ngrok-skip-browser-warning": "true" }
        });
        const data = await response.json();
        setHistory(Array.isArray(data) ? data : []);
        setIsLoading(false);
    } catch (e) {
        console.error("Fetch history failed:", e);
        setHistory([]);
        setIsLoading(false);
    }
  };

  const handleDeviceChange = (e) => {
      const id = e.target.value;
      setDeviceId(id);
      fetchHistory(id);
  };

  return (
    <SentinelPageFrame>
      <Stack spacing={2.5}>
        <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ xs: "flex-start", sm: "flex-end" }} spacing={2}>
          <Stack spacing={0.75}>
            <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>08 / TRAIL</Typography>
            <Typography component="h1" variant="h4" sx={{ color: colors.white, fontFamily: fonts.mono, fontWeight: 600 }}>{t("history")}</Typography>
            <Typography variant="body2" sx={{ color: colors.textSecondary }}>{t("viewLocationHistory")}</Typography>
          </Stack>
        </Stack>

        <SentinelSurface component="section" elevation={0} sx={{ p: 1.5 }}>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} alignItems="center">
            <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>{t("changeDevice").toUpperCase()}</Typography>
            <Select value={deviceId} onChange={handleDeviceChange} size="small" sx={selectSx}>
              {deviceList.map((item) => (
                <MenuItem key={item._id} value={item._id}>{item.deviceName || item._id}</MenuItem>
              ))}
            </Select>
          </Stack>
        </SentinelSurface>

        <Stack direction={{ xs: "column", xl: "row" }} spacing={2} alignItems="stretch">
          <SentinelSurface component="section" elevation={0} sx={{ flex: 1, p: 2.5 }}>
            <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>LOCATION TIMELINE</Typography>
            <Stack spacing={2.25} sx={{ mt: 2.5 }}>
              {isLoading ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', py: 5 }}><CircularProgress color="info" /></Box>
              ) : history.map((event) => (
                <TimelineEvent key={event._id} event={event} language={language} />
              ))}
              {!isLoading && history.length === 0 && (
                <Typography variant="body2" sx={{ color: colors.textSecondary }}>{t("noCriticalError")}</Typography>
              )}
            </Stack>
          </SentinelSurface>
        </Stack>
      </Stack>
    </SentinelPageFrame>
  );
}

const selectSx = { minWidth: "220px", borderRadius: "0px", color: colors.textPrimary, fontFamily: fonts.ui, "& .MuiOutlinedInput-notchedOutline": { borderColor: colors.line }, "&:hover .MuiOutlinedInput-notchedOutline": { borderColor: `${colors.cyan}80` }, "&.Mui-focused .MuiOutlinedInput-notchedOutline": { borderColor: colors.cyan }, "& .MuiSvgIcon-root": { color: colors.cyan } };
function TimelineEvent({ event }) {
    return (
        <Stack direction="row" spacing={1.5} alignItems="flex-start">
            <Stack alignItems="center" sx={{ width: "16px", flexShrink: 0 }}>
                <Box sx={{ width: "10px", height: "10px", border: `2px solid ${colors.cyan}`, backgroundColor: colors.void, transform: "rotate(45deg)", mt: 0.5 }} />
                <Box sx={{ width: "1px", flex: 1, minHeight: "42px", mt: 1, backgroundColor: colors.line }} />
            </Stack>
            <Box sx={{ flex: 1, minWidth: 0, borderBottom: `1px solid ${colors.line}`, pb: 1.5 }}>
                <Stack direction="row" justifyContent="space-between" spacing={1}>
                    <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.textMuted }}>{new Date(event.timestamp).toLocaleString("vi-VN")}</Typography>
                    <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.cyan }}>{event.latitude.toFixed(4)}, {event.longitude.toFixed(4)}</Typography>
                </Stack>
                <Typography variant="body2" sx={{ mt: 0.75, color: colors.textPrimary, fontWeight: 600 }}>Cập nhật vị trí tự động</Typography>
            </Box>
        </Stack>
    );
}

export default History;
