/**
=========================================================
* Material Dashboard 2 React - v2.2.0
=========================================================

* Product Page: https://www.creative-tim.com/product/material-dashboard-react
* Copyright 2023 Creative Tim (https://www.creative-tim.com)

Coded by www.creative-tim.com

 =========================================================

* The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
*/

// react-router-dom components
import { Link, useNavigate } from "react-router-dom";
import auth from "services/auth";
import { useState } from "react";
import * as yup from "yup";

// @mui material components
import Card from "@mui/material/Card";
import Checkbox from "@mui/material/Checkbox";
import Radio from "@mui/material/Radio";
import RadioGroup from "@mui/material/RadioGroup";
import FormControl from "@mui/material/FormControl";
import FormControlLabel from "@mui/material/FormControlLabel";
import FormLabel from "@mui/material/FormLabel";

// Material Dashboard 2 React components
import MDBox from "components/MDBox";
import MDTypography from "components/MDTypography";
import MDInput from "components/MDInput";
import MDButton from "components/MDButton";

// Authentication layout components
import CoverLayout from "layouts/authentication/components/CoverLayout";

// Images
import bgImage from "assets/images/bg-sign-up-cover.jpeg";

function Cover() {
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [phone, setPhone] = useState("");
  const [otpMethod, setOtpMethod] = useState("sms");
  const [step, setStep] = useState("form");
  const [otp, setOtp] = useState("");
  const [submittedEmail, setSubmittedEmail] = useState("");
  const [pendingRegistration, setPendingRegistration] = useState(null);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [debugOtp, setDebugOtp] = useState("");
  const [passwordFocused, setPasswordFocused] = useState(false);

  const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
  const passwordRequirements = [
    {
      label: "At least 8 characters",
      valid: password.length >= 8,
    },
    {
      label: "One uppercase letter",
      valid: /[A-Z]/.test(password),
    },
    {
      label: "One lowercase letter",
      valid: /[a-z]/.test(password),
    },
    {
      label: "One number",
      valid: /\d/.test(password),
    },
    {
      label: "One special character (@$!%*?&)",
      valid: /[@$!%*?&]/.test(password),
    },
  ];
  const showPasswordHelper = passwordFocused || password.length > 0;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccessMessage("");
    const schema = yup.object({
      name: yup.string().required("Name is required"),
      email: yup.string().email("Invalid email").required("Email is required"),
      password: yup
        .string()
        .required("Password is required")
        .matches(
          passwordRegex,
          "Password must be at least 8 characters with uppercase, lowercase, number, and special character"
        ),
      phone: yup
        .string()
        .required("Phone is required")
        .matches(/^[0-9]{10,15}$/, "Phone must be 10 to 15 digits"),
    });

    try {
      schema.validateSync({ name, email, password, phone }, { abortEarly: false });
    } catch (validationError) {
      setError(validationError.errors.join("; "));
      return;
    }

    setPendingRegistration({ name, email, phone, password });
    setStep("delivery");
  };

  const handleSendOtp = async () => {
    if (!pendingRegistration) {
      setError("Please complete registration details first.");
      return;
    }

    setError("");
    setSuccessMessage("");
    try {
      const result = await auth.startRegistration({
        ...pendingRegistration,
        method: otpMethod,
      });
      setSubmittedEmail(pendingRegistration.email);
      setStep("verify");
      setSuccessMessage(result.message || `OTP sent via ${otpMethod === "sms" ? "SMS" : "email"}.`);
      if (result.debugOtp) {
        setDebugOtp(result.debugOtp);
      }
    } catch (err) {
      setError(err.message || "Registration failed");
    }
  };

  const handleOtpSubmit = async (e) => {
    e.preventDefault();
    setError("");
    try {
      await auth.verifyRegistrationOtp({ email: submittedEmail, otp });
      navigate("/authentication/sign-in");
    } catch (err) {
      setError(err.message || "OTP verification failed");
    }
  };

  const handleResendOtp = async () => {
    setError("");
    try {
      const result = await auth.resendRegistrationOtp(submittedEmail);
      setSuccessMessage(
        result.message || `OTP re-sent via ${otpMethod === "sms" ? "SMS" : "email"}.`
      );
      if (result.debugOtp) {
        setDebugOtp(result.debugOtp);
      }
    } catch (err) {
      setError(err.message || "Could not resend OTP");
    }
  };

  return (
    <CoverLayout image={bgImage}>
      <Card>
        <MDBox
          variant="gradient"
          bgColor="info"
          borderRadius="lg"
          coloredShadow="success"
          mx={2}
          mt={-3}
          p={3}
          mb={1}
          textAlign="center"
        >
          <MDTypography variant="h4" fontWeight="medium" color="white" mt={1}>
            Smart Anti-Theft Registration
          </MDTypography>
          <MDTypography display="block" variant="button" color="white" my={1}>
            {step === "form"
              ? "Enter your real email, phone and a strong password to get started."
              : "Enter the 6-digit OTP sent to your selected contact method."}
          </MDTypography>
        </MDBox>
        <MDBox pt={4} pb={3} px={3}>
          <MDBox
            component="form"
            role="form"
            onSubmit={
              step === "form"
                ? handleSubmit
                : step === "delivery"
                ? (e) => {
                    e.preventDefault();
                    handleSendOtp();
                  }
                : handleOtpSubmit
            }
          >
            {step === "form" ? (
              <>
                <MDBox mb={2}>
                  <MDInput
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    type="text"
                    label="Full name"
                    variant="standard"
                    fullWidth
                  />
                </MDBox>
                <MDBox mb={2}>
                  <MDInput
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    type="email"
                    label="Email"
                    variant="standard"
                    fullWidth
                  />
                </MDBox>
                <MDBox mb={2}>
                  <MDInput
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    type="tel"
                    label="Phone"
                    variant="standard"
                    fullWidth
                  />
                </MDBox>
                <MDBox mb={2}>
                  <MDInput
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    onFocus={() => setPasswordFocused(true)}
                    onBlur={() => setPasswordFocused(false)}
                    type="password"
                    label="Password"
                    variant="standard"
                    fullWidth
                  />
                </MDBox>
                {showPasswordHelper && (
                  <MDBox mb={2} p={2} borderRadius="md" bgColor="grey-100">
                    <MDTypography variant="caption" color="text">
                      Password requirements
                    </MDTypography>
                    {passwordRequirements.map((item) => (
                      <MDBox key={item.label} display="flex" alignItems="center" mt={1}>
                        <MDTypography
                          component="span"
                          color={item.valid ? "success" : "text"}
                          sx={{ fontWeight: item.valid ? "bold" : "regular", mr: 1 }}
                        >
                          {item.valid ? "✓" : "○"}
                        </MDTypography>
                        <MDTypography component="span" variant="caption" color="text">
                          {item.label}
                        </MDTypography>
                      </MDBox>
                    ))}
                  </MDBox>
                )}
              </>
            ) : step === "delivery" ? (
              <>
                <MDBox mb={2}>
                  <MDTypography variant="button" color="text">
                    Choose how to receive your OTP:
                  </MDTypography>
                </MDBox>
                <MDBox mb={2}>
                  <FormControl component="fieldset">
                    <FormLabel component="legend">OTP delivery method</FormLabel>
                    <RadioGroup
                      row
                      value={otpMethod}
                      onChange={(e) => setOtpMethod(e.target.value)}
                    >
                      <FormControlLabel
                        value="sms"
                        control={<Radio color="info" />}
                        label="Phone SMS"
                      />
                      <FormControlLabel
                        value="email"
                        control={<Radio color="info" />}
                        label="Email"
                      />
                    </RadioGroup>
                  </FormControl>
                </MDBox>
                <MDBox mb={2}>
                  <MDTypography variant="caption" color="text">
                    You will receive an OTP at {otpMethod === "sms" ? phone : email}.
                  </MDTypography>
                </MDBox>
              </>
            ) : (
              <>
                <MDBox mb={2}>
                  <MDInput
                    value={otp}
                    onChange={(e) => setOtp(e.target.value)}
                    type="text"
                    label="OTP code"
                    variant="standard"
                    fullWidth
                  />
                </MDBox>
                <MDBox mb={2}>
                  <MDTypography variant="caption" color="text">
                    {successMessage}
                  </MDTypography>
                </MDBox>
                {debugOtp && (
                  <MDBox mb={2}>
                    <MDTypography variant="caption" color="text">
                      Debug OTP (local dev only): {debugOtp}
                    </MDTypography>
                  </MDBox>
                )}
              </>
            )}
            {error && (
              <MDBox mb={2}>
                <MDTypography color="error">{error}</MDTypography>
              </MDBox>
            )}
            <MDBox display="flex" alignItems="center" ml={-1}>
              <Checkbox />
              <MDTypography
                variant="button"
                fontWeight="regular"
                color="text"
                sx={{ cursor: "pointer", userSelect: "none", ml: -1 }}
              >
                &nbsp;&nbsp;I agree the&nbsp;
              </MDTypography>
              <MDTypography
                component="a"
                href="#"
                variant="button"
                fontWeight="bold"
                color="info"
                textGradient
              >
                Terms and Conditions
              </MDTypography>
            </MDBox>
            <MDBox mt={4} mb={1}>
              <MDButton
                type="submit"
                variant="gradient"
                color="info"
                fullWidth
                onClick={step === "delivery" ? handleSendOtp : undefined}
              >
                {step === "form" ? "Continue" : step === "delivery" ? "Send OTP" : "Verify OTP"}
              </MDButton>
            </MDBox>
            {step === "delivery" && (
              <MDBox mt={1} mb={1} textAlign="center">
                <MDButton variant="text" color="info" onClick={() => setStep("form")} fullWidth>
                  Back to registration
                </MDButton>
              </MDBox>
            )}
            {step === "verify" && (
              <MDBox mt={1} mb={1} textAlign="center">
                <MDButton variant="text" color="info" onClick={handleResendOtp} fullWidth>
                  Resend OTP
                </MDButton>
              </MDBox>
            )}
            <MDBox mt={3} mb={1} textAlign="center">
              <MDTypography variant="button" color="text">
                Already have an account?{" "}
                <MDTypography
                  component={Link}
                  to="/authentication/sign-in"
                  variant="button"
                  color="info"
                  fontWeight="medium"
                  textGradient
                >
                  Sign In
                </MDTypography>
              </MDTypography>
            </MDBox>
          </MDBox>
        </MDBox>
      </Card>
    </CoverLayout>
  );
}

export default Cover;
