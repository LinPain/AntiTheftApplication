/* eslint-disable react/prop-types, prettier/prettier */
import { useEffect, useState } from "react";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import Divider from "@mui/material/Divider";

import SentinelPageFrame from "layouts/sentinel/SentinelPageFrame";
import { IconGlyph, SentinelSurface, colors, fonts, textStyles } from "layouts/sentinel";
import { getCurrentUser } from "services/auth";
import deviceService from "services/device";

function SecurityActivity() {
  const [events, setEvents] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const user = getCurrentUser();

  const fetchEvents = async () => {
    if (!user) return;
    try {
      setIsLoading(true);
      const data = await deviceService.getSecurityEvents(user.username, user.token);
      setEvents(data);
      setIsLoading(false);
    } catch (error) {
      console.error("Failed to fetch events:", error);
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents();
  }, []);

  const getEventIcon = (type) => {
    switch (type) {
      case 'LOST_MODE_ENABLED': return { icon: 'lock', color: colors.dangerSoft };
      case 'LOST_MODE_DISABLED': return { icon: 'lock_open', color: colors.teal };
      case 'ALARM_STARTED':
      case 'ALARM_STARTED_ON_DEVICE': return { icon: 'notifications_active', color: colors.warning };
      case 'ALARM_STOPPED':
      case 'ALARM_STOPPED_ON_DEVICE': return { icon: 'notifications_off', color: colors.cyan };
      case 'DEVICE_OFFLINE': return { icon: 'cloud_off', color: colors.textMuted };
      case 'DEVICE_RECONNECTED': return { icon: 'cloud_done', color: colors.teal };
      case 'SIM_CHANGED': return { icon: 'sim_card_alert', color: colors.dangerSoft };
      case 'FAILED_PIN_ATTEMPT': return { icon: 'gpp_maybe', color: colors.warning };
      default: return { icon: 'info', color: colors.cyan };
    }
  };

  return (
    <SentinelPageFrame>
      <Stack spacing={2.5}>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-end">
          <Stack spacing={0.75}>
            <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>09 / SECURITY LOG</Typography>
            <Typography component="h1" variant="h4" sx={{ color: colors.white, fontFamily: fonts.mono, fontWeight: 600 }}>Nhật ký bảo mật</Typography>
            <Typography variant="body2" sx={{ color: colors.textSecondary }}>Lịch sử các sự kiện an ninh và phản hồi từ hệ thống.</Typography>
          </Stack>
          <Box onClick={fetchEvents} sx={{ cursor: 'pointer', color: colors.cyan }}>
              <IconGlyph name="refresh" size={24} />
          </Box>
        </Stack>

        <SentinelSurface elevation={0} sx={{ p: 2.5, minHeight: '600px' }}>
          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 10 }}>
              <CircularProgress color="info" />
            </Box>
          ) : events.length === 0 ? (
            <Typography variant="body2" sx={{ color: colors.textSecondary, textAlign: 'center', py: 5 }}>Chưa có hoạt động bảo mật nào được ghi nhận.</Typography>
          ) : (
            <Stack spacing={2}>
              {events.map((event) => {
                const { icon, color } = getEventIcon(event.eventType);
                return (
                  <Box key={event._id}>
                    <Stack direction="row" spacing={2} alignItems="center">
                      <Box sx={{
                        width: 42, height: 42,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        border: `1px solid ${color}80`, color: color,
                        bgcolor: `${color}1A`
                      }}>
                        <IconGlyph name={icon} size={20} />
                      </Box>
                      <Stack spacing={0.5} sx={{ flex: 1 }}>
                        <Typography variant="body2" sx={{ color: colors.white, fontWeight: 600, fontFamily: fonts.mono }}>
                          {event.eventType.replace(/_/g, ' ')}
                        </Typography>
                        <Typography variant="caption" sx={{ color: colors.textSecondary }}>
                          {new Date(event.timestamp).toLocaleString("vi-VN")} • {event.details || "System automated event"}
                        </Typography>
                      </Stack>
                    </Stack>
                    <Divider sx={{ mt: 2, borderColor: colors.line }} />
                  </Box>
                );
              })}
            </Stack>
          )}
        </SentinelSurface>
      </Stack>
    </SentinelPageFrame>
  );
}

export default SecurityActivity;
