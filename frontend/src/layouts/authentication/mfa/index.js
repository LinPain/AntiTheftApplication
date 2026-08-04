import { useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import Alert from "@mui/material/Alert";

import auth from "services/auth";
import SentinelAuthLayout from "layouts/sentinel/SentinelAuthLayout";
import { IconGlyph, colors, textStyles } from "layouts/sentinel";

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

function MFA() {
  const navigate = useNavigate();
  const location = useLocation();
  const username = location.state?.username || "";

  const [otp, setOtp] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [msg, setMsg] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (otp.length < 6) {
      setError("Mã OTP phải có 6 chữ số");
      return;
    }

    setError("");
    setIsLoading(true);
    try {
      const response = await auth.verifyLoginOtp({ username, otp });
      // Save user session
      const user = { username: response.username, token: response.token };
      localStorage.setItem("sat_current_user", JSON.stringify(user));

      navigate("/map");
    } catch (err) {
      setError(err.message || "Xác thực thất bại");
    } finally {
      setIsLoading(false);
    }
  };

  const handleResend = async () => {
    setError("");
    setMsg("");
    try {
      await auth.resendRegistrationOtp(username, "LOGIN");
      setMsg("Mã OTP mới đã được gửi!");
    } catch (err) {
      setError("Không thể gửi lại mã: " + err.message);
    }
  };

  if (!username) {
    return <Typography variant="h6" textAlign="center" py={5} color="white">Lỗi: Không tìm thấy thông tin tài khoản.</Typography>;
  }

  return (
    <SentinelAuthLayout
      eyebrow="SECURITY / 03"
      title="Xác thực đăng nhập"
      subtitle={`Nhập mã OTP 6 số đã gửi tới tài khoản ${username}.`}
    >
      <Stack component="form" spacing={3} onSubmit={handleSubmit}>
        <TextField
          id="mfa-otp"
          label="Mã OTP (6 chữ số)"
          value={otp}
          onChange={(e) => setOtp(e.target.value)}
          fullWidth
          sx={fieldSx}
          inputProps={{ style: { textAlign: 'center', letterSpacing: '0.5rem', fontSize: '1.5rem' } }}
        />

        {error && (
          <Alert severity="error" sx={{ borderRadius: 0, backgroundColor: `${colors.danger}26`, color: colors.dangerSoft }}>
            {error}
          </Alert>
        )}
        {msg && (
          <Alert severity="success" sx={{ borderRadius: 0, backgroundColor: `${colors.teal}26`, color: colors.teal }}>
            {msg}
          </Alert>
        )}

        <Button
          type="submit"
          fullWidth
          variant="outlined"
          disabled={isLoading}
          startIcon={<IconGlyph name="verified_user" size={17} />}
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
          {isLoading ? "Đang xác thực..." : "Xác nhận"}
        </Button>

        <Stack direction="row" justifyContent="center" spacing={1}>
          <Typography
            variant="caption"
            sx={{
              ...textStyles.mono,
              color: colors.cyan,
              cursor: "pointer",
              "&:hover": { textDecoration: "underline" },
            }}
            onClick={handleResend}
          >
            Gửi lại mã OTP
          </Typography>
        </Stack>
      </Stack>
    </SentinelAuthLayout>
  );
}

export default MFA;
