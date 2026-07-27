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
            {step === 3 ? "Đặt lại mật khẩu" : "Quên mật khẩu"}
          </MDTypography>
          <MDTypography display="block" variant="button" color="white" my={1}>
            {step === 1 && "Nhập Email hoặc Username để nhận mã OTP."}
            {step === 2 && `Nhập mã 6 số đã gửi tới tài khoản ${usernameFromServer}.`}
            {step === 3 && "Vui lòng nhập mật khẩu mới của bạn."}
          </MDTypography>
        </MDBox>
        <MDBox pt={4} pb={3} px={3}>
          <MDBox component="form" role="form" onSubmit={handleSubmit}>
            {step === 1 && (
              <MDBox mb={2}>
                <MDInput
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  type="text"
                  label="Email hoặc Username"
                  variant="standard"
                  fullWidth
                />
              </MDBox>
            )}
            {step === 2 && (
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
            )}
            {step === 3 && (
              <>
                <MDBox mb={2}>
                  <MDInput
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    type="password"
                    label="Mật khẩu mới"
                    variant="standard"
                    fullWidth
                  />
                </MDBox>
                <MDBox mb={2}>
                  <MDInput
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    type="password"
                    label="Xác nhận mật khẩu"
                    variant="standard"
                    fullWidth
                  />
                </MDBox>
              </>
            )}

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

            {step === 2 && (
              <MDBox mt={2} textAlign="center">
                <MDTypography
                  variant="button"
                  color="info"
                  fontWeight="medium"
                  textGradient
                  sx={{ cursor: "pointer" }}
                  onClick={async () => {
                    try {
                      await auth.resendRegistrationOtp(usernameFromServer, "RESET");
                      setMsg("Mã OTP mới đã được gửi!");
                    } catch (e) {
                      setError("Không thể gửi lại mã.");
                    }
                  }}
                >
                  Gửi lại mã OTP
                </MDTypography>
              </MDBox>
            )}

            <MDBox mt={4} mb={1}>
              <MDButton type="submit" variant="gradient" color="info" fullWidth disabled={isLoading}>
                {step === 1 ? "Tiếp tục" : step === 2 ? "Xác nhận OTP" : "Đặt lại mật khẩu"}
              </MDButton>
            </MDBox>
            <MDBox mt={3} mb={1} textAlign="center">
              <MDTypography variant="button" color="text">
                Quay lại&nbsp;
                <MDTypography
                  component="a"
                  href="#"
                  onClick={() => navigate("/authentication/sign-in")}
                  variant="button"
                  color="info"
                  fontWeight="medium"
                  textGradient
                >
                  Đăng nhập
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

export default ForgotPassword;
