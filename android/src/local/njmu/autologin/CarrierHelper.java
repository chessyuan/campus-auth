package local.njmu.autologin;

import java.util.Locale;

public class CarrierHelper {
    public static final String CARRIER_CMCC = "cmcc";
    public static final String CARRIER_TELECOM = "telecom";
    public static final String CARRIER_UNICOM = "unicom";
    public static final String CARRIER_CAMPUS = "campus";

    public static final String[] CARRIER_KEYS = {
        CARRIER_CMCC,
        CARRIER_TELECOM,
        CARRIER_UNICOM,
        CARRIER_CAMPUS
    };

    public static final String[] CARRIER_LABELS = {
        "中国移动 (@cmcc)",
        "中国电信 (@telecom)",
        "中国联通 (@unicom)",
        "校园内网 (无后缀)"
    };

    // Fail closed on malformed/unknown suffixes instead of silently dropping part of the account.
    public static boolean isValidUsernameInput(String input) {
        if (input == null || input.trim().isEmpty()) return false;
        String s = input.trim();
        if (s.matches(".*\\s+.*")) return false;
        int firstAt = s.indexOf('@');
        if (firstAt < 0) return true;
        if (firstAt == 0 || firstAt != s.lastIndexOf('@')) return false;
        String suffix = s.substring(firstAt + 1).toLowerCase(Locale.ROOT);
        return "cmcc".equals(suffix) || "telecom".equals(suffix) || "unicom".equals(suffix);
    }

    public static String cleanUsername(String input) {
        if (input == null) return "";
        String u = input.trim();
        int at = u.indexOf('@');
        if (at >= 0) {
            u = u.substring(0, at).trim();
        }
        return u;
    }

    public static String inferCarrier(String input, String fallback) {
        if (input == null) return fallback != null ? fallback : CARRIER_CMCC;
        String lower = input.trim().toLowerCase(Locale.ROOT);
        if (lower.endsWith("@cmcc")) return CARRIER_CMCC;
        if (lower.endsWith("@telecom")) return CARRIER_TELECOM;
        if (lower.endsWith("@unicom")) return CARRIER_UNICOM;
        return fallback != null ? fallback : CARRIER_CMCC;
    }

    public static String formatUsername(String rawUser, String carrier) {
        String base = cleanUsername(rawUser);
        if (carrier == null) carrier = CARRIER_CMCC;
        carrier = carrier.trim().toLowerCase(Locale.ROOT);

        if (CARRIER_TELECOM.equals(carrier)) {
            return base + "@telecom";
        } else if (CARRIER_UNICOM.equals(carrier)) {
            return base + "@unicom";
        } else if (CARRIER_CAMPUS.equals(carrier)) {
            return base;
        } else {
            return base + "@cmcc";
        }
    }

    public static String getCarrierLabel(String carrier) {
        if (CARRIER_TELECOM.equalsIgnoreCase(carrier)) return "中国电信";
        if (CARRIER_UNICOM.equalsIgnoreCase(carrier)) return "中国联通";
        if (CARRIER_CAMPUS.equalsIgnoreCase(carrier)) return "校园内网";
        return "中国移动";
    }

    public static int getCarrierIndex(String carrier) {
        for (int i = 0; i < CARRIER_KEYS.length; i++) {
            if (CARRIER_KEYS[i].equalsIgnoreCase(carrier)) {
                return i;
            }
        }
        return 0;
    }
}