import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";

import { useLanguage } from "utils/i18n";

function LanguageSwitcher() {
  const { language, setLanguage } = useLanguage();

  return (
    <Stack direction="row" spacing={0.75} alignItems="center">
      <Button
        size="small"
        variant={language === "vi" ? "contained" : "outlined"}
        onClick={() => setLanguage("vi")}
        sx={{
          minWidth: "42px",
          borderRadius: "0px",
          px: 1.1,
          py: 0.4,
          textTransform: "uppercase",
          fontSize: "11px",
          fontWeight: 700,
        }}
      >
        VI
      </Button>
      <Button
        size="small"
        variant={language === "en" ? "contained" : "outlined"}
        onClick={() => setLanguage("en")}
        sx={{
          minWidth: "42px",
          borderRadius: "0px",
          px: 1.1,
          py: 0.4,
          textTransform: "uppercase",
          fontSize: "11px",
          fontWeight: 700,
        }}
      >
        EN
      </Button>
    </Stack>
  );
}

export default LanguageSwitcher;
