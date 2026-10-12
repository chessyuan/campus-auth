package local.njmu.autologin;

import android.graphics.Color;

public final class OneUiPalette {
    public static final String MODE_SYSTEM = "SYSTEM";
    public static final String MODE_LIGHT = "LIGHT";
    public static final String MODE_DARK = "DARK";
    public static final String MODE_AMOLED = "AMOLED";

    public static final String[] THEME_MODES = {
        MODE_SYSTEM,
        MODE_LIGHT,
        MODE_DARK,
        MODE_AMOLED
    };

    public static final String[] THEME_MODE_LABELS = {
        "跟随系统",
        "浅色模式",
        "深色模式",
        "AMOLED 纯黑"
    };

    public static final String[] THEME_MODE_DESCRIPTIONS = {
        "自适应设备系统深浅色切换",
        "明亮清新，通透的 One UI 卡片体验",
        "沉浸护眼，低对比度暗色界面",
        "极致纯黑背景，更省电的视效体验"
    };

    public static final String DEFAULT_SEED_HEX = "#2189FF";

    public static final class Swatch {
        public final String name;
        public final String hex;
        public final int color;

        public Swatch(String name, String hex, int color) {
            this.name = name;
            this.hex = hex;
            this.color = color;
        }
    }

    public static final Swatch[] PRESET_SWATCHES = new Swatch[] {
        new Swatch("One UI 蓝", "#2189FF", 0xFF2189FF),
        new Swatch("翡翠绿", "#28A879", 0xFF28A879),
        new Swatch("薰衣草紫", "#8570E8", 0xFF8570E8),
        new Swatch("珊瑚粉", "#EC789A", 0xFFEC789A),
        new Swatch("琥珀橙", "#E8A13B", 0xFFE8A13B),
        new Swatch("青绿色", "#16A6B6", 0xFF16A6B6),
        new Swatch("石墨灰", "#687684", 0xFF687684),
        new Swatch("绯红色", "#DA626B", 0xFFDA626B)
    };

    public static final class PaletteColors {
        public final int background;
        public final int surface;
        public final int surfaceContainer;
        public final int surfaceContainerHigh;
        public final int outline;
        public final int outlineVariant;
        public final int textPrimary;
        public final int textSecondary;
        public final int primary;
        public final int onPrimary;
        public final int primaryContainer;
        public final int onPrimaryContainer;
        public final int secondaryContainer;
        public final int onSecondaryContainer;
        public final int tertiaryContainer;
        public final int onTertiaryContainer;
        public final int success;
        public final int warning;
        public final int error;
        public final int neutralDot;
        public final boolean isDark;

        public PaletteColors(int background, int surface, int surfaceContainer, int surfaceContainerHigh,
                             int outline, int outlineVariant, int textPrimary, int textSecondary,
                             int primary, int onPrimary, int primaryContainer, int onPrimaryContainer,
                             int secondaryContainer, int onSecondaryContainer,
                             int tertiaryContainer, int onTertiaryContainer,
                             int success, int warning, int error, int neutralDot, boolean isDark) {
            this.background = background;
            this.surface = surface;
            this.surfaceContainer = surfaceContainer;
            this.surfaceContainerHigh = surfaceContainerHigh;
            this.outline = outline;
            this.outlineVariant = outlineVariant;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
            this.primary = primary;
            this.onPrimary = onPrimary;
            this.primaryContainer = primaryContainer;
            this.onPrimaryContainer = onPrimaryContainer;
            this.secondaryContainer = secondaryContainer;
            this.onSecondaryContainer = onSecondaryContainer;
            this.tertiaryContainer = tertiaryContainer;
            this.onTertiaryContainer = onTertiaryContainer;
            this.success = success;
            this.warning = warning;
            this.error = error;
            this.neutralDot = neutralDot;
            this.isDark = isDark;
        }
    }

    private OneUiPalette() {}

    public static int parseColor(String hex, int fallback) {
        if (hex == null) return fallback;
        String clean = hex.trim();
        if (clean.startsWith("#")) clean = clean.substring(1);
        try {
            if (clean.length() == 6) {
                return (int) (Long.parseLong(clean, 16) | 0xFF000000L);
            } else if (clean.length() == 8) {
                return (int) Long.parseLong(clean, 16);
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    public static boolean isValidHex(String hex) {
        if (hex == null) return false;
        String clean = hex.trim();
        if (clean.startsWith("#")) clean = clean.substring(1);
        return clean.matches("^[0-9a-fA-F]{6}$|^[0-9a-fA-F]{8}$");
    }

    public static float[] colorToHsl(int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h = 0f;
        float s = 0f;
        float l = (max + min) / 2f;

        if (delta != 0f) {
            s = (l < 0.5f) ? (delta / (max + min)) : (delta / (2f - max - min));
            if (max == r) {
                h = ((g - b) / delta + (g < b ? 6f : 0f)) * 60f;
            } else if (max == g) {
                h = ((b - r) / delta + 2f) * 60f;
            } else {
                h = ((r - g) / delta + 4f) * 60f;
            }
        }
        h = (h % 360f + 360f) % 360f;
        s = Math.max(0f, Math.min(1f, s));
        l = Math.max(0f, Math.min(1f, l));
        return new float[]{h, s, l};
    }

    public static int hslToColor(float h, float s, float l) {
        float normH = (h % 360f + 360f) % 360f;
        float sat = Math.max(0f, Math.min(1f, s));
        float lum = Math.max(0f, Math.min(1f, l));

        float c = (1f - Math.abs(2f * lum - 1f)) * sat;
        float x = c * (1f - Math.abs((normH / 60f) % 2f - 1f));
        float m = lum - c / 2f;

        float r1 = 0, g1 = 0, b1 = 0;
        int sextant = (int) (normH / 60f);
        switch (sextant) {
            case 0: r1 = c; g1 = x; b1 = 0; break;
            case 1: r1 = x; g1 = c; b1 = 0; break;
            case 2: r1 = 0; g1 = c; b1 = x; break;
            case 3: r1 = 0; g1 = x; b1 = c; break;
            case 4: r1 = x; g1 = 0; b1 = c; break;
            default: r1 = c; g1 = 0; b1 = x; break;
        }

        int r = Math.round((r1 + m) * 255f);
        int g = Math.round((g1 + m) * 255f);
        int b = Math.round((b1 + m) * 255f);
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static double calculateLuminance(int color) {
        double r = ((color >> 16) & 0xFF) / 255.0;
        double g = ((color >> 8) & 0xFF) / 255.0;
        double b = (color & 0xFF) / 255.0;
        r = (r <= 0.03928) ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = (g <= 0.03928) ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = (b <= 0.03928) ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    public static double calculateContrastRatio(int fg, int bg) {
        double lum1 = calculateLuminance(fg);
        double lum2 = calculateLuminance(bg);
        double lighter = Math.max(lum1, lum2);
        double darker = Math.min(lum1, lum2);
        return (lighter + 0.05) / (darker + 0.05);
    }

    public static PaletteColors resolvePalette(String mode, int seedColor, boolean isSystemNight) {
        String effectiveMode = mode;
        if (effectiveMode == null || effectiveMode.isEmpty() || MODE_SYSTEM.equals(effectiveMode)) {
            effectiveMode = isSystemNight ? MODE_DARK : MODE_LIGHT;
        }

        float[] hsl = colorToHsl(seedColor);

        if (MODE_LIGHT.equals(effectiveMode)) {
            int primary = hslToColor(hsl[0], Math.max(0.65f, hsl[1]), 0.44f);
            int onPrimary = calculateContrastRatio(0xFFFFFFFF, primary) >= 4.5
                ? 0xFFFFFFFF : 0xFF000000;
            float primContSat = Math.max(0.20f, Math.min(0.45f, hsl[1] * 0.45f));
            int primaryContainer = hslToColor(hsl[0], primContSat, 0.90f);
            int onPrimaryContainer = hslToColor(hsl[0], hsl[1], 0.14f);

            float secHue = (hsl[0] + 12f) % 360f;
            float secSat = Math.max(0.15f, Math.min(0.35f, hsl[1] * 0.35f));
            int secondaryContainer = hslToColor(secHue, secSat, 0.91f);
            int onSecondaryContainer = hslToColor(secHue, hsl[1], 0.16f);

            float terHue = (hsl[0] + 55f) % 360f;
            float terSat = Math.max(0.15f, Math.min(0.40f, hsl[1] * 0.40f));
            int tertiaryContainer = hslToColor(terHue, terSat, 0.90f);
            int onTertiaryContainer = hslToColor(terHue, hsl[1], 0.15f);

            return new PaletteColors(
                0xFFF7F9FB, // background
                0xFFFFFFFF, // surface (card)
                0xFFF1F4F9, // surfaceContainer
                0xFFE9EDF3, // surfaceContainerHigh
                0xFFD4D8E0, // outline
                0xFFE2E5EC, // outlineVariant
                0xFF191C1E, // textPrimary
                0xFF5A606A, // textSecondary
                primary,
                onPrimary,
                primaryContainer,
                onPrimaryContainer,
                secondaryContainer,
                onSecondaryContainer,
                tertiaryContainer,
                onTertiaryContainer,
                0xFF28A879, // success
                0xFFE8A13B, // warning
                0xFFDA626B, // error
                0xFF8E95A2, // neutralDot
                false
            );
        } else if (MODE_AMOLED.equals(effectiveMode)) {
            int primary = hslToColor(hsl[0], Math.max(0.60f, hsl[1] * 0.95f), 0.75f);
            int onPrimary = 0xFF000000;
            float primContSat = Math.max(0.40f, hsl[1] * 0.80f);
            int primaryContainer = hslToColor(hsl[0], primContSat, 0.22f);
            float primOnContSat = Math.max(0.20f, Math.min(0.50f, hsl[1] * 0.50f));
            int onPrimaryContainer = hslToColor(hsl[0], primOnContSat, 0.92f);

            float secHue = (hsl[0] + 12f) % 360f;
            float secSat = Math.max(0.20f, Math.min(0.45f, hsl[1] * 0.60f));
            int secondaryContainer = hslToColor(secHue, secSat, 0.22f);
            int onSecondaryContainer = hslToColor(secHue, hsl[1], 0.90f);

            float terHue = (hsl[0] + 55f) % 360f;
            float terSat = Math.max(0.20f, Math.min(0.50f, hsl[1] * 0.60f));
            int tertiaryContainer = hslToColor(terHue, terSat, 0.22f);
            int onTertiaryContainer = hslToColor(terHue, hsl[1], 0.90f);

            return new PaletteColors(
                0xFF000000, // background
                0xFF101012, // surface (card)
                0xFF101012, // surfaceContainer
                0xFF18191D, // surfaceContainerHigh
                0xFF383A40, // outline
                0xFF24252A, // outlineVariant
                0xFFEAEAEA, // textPrimary
                0xFF9CA0AA, // textSecondary
                primary,
                onPrimary,
                primaryContainer,
                onPrimaryContainer,
                secondaryContainer,
                onSecondaryContainer,
                tertiaryContainer,
                onTertiaryContainer,
                0xFF28A879, // success
                0xFFE8A13B, // warning
                0xFFDA626B, // error
                0xFF687684, // neutralDot
                true
            );
        } else {
            // DARK
            int primary = hslToColor(hsl[0], Math.max(0.55f, hsl[1] * 0.90f), 0.74f);
            int proposedOnPrimary = hslToColor(hsl[0], hsl[1], 0.16f);
            int onPrimary = calculateContrastRatio(proposedOnPrimary, primary) >= 4.5
                ? proposedOnPrimary : 0xFF000000;
            int primaryContainer = hslToColor(hsl[0], hsl[1], 0.30f);
            float primOnContSat = Math.max(0.20f, Math.min(0.60f, hsl[1] * 0.60f));
            int onPrimaryContainer = hslToColor(hsl[0], primOnContSat, 0.88f);

            float secHue = (hsl[0] + 12f) % 360f;
            float secSat = Math.max(0.20f, Math.min(0.40f, hsl[1] * 0.50f));
            int secondaryContainer = hslToColor(secHue, secSat, 0.32f);
            int onSecondaryContainer = hslToColor(secHue, hsl[1], 0.88f);

            float terHue = (hsl[0] + 55f) % 360f;
            float terSat = Math.max(0.20f, Math.min(0.45f, hsl[1] * 0.50f));
            int tertiaryContainer = hslToColor(terHue, terSat, 0.32f);
            int onTertiaryContainer = hslToColor(terHue, hsl[1], 0.88f);

            return new PaletteColors(
                0xFF131418, // background
                0xFF1C1E23, // surface (card)
                0xFF202228, // surfaceContainer
                0xFF292B32, // surfaceContainerHigh
                0xFF8D9099, // outline
                0xFF43464F, // outlineVariant
                0xFFE2E2E6, // textPrimary
                0xFFA0A3AE, // textSecondary
                primary,
                onPrimary,
                primaryContainer,
                onPrimaryContainer,
                secondaryContainer,
                onSecondaryContainer,
                tertiaryContainer,
                onTertiaryContainer,
                0xFF28A879, // success
                0xFFE8A13B, // warning
                0xFFDA626B, // error
                0xFF727682, // neutralDot
                true
            );
        }
    }
}