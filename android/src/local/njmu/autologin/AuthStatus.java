package local.njmu.autologin;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AuthStatus {
    public static final String STATE_DISABLED = "DISABLED";
    public static final String STATE_NO_WIFI = "NO_WIFI";
    public static final String STATE_CHECKING = "CHECKING";
    public static final String STATE_ONLINE = "ONLINE";
    public static final String STATE_AUTHENTICATING = "AUTHENTICATING";
    public static final String STATE_VERIFIED = "VERIFIED";
    public static final String STATE_DENIED = "DENIED";
    public static final String STATE_RETRY = "RETRY";
    public static final String STATE_FOREIGN_PORTAL = "FOREIGN_PORTAL";
    public static final String STATE_ERROR = "ERROR";

    public static final int COLOR_GRAY_LIGHT = 0xFF66758A;
    public static final int COLOR_GRAY_DARK  = 0xFFA7B4C3;
    public static final int COLOR_BLUE_LIGHT = 0xFF3469CF;
    public static final int COLOR_BLUE_DARK  = 0xFF8EBAFF;
    public static final int COLOR_GREEN      = 0xFF2E957D;
    public static final int COLOR_AMBER      = 0xFFB57A2D;
    public static final int COLOR_RED        = 0xFFC84B50;

    public final String state;
    public final String description;
    public final long timestamp;
    public final int httpCode;
    public final String wifiIp;

    public AuthStatus(String state, String description, long timestamp, int httpCode, String wifiIp) {
        this.state = state != null ? state : STATE_DISABLED;
        this.description = description != null ? description : "";
        this.timestamp = timestamp;
        this.httpCode = httpCode;
        this.wifiIp = wifiIp != null ? wifiIp : "";
    }

    public static void save(Context c, String state, String desc, int httpCode, String ip) {
        long now = System.currentTimeMillis();
        SharedPreferences.Editor ed = c.getSharedPreferences("auth", 0).edit();
        ed.putString("status_state", state);
        ed.putString("status_desc", desc);
        ed.putLong("status_time", now);
        ed.putInt("status_http_code", httpCode);
        if (ip != null) {
            ed.putString("status_wifi_ip", ip);
        }
        // Backward compatibility for v1.4 observers
        String timeStr = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(now));
        ed.putString("status", desc + "\n" + timeStr);
        ed.apply();
    }

    public static AuthStatus load(Context c) {
        SharedPreferences p = c.getSharedPreferences("auth", 0);
        boolean enabled = p.getBoolean("enabled", false);
        String defaultState = enabled ? STATE_CHECKING : STATE_DISABLED;
        String defaultDesc = enabled ? "正在准备检查校园网" : "自动认证已暂停";

        String st = p.getString("status_state", defaultState);
        String desc = p.getString("status_desc", p.getString("status", defaultDesc));
        long time = p.getLong("status_time", 0L);
        int code = p.getInt("status_http_code", 0);
        String ip = p.getString("status_wifi_ip", "");
        return new AuthStatus(st, desc, time, code, ip);
    }

    public static String getTitle(String state) {
        if (STATE_ONLINE.equals(state) || STATE_VERIFIED.equals(state)) {
            return "已验证联网";
        } else if (STATE_NO_WIFI.equals(state)) {
            return "等待 Wi-Fi";
        } else if (STATE_AUTHENTICATING.equals(state)) {
            return "正在认证";
        } else if (STATE_CHECKING.equals(state)) {
            return "正在检测网络";
        } else if (STATE_RETRY.equals(state)) {
            return "等待重试";
        } else if (STATE_DENIED.equals(state)) {
            return "认证被拒绝";
        } else if (STATE_FOREIGN_PORTAL.equals(state)) {
            return "需认证 (外部网络)";
        } else if (STATE_ERROR.equals(state)) {
            return "检测异常";
        } else {
            return "已暂停";
        }
    }

    public static int getDotColor(String state, boolean isDark) {
        if (STATE_ONLINE.equals(state) || STATE_VERIFIED.equals(state)) {
            return COLOR_GREEN;
        } else if (STATE_AUTHENTICATING.equals(state) || STATE_RETRY.equals(state)) {
            return COLOR_AMBER;
        } else if (STATE_CHECKING.equals(state)) {
            return isDark ? COLOR_BLUE_DARK : COLOR_BLUE_LIGHT;
        } else if (STATE_DENIED.equals(state) || STATE_ERROR.equals(state)) {
            return COLOR_RED;
        } else {
            return isDark ? COLOR_GRAY_DARK : COLOR_GRAY_LIGHT;
        }
    }
}