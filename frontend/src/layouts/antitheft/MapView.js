import { useEffect, useRef, useState } from "react";
import Grid from "@mui/material/Grid";
import Card from "@mui/material/Card";
import MDBox from "components/MDBox";
import MDTypography from "components/MDTypography";
import DashboardLayout from "examples/LayoutContainers/DashboardLayout";
import DashboardNavbar from "examples/Navbars/DashboardNavbar";
import Footer from "examples/Footer";
import { Select, MenuItem, FormControl, InputLabel } from "@mui/material";

function MapView() {
  const mapRef = useRef(null);
  const mapInstance = useRef(null);
  const markers = useRef({}); // Store markers by deviceId
  const socketInstance = useRef(null);
  const [lastUpdate, setLastUpdate] = useState("Never");
  const [deviceList, setDeviceList] = useState([]);
  const [selectedDevice, setSelectedDevice] = useState("all");

  useEffect(() => {
    // Initialize map
    if (!mapInstance.current && window.L) {
      mapInstance.current = window.L.map(mapRef.current).setView([10.762622, 106.660172], 13);
      window.L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      }).addTo(mapInstance.current);
    }

    const token = localStorage.getItem("token");
    const activeUser = localStorage.getItem("activeUser");

    if (token && activeUser && window.io) {
      // Initialize Socket.io
      socketInstance.current = window.io("/", {
        auth: { token },
        extraHeaders: { "ngrok-skip-browser-warning": "true" }
      });

      socketInstance.current.on("connect", () => {
        socketInstance.current.emit("join", activeUser.toLowerCase());
      });

      socketInstance.current.on("locationUpdate", (data) => {
        const pos = [data.latitude, data.longitude];

        if (!markers.current[data.deviceId] && window.L) {
          markers.current[data.deviceId] = window.L.marker(pos)
            .addTo(mapInstance.current)
            .bindPopup(`<b>Device:</b> ${data.deviceId}`);

          setDeviceList(prev => prev.includes(data.deviceId) ? prev : [...prev, data.deviceId]);
        } else if (markers.current[data.deviceId]) {
          markers.current[data.deviceId].setLatLng(pos);
        }

        if (selectedDevice === "all" || selectedDevice === data.deviceId) {
          mapInstance.current.panTo(pos);
        }

        setLastUpdate(new Date().toLocaleTimeString());
      });

      // Fetch all devices for this user
      fetch(`/api/${activeUser}/location/devices/status`, {
        headers: {
          "Authorization": `Bearer ${token}`,
          "ngrok-skip-browser-warning": "true"
        }
      })
      .then(res => res.json())
      .then(list => {
        const ids = list.map(d => d._id);
        setDeviceList(ids);

        list.forEach(dev => {
          const pos = [dev.lastLatitude, dev.lastLongitude];
          if (!markers.current[dev._id] && window.L) {
            markers.current[dev._id] = window.L.marker(pos)
              .addTo(mapInstance.current)
              .bindPopup(`<b>Device:</b> ${dev._id}`);
          } else if (markers.current[dev._id]) {
            markers.current[dev._id].setLatLng(pos);
          }
        });

        // Center on the first device if all
        if (selectedDevice === "all" && list.length > 0) {
          mapInstance.current.setView([list[0].lastLatitude, list[0].lastLongitude], 15);
        }
      });
    }

    return () => {
      if (socketInstance.current) socketInstance.current.disconnect();
    };
  }, [selectedDevice]);

  const handleDeviceChange = (event) => {
    const devId = event.target.value;
    setSelectedDevice(devId);
    if (devId !== "all" && markers.current[devId]) {
      mapInstance.current.panTo(markers.current[devId].getLatLng());
      markers.current[devId].openPopup();
    }
  };

  return (
    <DashboardLayout>
      <DashboardNavbar />
      <MDBox py={3}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <Card>
              <MDBox p={2} display="flex" justifyContent="space-between" alignItems="center">
                <MDBox>
                  <MDTypography variant="h6" fontWeight="medium">
                    Bản đồ theo dõi thời gian thực
                  </MDTypography>
                  <MDTypography variant="button" color="text" fontWeight="regular">
                    Cập nhật lần cuối: {lastUpdate}
                  </MDTypography>
                </MDBox>
                <FormControl variant="standard" sx={{ m: 1, minWidth: 200 }}>
                  <InputLabel>Chọn thiết bị</InputLabel>
                  <Select value={selectedDevice} onChange={handleDeviceChange} label="Chọn thiết bị">
                    <MenuItem value="all">Tất cả thiết bị</MenuItem>
                    {deviceList.map((id) => (
                      <MenuItem key={id} value={id}>{id}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </MDBox>
              <MDBox
                ref={mapRef}
                style={{ height: "600px", width: "100%", borderRadius: "0 0 12px 12px" }}
              />
            </Card>
          </Grid>
        </Grid>
      </MDBox>
      <Footer />
    </DashboardLayout>
  );
}

export default MapView;
