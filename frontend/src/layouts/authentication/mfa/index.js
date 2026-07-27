import { useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import Card from "@mui/material/Card";
import MDBox from "components/MDBox";
import MDTypography from "components/MDTypography";
import MDInput from "components/MDInput";
import MDButton from "components/MDButton";
import CoverLayout from "layouts/authentication/components/CoverLayout";
import bgImage from "assets/images/bg-sign-in-basic.jpeg";
import auth from "services/auth";
import { triggerDiscoveryPulse } from "services/device";

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

      // Auto-register website
      try {
        await triggerDiscoveryPulse(user.username, user.token);
      } catch (pulseError) {
        console.error("Auto-discovery failed:", pulseError);
      }

      navigate("/account");
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
    return <MDTypography variant="h6" textAlign="center" py={5}>Lỗi: Không tìm thấy thông tin tài khoản.</MDTypography>;
  }

  return (
    <CoverLayout image={bgImage}>
      <Card>
        <MDBox
          variant="gradient"
          bgColor="info"
          borderRadius="lg"
          coloredShadow="info"
          mx={2}
          mt={-3}
          p={3}
          mb={1}
          textAlign="center"
        >
          <MDTypography variant="h4" fontWeight="medium" color="white" mt={1}>
            Xác thực đăng nhập
          </MDTypography>
          <MDTypography display="block" variant="button" color="white" my={1}>
            Nhập mã OTP 6 số đã gửi tới tài khoản {username}.
          </MDTypography>
        </MDBox>
        <MDBox pt={4} pb={3} px={3}>
          <MDBox component="form" role="form" onSubmit={handleSubmit}>
            <MDBox mb={2}>
              <MDInput
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
                type="text"
                label="Mã OTP"
                variant="standard"
                fullWidth
              />
            </MDBox>

            {error && (
              <MDBox mb={2}>
                <MDTypography color="error" variant="caption">{error}</MDTypography>
              </MDBox>
            )}
            {msg && (
              <MDBox mb={2}>
                <MDTypography color="success" variant="caption">{msg}</MDTypography>
              </MDBox>
            )}

            <MDBox mt={4} mb={1}>
              <MDButton type="submit" variant="gradient" color="info" fullWidth disabled={isLoading}>
                {isLoading ? "Đang xác thực..." : "Xác nhận"}
              </MDButton>
            </MDBox>
            <MDBox mt={2} textAlign="center">
              <MDTypography
                variant="button"
                color="info"
                fontWeight="medium"
                textGradient
                sx={{ cursor: "pointer" }}
                onClick={handleResend}
              >
                Gửi lại mã OTP
              </MDTypography>
            </MDBox>
          </MDBox>
        </MDBox>
      </Card>
    </CoverLayout>
  );
}

export default MFA;
