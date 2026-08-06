/* eslint-disable react/prop-types, prettier/prettier */
import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import * as yup from "yup";

import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import FormControl from "@mui/material/FormControl";
import FormControlLabel from "@mui/material/FormControlLabel";
import FormLabel from "@mui/material/FormLabel";
import Radio from "@mui/material/Radio";
import RadioGroup from "@mui/material/RadioGroup";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";

import auth from "services/auth";
import SentinelAuthLayout from "layouts/sentinel/SentinelAuthLayout";
import { IconGlyph, colors, fonts, textStyles } from "layouts/sentinel";
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

function SignUp() {
  const navigate = useNavigate();
  const { language } = useLanguage();
  const [username, setUsername] = useState("");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [phone, setPhone] = useState("");
  const [otpMethod, setOtpMethod] = useState("email"); // Default to email for this backend
  const [step, setStep] = useState("form");
  const [otp, setOtp] = useState("");
  const [submittedUser, setSubmittedUser] = useState("");
  const [pendingRegistration, setPendingRegistration] = useState(null);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [debugOtp, setDebugOtp] = useState("");
  const [passwordFocused, setPasswordFocused] = useState(false);

  const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
  const passwordRequirements = [
    { label: t("passwordAtLeast8"), valid: password.length >= 8 },
    { label: t("uppercase"), valid: /[A-Z]/.test(password) },
    { label: t("lowercase"), valid: /[a-z]/.test(password) },
    { label: t("number"), valid: /\d/.test(password) },
    { label: t("special"), valid: /[@$!%*?&]/.test(password) },
  ];

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSuccessMessage("");

    if (!username.trim() || !email.trim() || !password.trim()) {
      setError("Vui lòng nhập đầy đủ thông tin.");
      return;
    }

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailPattern.test(email)) {
      setError(t("validationInvalidEmail", "Invalid email"));
      return;
    }

    if (password.length < 8) {
      setError("Mật khẩu phải có ít nhất 8 ký tự.");
      return;
    }

    setPendingRegistration({ username: username.trim(), email: email.trim(), password });
    handleSendOtp();
  };

  const handleSendOtp = async () => {
    const registrationData = {
      username: username.trim().toLowerCase(),
      email: email.trim().toLowerCase(),
      password,
    };

    if (!registrationData.username || !registrationData.email || !registrationData.password) {
      setError("Vui lòng nhập đầy đủ thông tin.");
      return;
    }

    setError("");
    setSuccessMessage("Đang đăng ký...");
    setDebugOtp("");
    try {
      const result = await auth.startRegistration(registrationData);
      setSubmittedUser(registrationData.username);
      setStep("verify");
      setSuccessMessage(result.message || "Đã gửi mã OTP đến email của bạn.");
      if (result.mockCode) setDebugOtp(result.mockCode);
    } catch (requestError) {
      setError(requestError.message);
      setSuccessMessage("");
    }
  };

  const handleOtpSubmit = async (event) => {
    event.preventDefault();
    setError("");
    try {
      await auth.verifyRegistrationOtp({ username: submittedUser, otp });
      alert("Đăng ký thành công! Vui lòng đăng nhập.");
      navigate("/authentication/sign-in");
    } catch (verificationError) {
      setError(verificationError.message);
    }
  };

  const handleResendOtp = async () => {
    setError("");
    setMsg("");
    try {
      const result = await auth.resendRegistrationOtp(submittedUser, "REGISTRATION");
      setSuccessMessage("Mã OTP mới đã được gửi!");
      if (result.mockCode) setDebugOtp(result.mockCode);
    } catch (err) {
      setError("Không thể gửi lại mã: " + err.message);
    }
  };

  const handlePrimaryAction = (event) => {
    event?.preventDefault?.();
    if (step === "form") {
      handleSubmit(event);
    } else if (step === "delivery") {
      handleSendOtp();
    } else {
      handleOtpSubmit(event);
    }
  };

  return (
    <SentinelAuthLayout
      eyebrow={t("identityProvisioningEyebrow")}
      title={t("createIdentityTitle")}
      subtitle={step === "form" ? t("authSubtitleSignUpForm") : t("authSubtitleSignUpVerify")}
    >
      <Stack component="form" spacing={2} onSubmit={handlePrimaryAction}>
        {step === "form" && (
          <>
            <TextField
              label="Tên đăng nhập (Username)"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              fullWidth
              sx={fieldSx}
            />
            <TextField
              label={t("email")}
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              fullWidth
              sx={fieldSx}
            />
            <TextField
              label={t("password")}
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              onFocus={() => setPasswordFocused(true)}
              onBlur={() => setPasswordFocused(false)}
              fullWidth
              sx={fieldSx}
            />
            {(passwordFocused || password.length > 0) && (
              <Stack
                spacing={0.75}
                sx={{
                  border: `1px solid ${colors.line}`,
                  backgroundColor: `${colors.void}8C`,
                  p: 1.5,
                }}
              >
                <Typography variant="caption" sx={{ ...textStyles.label, color: colors.textMuted }}>
                  {t("passwordCheck")}
                </Typography>
                {passwordRequirements.map((item) => (
                  <Stack key={item.label} direction="row" spacing={1} alignItems="center">
                    <Typography
                      variant="caption"
                      sx={{
                        color: item.valid ? colors.cyan : colors.textMuted,
                        fontFamily: fonts.mono,
                      }}
                    >
                      {item.valid ? "✓" : "○"}
                    </Typography>
                    <Typography variant="caption" sx={{ color: colors.textSecondary }}>
                      {item.label}
                    </Typography>
                  </Stack>
                ))}
              </Stack>
            )}
          </>
        )}
        {step === "delivery" && (
          <>
            <Typography variant="body2" sx={{ color: colors.textSecondary }}>
              {t("otpDelivery")}
            </Typography>
            <FormControl component="fieldset">
              <FormLabel
                component="legend"
                sx={{ color: colors.textMuted, fontFamily: fonts.mono, fontSize: "11px" }}
              >
                {t("otpDeliveryMethod")}
              </FormLabel>
              <RadioGroup
                row
                value={otpMethod}
                onChange={(event) => setOtpMethod(event.target.value)}
              >
                <FormControlLabel
                  value="sms"
                  control={
                    <Radio
                      sx={{ color: colors.textMuted, "&.Mui-checked": { color: colors.cyan } }}
                    />
                  }
                  label={
                    <Typography variant="caption" sx={{ color: colors.textSecondary }}>
                      {t("phoneSms")}
                    </Typography>
                  }
                />
                <FormControlLabel
                  value="email"
                  control={
                    <Radio
                      sx={{ color: colors.textMuted, "&.Mui-checked": { color: colors.cyan } }}
                    />
                  }
                  label={
                    <Typography variant="caption" sx={{ color: colors.textSecondary }}>
                      {t("emailMethod")}
                    </Typography>
                  }
                />
              </RadioGroup>
            </FormControl>
            <Typography variant="caption" sx={{ color: colors.textSecondary }}>
              {t("receiveOtpAt")} {otpMethod === "sms" ? phone : email}.
            </Typography>
          </>
        )}
        {step === "verify" && (
          <>
            <TextField
              label={t("otpCode")}
              value={otp}
              onChange={(event) => setOtp(event.target.value)}
              inputProps={{ inputMode: "numeric", maxLength: 6 }}
              fullWidth
              sx={fieldSx}
            />
            {successMessage && (
              <Typography variant="caption" sx={{ color: colors.cyan }}>
                {successMessage}
              </Typography>
            )}
            {debugOtp && (
              <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.textMuted }}>
                {t("temporaryOtpLabel")} {debugOtp}
              </Typography>
            )}
          </>
        )}
        {error && (
          <Alert
            severity="error"
            role="alert"
            sx={{
              borderRadius: "0px",
              backgroundColor: `${colors.danger}26`,
              color: colors.dangerSoft,
              "& .MuiAlert-icon": { color: "inherit" },
            }}
          >
            {error}
          </Alert>
        )}
        {step === "form" && (
          <FormControlLabel
            control={
              <Checkbox sx={{ color: colors.textMuted, "&.Mui-checked": { color: colors.cyan } }} />
            }
            label={
              <Typography variant="caption" sx={{ color: colors.textSecondary }}>
                {t("iAgree")}
              </Typography>
            }
          />
        )}
        <Button
          type="button"
          fullWidth
          variant="outlined"
          onClick={(event) => {
            if (step === "form") {
              handleSubmit(event);
            } else {
              handleOtpSubmit(event);
            }
          }}
          startIcon={
            <IconGlyph name={step === "verify" ? "verified_user" : "arrow_forward"} size={17} />
          }
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
          {step === "form" ? t("continue") : step === "delivery" ? t("sendOtp") : t("verifyOtp")}
        </Button>
        {step === "delivery" && (
          <Button
            type="button"
            variant="text"
            onClick={() => setStep("form")}
            sx={{ color: colors.textSecondary, textTransform: "none" }}
          >
            {t("backToRegistration")}
          </Button>
        )}
        {step === "verify" && (
          <Button
            type="button"
            variant="text"
            onClick={handleResendOtp}
            sx={{ color: colors.textSecondary, textTransform: "none" }}
          >
            {t("resendOtp")}
          </Button>
        )}
        <Typography
          component={Link}
          to="/authentication/sign-in"
          variant="caption"
          sx={{
            color: colors.textSecondary,
            textAlign: "center",
            textDecoration: "none",
            "&:hover": { color: colors.cyan },
          }}
        >
          {t("alreadyHaveAccount")}
        </Typography>
      </Stack>
    </SentinelAuthLayout>
  );
}

export default SignUp;
