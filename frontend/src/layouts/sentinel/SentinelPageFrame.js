/* eslint-disable react/prop-types, prettier/prettier */
import { useLocation } from "react-router-dom";

import Box from "@mui/material/Box";
import GlobalStyles from "@mui/material/GlobalStyles";
import { keyframes } from "@mui/material/styles";

import { colors, fonts, Rail, TopBar } from "layouts/sentinel";

const pageReveal = keyframes`
  from { opacity: 0; transform: translate3d(0, 14px, 0); }
  to { opacity: 1; transform: translate3d(0, 0, 0); }
`;

function SentinelPageFrame({ children }) {
  const { pathname } = useLocation();

  return (
    <>
      <GlobalStyles
        styles={{
          body: { backgroundColor: colors.void },
          "#app": { minHeight: "100vh" },
          ".sentinel-page": { fontFamily: fonts.ui },
          ".sentinel-page [data-page-reveal]": { animation: `${pageReveal} 520ms cubic-bezier(.22,1,.36,1) both` },
          "@media (prefers-reduced-motion: reduce)": { ".sentinel-page [data-page-reveal]": { animation: "none" } },
        }}
      />
      <Box className="sentinel-page" sx={{ minHeight: "100vh", overflow: "hidden", backgroundColor: colors.void, color: colors.textPrimary, backgroundImage: `radial-gradient(circle at 78% 12%, rgba(0,49,83,.48), transparent 32%), radial-gradient(circle at 12% 96%, rgba(0,255,255,.06), transparent 28%)` }}>
        <Box sx={{ display: "flex", minHeight: "100vh" }}>
          <Rail activePath={pathname} />
          <Box component="main" sx={{ minWidth: 0, flex: 1, p: { xs: 2, sm: 3, xl: 3.5 } }}>
            <TopBar />
            <Box data-page-reveal sx={{ mt: 3 }}>{children}</Box>
          </Box>
        </Box>
      </Box>
    </>
  );
}

export default SentinelPageFrame;
