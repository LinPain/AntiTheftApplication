import { useState, useEffect } from "react";
import Grid from "@mui/material/Grid";
import Card from "@mui/material/Card";
import Icon from "@mui/material/Icon";
import IconButton from "@mui/material/IconButton";
import CircularProgress from "@mui/material/CircularProgress";

import MDBox from "components/MDBox";
import MDTypography from "components/MDTypography";
import MDButton from "components/MDButton";

import DashboardLayout from "examples/LayoutContainers/DashboardLayout";
import DashboardNavbar from "examples/Navbars/DashboardNavbar";
import Footer from "examples/Footer";

import { getDeviceList, removeDevice } from "services/device";
import { getCurrentUser } from "services/auth";

function Devices() {
  const [devices, setDevices] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const user = getCurrentUser();

  const fetchDevices = async () => {
    if (!user) return;
    try {
      setIsLoading(true);
      const data = await getDeviceList(user.username, user.token);
      // Backend returns status object with userStates structure
      // We need to fetch from /api/:username/location/devices/status for the actual list
      const response = await fetch(`${process.env.REACT_APP_API_BASE}/api/${user.username}/location/devices/status`, {
        headers: {
          "Authorization": `Bearer ${user.token}`,
          "ngrok-skip-browser-warning": "true"
        }
      });
      const deviceList = await response.json();
      setDevices(deviceList);
    } catch (error) {
      console.error("Failed to fetch devices:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (deviceId) => {
    if (!window.confirm(`Bạn có chắc chắn muốn gỡ thiết bị ${deviceId}?`)) return;
    try {
      await removeDevice(user.username, deviceId, user.token);
      fetchDevices();
    } catch (error) {
      alert("Lỗi: " + error.message);
    }
  };

  useEffect(() => {
    fetchDevices();
  }, []);

  return (
    <DashboardLayout>
      <DashboardNavbar />
      <MDBox py={3}>
        <MDBox mb={3}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Card>
                <MDBox p={3} display="flex" justifyContent="space-between" alignItems="center">
                  <MDBox>
                    <MDTypography variant="h6" fontWeight="medium">
                      Danh sách thiết bị đã đăng nhập
                    </MDTypography>
                    <MDTypography variant="button" color="text" fontWeight="regular">
                      Quản lý các thiết bị đang truy cập tài khoản của bạn.
                    </MDTypography>
                  </MDBox>
                  <MDButton variant="gradient" color="info" onClick={fetchDevices} disabled={isLoading}>
                    <Icon>refresh</Icon>&nbsp;Làm mới
                  </MDButton>
                </MDBox>
                <MDBox pb={3} px={3}>
                  {isLoading ? (
                    <MDBox display="flex" justifyContent="center" p={5}>
                      <CircularProgress color="info" />
                    </MDBox>
                  ) : devices.length === 0 ? (
                    <MDTypography variant="body2" color="text" textAlign="center" py={5}>
                      Chưa có thiết bị nào được ghi nhận.
                    </MDTypography>
                  ) : (
                    <MDBox component="ul" display="flex" flexDirection="column" p={0} m={0}>
                      {devices.map((device) => (
                        <MDBox
                          key={device._id}
                          component="li"
                          display="flex"
                          justifyContent="space-between"
                          alignItems="center"
                          py={2}
                          pr={1}
                          borderBottom="1px solid #eee"
                        >
                          <MDBox display="flex" alignItems="center">
                            <MDBox
                              mr={2}
                              display="flex"
                              justifyContent="center"
                              alignItems="center"
                              width="48px"
                              height="48px"
                              bgColor="info"
                              variant="gradient"
                              borderRadius="lg"
                              shadow="md"
                            >
                              <Icon fontSize="medium" color="white">
                                {device.deviceName?.includes("Web") ? "laptop" : "smartphone"}
                              </Icon>
                            </MDBox>
                            <MDBox display="flex" flexDirection="column">
                              <MDTypography variant="button" fontWeight="medium">
                                {device.deviceName || device._id}
                              </MDTypography>
                              <MDTypography variant="caption" color="text">
                                Lần cuối: {new Date(device.lastTimestamp).toLocaleString("vi-VN")}
                              </MDTypography>
                            </MDBox>
                          </MDBox>
                          <IconButton color="error" onClick={() => handleDelete(device._id)}>
                            <Icon>delete</Icon>
                          </IconButton>
                        </MDBox>
                      ))}
                    </MDBox>
                  )}
                </MDBox>
              </Card>
            </Grid>
          </Grid>
        </MDBox>
      </MDBox>
      <Footer />
    </DashboardLayout>
  );
}

export default Devices;
