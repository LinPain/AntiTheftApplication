import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Card from "@mui/material/Card";
import MDBox from "components/MDBox";
import MDTypography from "components/MDTypography";
import MDInput from "components/MDInput";
import MDButton from "components/MDButton";
import CoverLayout from "layouts/authentication/components/CoverLayout";
import bgImage from "assets/images/bg-sign-up-cover.jpeg";
import auth from "services/auth";

function ForgotPassword() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = (e) => {
    e.preventDefault();
    setError("");
    setMsg("");
    try {
      auth.sendPasswordReset(email);
      setMsg("Password reset requested. (Demo: check localStorage for reset request)");
    } catch (err) {
      setError(err.message || "Reset failed");
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
            Forgot Password
          </MDTypography>
          <MDTypography display="block" variant="button" color="white" my={1}>
            Enter your account email to request a password reset (demo)
          </MDTypography>
        </MDBox>
        <MDBox pt={4} pb={3} px={3}>
          <MDBox component="form" role="form" onSubmit={handleSubmit}>
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
            {error && (
              <MDBox mb={2}>
                <MDTypography color="error">{error}</MDTypography>
              </MDBox>
            )}
            {msg && (
              <MDBox mb={2}>
                <MDTypography color="text">{msg}</MDTypography>
              </MDBox>
            )}
            <MDBox mt={4} mb={1}>
              <MDButton type="submit" variant="gradient" color="info" fullWidth>
                Send reset
              </MDButton>
            </MDBox>
            <MDBox mt={3} mb={1} textAlign="center">
              <MDTypography variant="button" color="text">
                Remembered?&nbsp;
                <MDTypography
                  component="a"
                  href="#"
                  onClick={() => navigate("/authentication/sign-in")}
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

export default ForgotPassword;
