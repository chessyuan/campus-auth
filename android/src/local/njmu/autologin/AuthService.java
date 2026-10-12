package local.njmu.autologin;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;
import org.json.JSONObject;

public class AuthService extends Service {
    java.net.CookieManager cookies = new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    volatile Network requestedWifi;
    long lastKick = 0;
    ScheduledExecutorService worker;
    ConnectivityManager cm;
    ConnectivityManager.NetworkCallback callback;

    public static String getCarrierSuffix(String carrier) {
        if ("telecom".equalsIgnoreCase(carrier)) return "@telecom";
        if ("unicom".equalsIgnoreCase(carrier)) return "@unicom";
        if ("campus".equalsIgnoreCase(carrier)) return "";
        return "@cmcc";
    }

    public static String getCarrierLabel(String carrier) {
        if ("telecom".equalsIgnoreCase(carrier)) return "中国电信";
        if ("unicom".equalsIgnoreCase(carrier)) return "中国联通";
        if ("campus".equalsIgnoreCase(carrier)) return "校园内网";
        return "中国移动";
    }

    public static String formatUsername(String raw, String carrier) {
        if (raw == null) return "";
        String base = raw.contains("@") ? raw.substring(0, raw.indexOf("@")) : raw;
        return base + getCarrierSuffix(carrier);
    }

    void status(String s, String stateCode) {
        String timeStr = new SimpleDateFormat("MM-dd HH:mm:ss").format(new Date());
        getSharedPreferences("auth", 0).edit()
            .putString("status", s)
            .putString("statusTime", timeStr)
            .putString("stateCode", stateCode)
            .putLong("serviceHeartbeatMs", System.currentTimeMillis())
            .apply();
    }

    void extractAndSaveIp(LinkProperties p) {
        if (p == null) return;
        for (LinkAddress la : p.getLinkAddresses()) {
            InetAddress a = la.getAddress();
            if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                getSharedPreferences("auth", 0).edit().putString("wifiIpv4", a.getHostAddress()).apply();
                return;
            }
        }
    }

    public void onCreate() {
        super.onCreate();
        if (AuthScheduler.isQuietMode(this) || !AuthScheduler.isEnabled(this)) {
            stopSelf();
            return;
        }
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel("quiet", "后台认证状态", NotificationManager.IMPORTANCE_LOW);
        ch.setSound(null, null);
        ch.enableVibration(false);
        nm.createNotificationChannel(ch);

        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        startForeground(1, new Notification.Builder(this, "quiet")
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle("校园网自动认证运行中")
            .setContentText("后台静默检测，无需打开认证网页")
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build());

        getSharedPreferences("auth", 0).edit()
            .putBoolean("serviceRunning", true)
            .putLong("serviceHeartbeatMs", System.currentTimeMillis())
            .apply();

        cm = getSystemService(ConnectivityManager.class);
        worker = Executors.newSingleThreadScheduledExecutor();

        callback = new ConnectivityManager.NetworkCallback() {
            public void onAvailable(Network n) {
                requestedWifi = n;
                kick();
            }
            public void onLinkPropertiesChanged(Network n, LinkProperties p) {
                requestedWifi = n;
                extractAndSaveIp(p);
                kick();
            }
            public void onCapabilitiesChanged(Network n, NetworkCapabilities c) {
                requestedWifi = n;
                if (!c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) kick();
            }
            public void onLost(Network n) {
                if (n.equals(requestedWifi)) {
                    requestedWifi = null;
                    getSharedPreferences("auth", 0).edit().putString("wifiIpv4", "未连接").apply();
                    status("等待 Wi-Fi，未连接校园网", "NO_WIFI");
                }
            }
        };

        NetworkRequest req = new NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build();
        cm.requestNetwork(req, callback);
        worker.scheduleWithFixedDelay(() -> check(), 1, 10, TimeUnit.SECONDS);
    }

    public synchronized void kick() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastKick < 1500) return;
        lastKick = now;
        try {
            worker.schedule(() -> check(), 300, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {}
    }

    void check() {
        new AuthEngine(this, requestedWifi, false).check();
        getSharedPreferences("auth", 0).edit()
            .putLong("serviceHeartbeatMs", System.currentTimeMillis()).apply();
    }

    public int onStartCommand(Intent i, int flags, int id) {
        if (!getSharedPreferences("auth", 0).getBoolean("enabled", false) || AuthScheduler.isQuietMode(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (i != null && "local.njmu.autologin.CHECK_NOW".equals(i.getAction()) && worker != null) {
            try { worker.execute(this::check); } catch (RuntimeException ignored) {}
        } else {
            kick();
        }
        return START_STICKY;
    }

    public IBinder onBind(Intent i) { return null; }

    public void onDestroy() {
        getSharedPreferences("auth", 0).edit()
            .putBoolean("serviceRunning", false)
            .apply();
        try { if (cm != null && callback != null) cm.unregisterNetworkCallback(callback); } catch (Exception ignored) {}
        if (worker != null) worker.shutdownNow();
        super.onDestroy();
    }
}
