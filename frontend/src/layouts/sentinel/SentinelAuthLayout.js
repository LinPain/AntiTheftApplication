/* eslint-disable react/prop-types, prettier/prettier */
import Box from "@mui/material/Box";
import Chip from "@mui/material/Chip";
import GlobalStyles from "@mui/material/GlobalStyles";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";

import { IconGlyph, SentinelSurface, colors, fonts, textStyles } from "layouts/sentinel";
import LanguageSwitcher from "components/LanguageSwitcher";
import { useLanguage, t } from "utils/i18n";

function SentinelAuthLayout({ eyebrow, title, subtitle, children, footer }) {
  const { language } = useLanguage();
  return (
    <>
      <GlobalStyles
        styles={{
          body: { backgroundColor: colors.void },
          "#app": { minHeight: "100vh" },
          ".sentinel-auth": { fontFamily: fonts.ui },
          ".sentinel-auth .sentinel-mono": { fontFamily: fonts.mono },
        }}
      />
      <Box
        className="sentinel-auth"
        sx={{
          minHeight: "100vh",
          overflow: "hidden",
          color: colors.textPrimary,
          backgroundColor: colors.void,
          backgroundImage: `radial-gradient(circle at 84% 8%, rgba(0,49,83,.58), transparent 30%), radial-gradient(circle at 12% 88%, rgba(0,255,255,.06), transparent 28%)`,
        }}
      >
        <Box sx={{ minHeight: "100vh", p: { xs: 2, sm: 3, md: 4 } }}>
          <Stack
            direction="row"
            justifyContent="space-between"
            alignItems="center"
            sx={{ borderBottom: `1px solid ${colors.line}`, pb: 2 }}
          >
            <Stack direction="row" spacing={1.5} alignItems="center">
              <Box
                sx={{
                  display: "flex",
                  width: "42px",
                  height: "42px",
                  alignItems: "center",
                  justifyContent: "center",
                  border: `1px solid ${colors.cyan}99`,
                  backgroundColor: colors.panel,
                  color: colors.cyan,
                  boxShadow: "0 0 18px rgba(15,255,255,.22)",
                }}
              >
                <IconGlyph name="shield" size={22} />
              </Box>
              <Box>
                <Typography
                  variant="h6"
                  sx={{
                    color: colors.white,
                    fontFamily: fonts.mono,
                    fontWeight: 600,
                    lineHeight: 1.1,
                  }}
                >
                  Smart Anti-Theft
                </Typography>
                <Typography
                  variant="caption"
                  sx={{ ...textStyles.label, display: "block", mt: 0.5, color: colors.textMuted }}
                >
                  {t("authGateEyebrow")}
                </Typography>
              </Box>
            </Stack>
            <Stack direction="row" spacing={1} alignItems="center">
              <LanguageSwitcher />
              <Chip
                label={t("liveSecure")}
                size="small"
                variant="outlined"
                sx={{
                  height: "25px",
                  borderRadius: "0px",
                  borderColor: `${colors.cyan}59`,
                  color: colors.cyan,
                  fontFamily: fonts.mono,
                  fontSize: "10px",
                  letterSpacing: "0.16em",
                  textTransform: "uppercase",
                }}
              />
            </Stack>
          </Stack>

          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: {
                xs: "1fr",
                lg: "minmax(180px, .65fr) minmax(360px, 480px) minmax(180px, .65fr)",
              },
              alignItems: "center",
              gap: { xs: 3, lg: 6 },
              minHeight: "calc(100vh - 120px)",
              maxWidth: "1320px",
              mx: "auto",
            }}
          >
            <Box sx={{ display: { xs: "none", lg: "block" } }}>
              <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>
                {t("secureChannel")}
              </Typography>
              <Typography
                variant="body2"
                sx={{ mt: 1.5, color: colors.textSecondary, lineHeight: 1.7 }}
              >
                {t("secureChannelDescription")}
              </Typography>
              <Stack spacing={1.25} sx={{ mt: 3 }}>
                <StatusLine
                  icon="lock"
                  label={language === "en" ? "TLS session" : "Phiên TLS"}
                  value={language === "en" ? "ARMED" : "SẴN SÀNG"}
                />
                <StatusLine
                  icon="fingerprint"
                  label={language === "en" ? "Identity proof" : "Bằng chứng danh tính"}
                  value={language === "en" ? "READY" : "SẴN SÀNG"}
                />
                <StatusLine
                  icon="verified_user"
                  label={language === "en" ? "Recovery mode" : "Chế độ khôi phục"}
                  value={language === "en" ? "ONLINE" : "TRỰC TUYẾN"}
                />
              </Stack>
            </Box>

            <SentinelSurface
              component="section"
              aria-labelledby="auth-title"
              elevation={0}
              sx={{
                borderRadius: "0px",
                p: { xs: 2.5, sm: 3.5 },
                backgroundColor: `${colors.panel}CC`,
              }}
            >
              <Typography variant="caption" sx={{ ...textStyles.label, color: colors.cyan }}>
                {eyebrow}
              </Typography>
              <Typography
                id="auth-title"
                component="h1"
                variant="h4"
                sx={{
                  mt: 1,
                  color: colors.white,
                  fontFamily: fonts.mono,
                  fontWeight: 600,
                  letterSpacing: "-0.02em",
                }}
              >
                {title}
              </Typography>
              <Typography
                variant="body2"
                sx={{ mt: 1, color: colors.textSecondary, lineHeight: 1.6 }}
              >
                {subtitle}
              </Typography>
              <Box sx={{ mt: 3 }}>{children}</Box>
            </SentinelSurface>

            <Box
              sx={{ display: { xs: "none", lg: "block" }, justifySelf: "end", textAlign: "right" }}
            >
              <Typography variant="caption" sx={{ ...textStyles.label, color: colors.textMuted }}>
                {t("privacyFirstConsole")}
              </Typography>
              <Typography
                variant="body2"
                sx={{ mt: 1.5, color: colors.textSecondary, lineHeight: 1.7 }}
              >
                {t("privacyNotice")}
              </Typography>
              <Typography
                variant="caption"
                sx={{ ...textStyles.mono, display: "block", mt: 3, color: colors.textMuted }}
              >
                v2.4.1 · {language === "en" ? "END-TO-END ENCRYPTED" : "MÃ HÓA CUỐI KÈM ĐẦU"}
              </Typography>
            </Box>
          </Box>

          <Typography
            component="footer"
            variant="caption"
            sx={{
              ...textStyles.label,
              display: "block",
              borderTop: `1px solid ${colors.line}`,
              pt: 1.5,
              color: colors.textMuted,
            }}
          >
            {footer || t("privacyFirst")}
          </Typography>
        </Box>
      </Box>
    </>
  );
}

function StatusLine({ icon, label, value }) {
  return (
    <Stack
      direction="row"
      spacing={1}
      alignItems="center"
      justifyContent="space-between"
      sx={{ borderLeft: `1px solid ${colors.cyan}59`, pl: 1.25 }}
    >
      <Stack direction="row" spacing={1} alignItems="center">
        <IconGlyph name={icon} size={15} color={colors.cyan} />
        <Typography variant="caption" sx={{ color: colors.textSecondary }}>
          {label}
        </Typography>
      </Stack>
      <Typography variant="caption" sx={{ ...textStyles.mono, color: colors.cyan }}>
        {value}
      </Typography>
    </Stack>
  );
}

export default SentinelAuthLayout;
