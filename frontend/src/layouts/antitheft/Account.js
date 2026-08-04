/* eslint-disable react/prop-types, prettier/prettier */
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import * as yup from "yup";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import Tooltip from "@mui/material/Tooltip";

import auth from "services/auth";
import deviceService from "services/device";
import SentinelPageFrame from "layouts/sentinel/SentinelPageFrame";
import { IconGlyph, SentinelSurface, colors, fonts, textStyles } from "layouts/sentinel";
import { useLanguage, t } from "utils/i18n";

const fieldSx = {
  "& .MuiInputLabel-root": { color: colors.textMuted, fontFamily: fonts.ui },
  "& .MuiInputLabel-root.Mui-focused": { color: colors.cyan },
  "& .MuiOutlinedInput-root": {
    borderRadius: "0px",
    color: colors.textPrimary,
    backgroundColor: `${colors.void}8C`,
    fontFamily: fonts.ui,
    "& fieldset": { borderColor: colors.line },
    "&:hover fieldset": { borderColor: `${colors.cyan}80` },
    "&.Mui-focused fieldset": { borderColor: colors.cyan },
  },
  "& .MuiFormHelperText-root": { marginLeft: 0, fontFamily: fonts.mono, color: colors.dangerSoft },
};

function Account() {
  const navigate = useNavigate();
  const { language, setLanguage } = useLanguage();
  const [user, setUser] = useState(null);
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [selectedDevice, setSelectedDevice] = useState("");
  const [deviceList, setDeviceList] = useState([]);
  const [message, setMessage] = useState({ tone: "", text: "" });

  useEffect(() => {
    const currentUser = auth.getCurrentUser();
    setUser(currentUser);

    if (currentUser) {
        setSelectedDevice(localStorage.getItem("sat_device") || "");
        deviceService.getDeviceList(currentUser.username, currentUser.token)
            .then(list => {
                setDeviceList(list);
                if (list.length > 0 && !localStorage.getItem("sat_device")) {
                    const firstId = list[0]._id;
                    setSelectedDevice(firstId);
                    localStorage.setItem("sat_device", firstId);
                }
            })
            .catch(e => console.error("Account: fetch devices failed", e));
    }
  }, []);

  const handleChangePassword = async (event) => {
    event.preventDefault();
    setMessage({ tone: "", text: "" });
    const schema = yup.object({
      oldPassword: yup.string().required("Mật khẩu cũ là bắt buộc"),
      newPassword: yup
        .string()
        .min(8, "Mật khẩu mới phải có ít nhất 8 ký tự")
        .required("Mật khẩu mới là bắt buộc"),
    });

    try {
      schema.validateSync({ oldPassword, newPassword }, { abortEarly: false });
      await auth.changePassword({ username: user.username, oldPassword, newPassword });
      setMessage({ tone: "success", text: "Đổi mật khẩu thành công!" });
      setOldPassword("");
      setNewPassword("");
    } catch (error) {
      setMessage({
        tone: "error",
        text: error.errors ? error.errors.join("; ") : error.message || "Đổi mật khẩu thất bại",
      });
    }
  };

  const handleLogout = () => {
    auth.logout();
    navigate("/authentication/sign-in");
  };

  const handleResetDemo = async () => {
    try {
      await auth.resetDemoAccount();
      setUser(null);
      setMessage({ tone: "success", text: t("demoClearedSuccess") });
      navigate("/authentication/sign-in");
    } catch (error) {
      auth.clearDemoState();
      setUser(null);
      setMessage({ tone: "error", text: error.message || t("demoClearError") });
      navigate("/authentication/sign-in");
    }
  };

  const handleDeviceChange = (event) => {
    const nextDevice = event.target.value;
    setSelectedDevice(nextDevice);
    localStorage.setItem("sat_device", nextDevice);
  };

  const handleLanguageChange = (nextLanguage) => {
    setLanguage(nextLanguage);
  };

  const displayName = user?.name || user?.username || t("accountName");
  const avatarText = displayName
    .split(" ")
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase() || "??";

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
              05 / {t("identityProfile", "IDENTITY PROFILE").toUpperCase()}
            </Typography>
            <Typography
              component="h1"
              variant="h4"
              sx={{ color: colors.white, fontFamily: fonts.mono, fontWeight: 600 }}
            >
              {t("accountProfile")}
            </Typography>
            <Typography variant="body2" sx={{ color: colors.textSecondary }}>
              {t("manageAccount")}
            </Typography>
          </Stack>
          <Tooltip title={t("refreshStatus")}>
            <IconButton
              aria-label={t("refreshStatus")}
              onClick={() => setUser(auth.getCurrentUser())}
              sx={{
                border: `1px solid ${colors.line}`,
                borderRadius: "0px",
                color: colors.cyan,
                "&:hover": { borderColor: colors.cyan, backgroundColor: colors.cyanFaint },
              }}
            >
              <IconGlyph name="refresh" />
            </IconButton>
          </Tooltip>
        </Stack>

        {!user ? (
          <SentinelSurface component="section" role="status" elevation={0} sx={{ p: 3 }}>
            <Stack direction="row" spacing={1.5} alignItems="flex-start">
              <IconGlyph name="lock" color={colors.dangerSoft} />
              <BoxlessMessage title={t("identityNotFound")} body={t("loginToManage")} />
            </Stack>
          </SentinelSurface>
        ) : (
          <Stack spacing={2.5}>
            <SentinelSurface component="section" elevation={0} sx={{ p: 3 }}>
              <Stack direction={{ xs: "column", md: "row" }} spacing={2.5} alignItems="center">
                <Box
                  sx={{
                    width: 84,
                    height: 84,
                    borderRadius: "50%",
                    background: `linear-gradient(135deg, ${colors.cyan}, ${colors.teal})`,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: colors.white,
                    fontFamily: fonts.mono,
                    fontSize: 28,
                    fontWeight: 700,
                    boxShadow: `0 0 24px ${colors.cyan}66`,
                  }}
                >
                  {avatarText}
                </Box>
                <Stack spacing={0.4} sx={{ flex: 1 }}>
                  <Typography variant="h5" sx={{ color: colors.white, fontWeight: 600 }}>
                    {displayName}
                  </Typography>
                  <Typography variant="body2" sx={{ color: colors.textSecondary }}>
                    {user.email}
                  </Typography>
                  <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>
                    {t("statusVerified")}
                  </Typography>
                </Stack>
              </Stack>
            </SentinelSurface>

            <Stack direction={{ xs: "column", lg: "row" }} spacing={2} alignItems="stretch">
              <SentinelSurface
                component="section"
                aria-labelledby="identity-title"
                elevation={0}
                sx={{ flex: 1, p: 2.5 }}
              >
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                  <Typography
                    id="identity-title"
                    variant="caption"
                    sx={{ ...textStyles.label, color: colors.cyan }}
                  >
                    {t("identityRecord").toUpperCase()}
                  </Typography>
                  <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.cyan }}>
                    {t("verified").toUpperCase()}
                  </Typography>
                </Stack>
                <Stack spacing={1.5} sx={{ mt: 2.5 }}>
                  <IdentityRow label={t("fullNameLabel").toUpperCase()} value={displayName} />
                  <IdentityRow label={t("emailChannel").toUpperCase()} value={user.email} />
                  <IdentityRow label={t("phoneChannel").toUpperCase()} value={user.phone || t("notProvided")} />
                </Stack>
              </SentinelSurface>

              <SentinelSurface
                component="section"
                aria-labelledby="password-title"
                elevation={0}
                sx={{ flex: 1, p: 2.5 }}
              >
                <Stack spacing={0.5}>
                  <Typography
                    id="password-title"
                    variant="caption"
                    sx={{ ...textStyles.label, color: colors.cyan }}
                  >
                    {t("credentialRotation").toUpperCase()}
                  </Typography>
                  <Typography variant="h6" sx={{ color: colors.white, fontWeight: 600 }}>
                    {t("changePassword")}
                  </Typography>
                </Stack>
                <Stack
                  component="form"
                  spacing={2}
                  onSubmit={handleChangePassword}
                  sx={{ mt: 2.5 }}
                >
                  <TextField
                    type="password"
                    label={t("oldPassword")}
                    value={oldPassword}
                    onChange={(event) => setOldPassword(event.target.value)}
                    fullWidth
                    sx={fieldSx}
                  />
                  <TextField
                    type="password"
                    label={t("newPassword")}
                    value={newPassword}
                    onChange={(event) => setNewPassword(event.target.value)}
                    fullWidth
                    sx={fieldSx}
                  />
                  <Button
                    type="submit"
                    variant="outlined"
                    startIcon={<IconGlyph name="key" size={17} />}
                    sx={{
                      alignSelf: "flex-start",
                      borderRadius: "0px",
                      borderColor: `${colors.cyan}80`,
                      color: colors.cyan,
                      fontWeight: 600,
                      textTransform: "none",
                      "&:hover": { borderColor: colors.cyan, backgroundColor: colors.cyanFaint },
                    }}
                  >
                    {t("updatePassword")}
                  </Button>
                  {message.text && (
                    <Alert
                      severity={message.tone || "info"}
                      role="alert"
                      sx={{
                        borderRadius: "0px",
                        backgroundColor:
                          message.tone === "error" ? `${colors.danger}26` : `${colors.cyan}1A`,
                        color: message.tone === "error" ? colors.dangerSoft : colors.cyan,
                        "& .MuiAlert-icon": { color: "inherit" },
                      }}
                    >
                      {message.text}
                    </Alert>
                  )}
                </Stack>
              </SentinelSurface>
            </Stack>

            <Stack direction={{ xs: "column", lg: "row" }} spacing={2} alignItems="stretch">
              <SentinelSurface component="section" elevation={0} sx={{ flex: 1, p: 2.5 }}>
                <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>
                  {t("deviceControl").toUpperCase()}
                </Typography>
                <Stack spacing={2} sx={{ mt: 2.5 }}>
                  <TextField
                    select
                    label={t("changeDevice")}
                    value={selectedDevice}
                    onChange={handleDeviceChange}
                    fullWidth
                    sx={fieldSx}
                  >
                    {deviceList.map((dev) => (
                        <MenuItem key={dev._id} value={dev._id}>
                            {dev.deviceName || dev._id}
                        </MenuItem>
                    ))}
                    {deviceList.length === 0 && <MenuItem value="">{t("noDevicesFound", "No devices found")}</MenuItem>}
                  </TextField>
                  <Typography variant="body2" sx={{ color: colors.textSecondary }}>
                    {t("selectedDevice")}:{" "}
                    <Box component="span" sx={{ color: colors.white, fontWeight: 600 }}>
                      {deviceList.find(d => d._id === selectedDevice)?.deviceName || selectedDevice || "None"}
                    </Box>
                  </Typography>
                </Stack>
              </SentinelSurface>

              <SentinelSurface component="section" elevation={0} sx={{ flex: 1, p: 2.5 }}>
                <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>
                  {t("language").toUpperCase()}
                </Typography>
                <Stack direction="row" spacing={1.5} sx={{ mt: 2.5 }}>
                  <Button
                    variant={language === "vi" ? "contained" : "outlined"}
                    onClick={() => handleLanguageChange("vi")}
                    sx={{
                      borderRadius: "0px",
                      backgroundColor: language === "vi" ? colors.cyan : "transparent",
                      color: language === "vi" ? colors.void : colors.cyan,
                      borderColor: colors.cyan,
                      textTransform: "none",
                    }}
                  >
                    Tiếng Việt
                  </Button>
                  <Button
                    variant={language === "en" ? "contained" : "outlined"}
                    onClick={() => handleLanguageChange("en")}
                    sx={{
                      borderRadius: "0px",
                      backgroundColor: language === "en" ? colors.cyan : "transparent",
                      color: language === "en" ? colors.void : colors.cyan,
                      borderColor: colors.cyan,
                      textTransform: "none",
                    }}
                  >
                    English
                  </Button>
                </Stack>
              </SentinelSurface>
            </Stack>

            <SentinelSurface component="section" elevation={0} sx={{ p: 2.5 }}>
              <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
                <Button
                  variant="outlined"
                  color="error"
                  startIcon={<IconGlyph name="logout" size={17} />}
                  onClick={handleLogout}
                  sx={{
                    borderRadius: "0px",
                    borderColor: `${colors.dangerSoft}80`,
                    color: colors.dangerSoft,
                    textTransform: "none",
                    "&:hover": {
                      borderColor: colors.dangerSoft,
                      backgroundColor: `${colors.danger}26`,
                    },
                  }}
                >
                  {t("logout")}
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<IconGlyph name="delete_forever" size={17} />}
                  onClick={handleResetDemo}
                  sx={{
                    borderRadius: "0px",
                    borderColor: `${colors.cyan}80`,
                    color: colors.cyan,
                    textTransform: "none",
                    "&:hover": { borderColor: colors.cyan, backgroundColor: colors.cyanFaint },
                  }}
                >
                  {t("clearDemo")}
                </Button>
              </Stack>
            </SentinelSurface>
          </Stack>
        )}
      </Stack>
    </SentinelPageFrame>
  );
}

function IdentityRow({ label, value }) {
  return (
    <Stack
      direction={{ xs: "column", sm: "row" }}
      spacing={{ xs: 0.5, sm: 2 }}
      sx={{ borderBottom: `1px solid ${colors.line}`, pb: 1.5 }}
    >
      <Typography
        variant="caption"
        sx={{ ...textStyles.label, minWidth: "124px", color: colors.textMuted }}
      >
        {label}
      </Typography>
      <Typography
        variant="body2"
        sx={{ color: colors.textPrimary, fontFamily: fonts.ui, overflowWrap: "anywhere" }}
      >
        {value}
      </Typography>
    </Stack>
  );
}

function BoxlessMessage({ title, body }) {
  return (
    <Stack spacing={0.75}>
      <Typography variant="h6" sx={{ color: colors.white }}>
        {title}
      </Typography>
      <Typography variant="body2" sx={{ color: colors.textSecondary }}>
        {body}
      </Typography>
    </Stack>
  );
}

export default Account;
