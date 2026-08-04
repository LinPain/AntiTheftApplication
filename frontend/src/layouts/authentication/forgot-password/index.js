/* eslint-disable react/prop-types, prettier/prettier */
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";

import auth from "services/auth";
import SentinelAuthLayout from "layouts/sentinel/SentinelAuthLayout";
import { IconGlyph, colors } from "layouts/sentinel";
import { useLanguage, t } from "utils/i18n";

const fieldSx = {
  "& .MuiInputLabel-root": { color: colors.textMuted },
  "& .MuiInputLabel-root.Mui-focused": { color: colors.cyan },
  "& .MuiOutlinedInput-root": {
    borderRadius: "0px",
    color: colors.textPrimary,
    backgroundColor: `${colors.void}8C`,
    "& fieldset": { borderColor: colors.line },
    "&:hover fieldset": { borderColor: `${colors.cyan}80` },
    "&.Mui-focused fieldset": { borderColor: colors.cyan },
  },
};

function ForgotPassword() {
  const navigate = useNavigate();
  useLanguage();
  const [step, setStep] = useState(1); // 1: Email, 2: OTP, 3: New Password
  const [identifier, setIdentifier] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [usernameFromServer, setUsernameFromServer] = useState("");
  const [resetToken, setResetToken] = useState("");

  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setMsg("");
    setIsLoading(true);

    try {
      if (step === 1) {
        const resp = await auth.forgotPassword(identifier);
        setUsernameFromServer(resp.username);
        setStep(2);
        setMsg("Mã OTP đã được gửi đến email của bạn.");
      } else if (step === 2) {
        const resp = await auth.verifyReset(usernameFromServer, otp);
        setResetToken(resp.resetToken);
        setStep(3);
        setMsg("Xác thực thành công. Vui lòng đặt mật khẩu mới.");
      } else if (step === 3) {
        if (newPassword !== confirmPassword) throw new Error("Mật khẩu không khớp");
        if (newPassword.length < 8) throw new Error("Mật khẩu phải có ít nhất 8 ký tự");
        await auth.resetPassword(resetToken, newPassword);
        alert("Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
        navigate("/authentication/sign-in");
      }
    } catch (err) {
      setError(err.message || "Đã có lỗi xảy ra");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <SentinelAuthLayout
      eyebrow="RECOVERY / 02"
      title={step === 3 ? "Đặt lại mật khẩu" : "Quên mật khẩu"}
      subtitle={
        step === 1 ? "Nhập Email hoặc Username để nhận mã OTP." :
        step === 2 ? `Nhập mã 6 số đã gửi tới tài khoản ${usernameFromServer}.` :
        "Vui lòng nhập mật khẩu mới của bạn."
      }
    >
      <Stack component="form" spacing={2} onSubmit={handleSubmit}>
        {step === 1 && (
            <TextField
                id="reset-identifier"
                label="Email hoặc Username"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                fullWidth
                sx={fieldSx}
            />
        )}
        {step === 2 && (
            <TextField
                id="reset-otp"
                label="Mã OTP (6 số)"
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
                inputProps={{ maxLength: 6 }}
                fullWidth
                sx={fieldSx}
            />
        )}
        {step === 3 && (
            <>
                <TextField
                    id="new-password"
                    label="Mật khẩu mới"
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    fullWidth
                    sx={fieldSx}
                />
                <TextField
                    id="confirm-password"
                    label="Xác nhận mật khẩu"
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    fullWidth
                    sx={fieldSx}
                />
            </>
        )}

        {error && (
          <Alert severity="error" sx={{ borderRadius: 0, backgroundColor: `${colors.danger}26`, color: colors.dangerSoft }}>
            {error}
          </Alert>
        )}
        {msg && (
          <Alert severity="success" sx={{ borderRadius: 0, backgroundColor: `${colors.cyan}1A`, color: colors.cyan }}>
            {msg}
          </Alert>
        )}

        <Button
          type="submit"
          fullWidth
          variant="outlined"
          disabled={isLoading}
          startIcon={<IconGlyph name={step === 3 ? "vpn_key" : "mark_email_read"} size={17} />}
          sx={{
            borderRadius: "0px",
            borderColor: `${colors.cyan}80`,
            color: colors.cyan,
            py: 1.25,
            fontWeight: 600,
            textTransform: "none",
            "&:hover": { borderColor: colors.cyan, backgroundColor: colors.cyanFaint },
          }}
        >
          {step === 1 ? "Tiếp tục" : step === 2 ? "Xác nhận OTP" : "Đặt lại mật khẩu"}
        </Button>
        <Typography
          component={Link}
          to="/authentication/sign-in"
          variant="caption"
          onClick={() => navigate("/authentication/sign-in")}
          sx={{
            color: colors.textSecondary,
            textAlign: "center",
            textDecoration: "none",
            "&:hover": { color: colors.cyan },
          }}
        >
          {t("rememberedPassword")}
        </Typography>
      </Stack>
    </SentinelAuthLayout>
  );
}

export default ForgotPassword;
