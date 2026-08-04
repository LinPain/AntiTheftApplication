/* eslint-disable react/prop-types, prettier/prettier */
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import * as yup from "yup";

import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import FormControlLabel from "@mui/material/FormControlLabel";
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

function SignIn() {
  const navigate = useNavigate();
  useLanguage();
  const [rememberMe, setRememberMe] = useState(() => {
    return localStorage.getItem("sat_remember_me") === "true";
  });
  const [email, setEmail] = useState(() => {
    return localStorage.getItem("sat_remember_email") || "";
  });
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");

    if (rememberMe) {
      localStorage.setItem("sat_remember_email", email.trim());
      localStorage.setItem("sat_remember_me", "true");
    } else {
      localStorage.removeItem("sat_remember_email");
      localStorage.setItem("sat_remember_me", "false");
    }

    try {
      yup
        .object({
          email: yup
            .string()
            .email(t("validationInvalidEmail", "Invalid email"))
            .required(t("validationEmailRequired", "Email is required")),
          password: yup
            .string()
            .min(6, t("validationPasswordTooShort", "Password must be at least 6 characters"))
            .required(t("validationPasswordRequired", "Password is required")),
        })
        .validateSync({ email, password }, { abortEarly: false });
      const response = await auth.login({ email: email.trim(), password });

      if (response.mfaRequired) {
        navigate("/authentication/mfa", { state: { username: response.username || email.trim() } });
        return;
      }

      // If no MFA (direct login), handle session
      const user = { username: response.username, token: response.token };
      localStorage.setItem("sat_current_user", JSON.stringify(user));

      navigate("/map");
    } catch (validationError) {
      setError(
        validationError.errors
          ? validationError.errors.join("; ")
          : validationError.message || "Login failed"
      );
    }
  };

  return (
    <SentinelAuthLayout
      eyebrow={t("authGateEyebrow")}
      title={t("signIn")}
      subtitle={t("authSubtitleSignIn")}
    >
      <Stack component="form" spacing={2} onSubmit={handleSubmit}>
        <TextField
          id="sign-in-email"
          label={t("email")}
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          autoComplete="email"
          fullWidth
          sx={fieldSx}
        />
        <TextField
          id="sign-in-password"
          label={t("password")}
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete="current-password"
          fullWidth
          sx={fieldSx}
        />
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
        <FormControlLabel
          control={
            <Checkbox
              checked={rememberMe}
              onChange={(event) => setRememberMe(event.target.checked)}
              sx={{ color: colors.textMuted, "&.Mui-checked": { color: colors.cyan } }}
            />
          }
          label={
            <Typography variant="caption" sx={{ color: colors.textSecondary }}>
              {t("rememberMe")}
            </Typography>
          }
        />
        <Button
          type="submit"
          fullWidth
          variant="outlined"
          startIcon={<IconGlyph name="login" size={17} />}
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
          {t("signInSecurely")}
        </Button>
        <Stack direction="row" justifyContent="space-between" spacing={2} sx={{ pt: 1 }}>
          <Typography
            component={Link}
            to="/authentication/forgot-password"
            variant="caption"
            sx={{
              ...textStyles.mono,
              color: colors.textSecondary,
              textDecoration: "none",
              "&:hover": { color: colors.cyan },
            }}
          >
            {t("forgotPassword")}
          </Typography>
          <Typography
            component={Link}
            to="/authentication/sign-up"
            variant="caption"
            sx={{ ...textStyles.mono, color: colors.cyan, textDecoration: "none" }}
          >
            {t("createIdentity")}
          </Typography>
        </Stack>
      </Stack>
    </SentinelAuthLayout>
  );
}

export default SignIn;
