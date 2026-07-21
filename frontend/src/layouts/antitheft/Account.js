import Grid from "@mui/material/Grid";
import MDBox from "components/MDBox";
import MDInput from "components/MDInput";
import MDButton from "components/MDButton";
import MDTypography from "components/MDTypography";
import DashboardLayout from "examples/LayoutContainers/DashboardLayout";
import DashboardNavbar from "examples/Navbars/DashboardNavbar";
import Footer from "examples/Footer";
import { useState, useEffect } from "react";
import auth from "services/auth";
import * as yup from "yup";

function Account() {
  const [user, setUser] = useState(null);
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [msg, setMsg] = useState("");

  useEffect(() => {
    setUser(auth.getCurrentUser());
  }, []);

  const handleChangePassword = (e) => {
    e.preventDefault();
    setMsg("");
    const schema = yup.object({
      oldPassword: yup.string().required("Old password is required"),
      newPassword: yup
        .string()
        .min(6, "New password must be at least 6 characters")
        .required("New password is required"),
    });
    try {
      schema.validateSync({ oldPassword, newPassword }, { abortEarly: false });
    } catch (validationError) {
      setMsg(validationError.errors.join("; "));
      return;
    }

    try {
      auth.changePassword({ email: user.email, oldPassword, newPassword });
      setMsg("Password changed successfully");
      setOldPassword("");
      setNewPassword("");
    } catch (err) {
      setMsg(err.message || "Change password failed");
    }
  };

  if (!user) {
    return (
      <DashboardLayout>
        <DashboardNavbar />
        <MDBox py={3}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <h2>Không có người dùng</h2>
              <p>Vui lòng đăng nhập hoặc đăng ký.</p>
            </Grid>
          </Grid>
        </MDBox>
        <Footer />
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <DashboardNavbar />
      <MDBox py={3}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <h2>Quản lý tài khoản</h2>
            <p>Thông tin người dùng:</p>
            <MDTypography>Name: {user.name}</MDTypography>
            <MDTypography>Email: {user.email}</MDTypography>
            <MDTypography>Phone: {user.phone}</MDTypography>
          </Grid>
          <Grid item xs={12} md={6}>
            <h3>Đổi mật khẩu</h3>
            <form onSubmit={handleChangePassword}>
              <MDBox mb={2}>
                <MDInput
                  type="password"
                  label="Old password"
                  value={oldPassword}
                  onChange={(e) => setOldPassword(e.target.value)}
                  fullWidth
                />
              </MDBox>
              <MDBox mb={2}>
                <MDInput
                  type="password"
                  label="New password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  fullWidth
                />
              </MDBox>
              <MDBox mb={2}>
                <MDButton type="submit" variant="gradient" color="info">
                  Change password
                </MDButton>
              </MDBox>
              {msg && <MDTypography color="text">{msg}</MDTypography>}
            </form>
          </Grid>
        </Grid>
      </MDBox>
      <Footer />
    </DashboardLayout>
  );
}

export default Account;
