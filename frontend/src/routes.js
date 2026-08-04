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

/** 
  All of the routes for the Material Dashboard 2 React are added here,
  You can add a new route, customize the routes and delete the routes here.

  Once you add a new route on this file it will be visible automatically on
  the Sidenav.

  For adding a new route you can follow the existing routes in the routes array.
  1. The `type` key with the `collapse` value is used for a route.
  2. The `type` key with the `title` value is used for a title inside the Sidenav. 
  3. The `type` key with the `divider` value is used for a divider between Sidenav items.
  4. The `name` key is used for the name of the route on the Sidenav.
  5. The `key` key is used for the key of the route (It will help you with the key prop inside a loop).
  6. The `icon` key is used for the icon of the route on the Sidenav, you have to add a node.
  7. The `collapse` key is used for making a collapsible item on the Sidenav that has other routes
  inside (nested routes), you need to pass the nested routes inside an array as a value for the `collapse` key.
  8. The `route` key is used to store the route location which is used for the react router.
  9. The `href` key is used to store the external links location.
  10. The `title` key is only for the item with the type of `title` and its used for the title text on the Sidenav.
  10. The `component` key is used to store the component of its route.
*/

// Routes for Smart Anti-Theft application
import SignIn from "layouts/authentication/sign-in";
import SignUp from "layouts/authentication/sign-up";
import ForgotPassword from "layouts/authentication/forgot-password";
import MFA from "layouts/authentication/mfa";

import Account from "layouts/antitheft/Account";
import Devices from "layouts/antitheft/Devices";
import MapView from "layouts/antitheft/MapView";
import History from "layouts/antitheft/History";
import SecurityActivity from "layouts/antitheft/SecurityActivity";
import SentinelConsole from "layouts/sentinel";
import { getLanguage } from "utils/i18n";

// @mui icons
import Icon from "@mui/material/Icon";

export const getLocalizedRoutes = (language = getLanguage()) => [
  {
    type: "collapse",
    name: language === "en" ? "Sentinel Console" : "Bảng điều khiển Sentinel",
    key: "sentinel",
    icon: <Icon fontSize="small">shield</Icon>,
    route: "/sentinel",
    component: <SentinelConsole />,
  },
  {
    type: "collapse",
    name: language === "en" ? "Account Management" : "Quản lý tài khoản",
    key: "account",
    icon: <Icon fontSize="small">person</Icon>,
    route: "/account",
    component: <Account />,
  },
  {
    type: "collapse",
    name: language === "en" ? "Profile Account" : "Hồ sơ tài khoản",
    key: "profile-account",
    icon: <Icon fontSize="small">account_circle</Icon>,
    route: "/profile-account",
    component: <Account />,
  },
  {
    type: "collapse",
    name: language === "en" ? "Devices" : "Thiết bị",
    key: "devices",
    icon: <Icon fontSize="small">devices</Icon>,
    route: "/devices",
    component: <Devices />,
  },
  {
    type: "collapse",
    name: language === "en" ? "Map" : "Bản đồ",
    key: "map",
    icon: <Icon fontSize="small">map</Icon>,
    route: "/map",
    component: <MapView />,
  },
  {
    type: "collapse",
    name: language === "en" ? "Location History" : "Lịch sử vị trí",
    key: "history",
    icon: <Icon fontSize="small">history</Icon>,
    route: "/history",
    component: <History />,
  },
  {
    type: "collapse",
    name: language === "en" ? "Security Log" : "Nhật ký bảo mật",
    key: "security-activity",
    icon: <Icon fontSize="small">security</Icon>,
    route: "/security-activity",
    component: <SecurityActivity />,
  },
  {
    type: "collapse",
    name: language === "en" ? "Sign In" : "Đăng nhập",
    key: "sign-in",
    icon: <Icon fontSize="small">login</Icon>,
    route: "/authentication/sign-in",
    component: <SignIn />,
  },
  {
    type: "collapse",
    name: language === "en" ? "Forgot Password" : "Quên mật khẩu",
    key: "forgot-password",
    icon: <Icon fontSize="small">help</Icon>,
    route: "/authentication/forgot-password",
    component: <ForgotPassword />,
  },
  {
    type: "collapse",
    name: language === "en" ? "Sign Up" : "Đăng ký",
    key: "sign-up",
    icon: <Icon fontSize="small">assignment</Icon>,
    route: "/authentication/sign-up",
    component: <SignUp />,
  },
  {
    type: "none",
    name: "MFA",
    key: "mfa",
    route: "/authentication/mfa",
    component: <MFA />,
  },
];

const routes = getLocalizedRoutes();
export default routes;
