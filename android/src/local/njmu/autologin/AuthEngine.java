package local.njmu.autologin;

import android.content.*;
import android.net.*;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONObject;

/** Single authenticated Wi-Fi probe, shared by persistent and scheduled modes. */
final class AuthEngine {
    private final Context context;
    private final ConnectivityManager cm;
    private final Network preferredWifi;
    private final boolean requiredQuiet;
    private final java.net.CookieManager cookies =
        new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    AuthEngine(Context ctx, Network wifi, boolean quiet) {
        context=ctx.getApplicationContext();
        cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        preferredWifi=wifi;
        requiredQuiet=quiet;
    }
    void status(String s,String code){
        String t=new SimpleDateFormat("MM-dd HH:mm:ss").format(new Date());
        context.getSharedPreferences("auth",0).edit()
            .putString("status",s).putString("statusTime",t).putString("stateCode",code)
            .putLong("lastCheckTimeMs",System.currentTimeMillis())
            .apply();
    }
    static class Response {
        int code;
        String location, body;
        Response(int c, String l, String b) { code = c; location = l; body = b; }
    }

    Response request(Network net, String url, String post) throws Exception {
        HttpURLConnection c = (HttpURLConnection) net.openConnection(new URL(url), java.net.Proxy.NO_PROXY);
        c.setConnectTimeout(7000);
        c.setReadTimeout(7000);
        c.setInstanceFollowRedirects(false);
        c.setUseCaches(false);
        for (java.util.Map.Entry<String, java.util.List<String>> h : cookies.get(new URI(url), java.util.Collections.emptyMap()).entrySet()) {
            c.setRequestProperty(h.getKey(), android.text.TextUtils.join("; ", h.getValue()));
        }
        if (post != null) {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            c.setRequestProperty("Referer", "https://wlan.njmu.edu.cn/guest/wifi_login.php");
            c.setRequestProperty("Origin", "https://wlan.njmu.edu.cn");
            byte[] bytes = post.getBytes("UTF-8");
            c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream o = c.getOutputStream()) { o.write(bytes); }
        }
        int code = c.getResponseCode();
        cookies.put(new URI(url), c.getHeaderFields());
        String loc = c.getHeaderField("Location");
        String body = "";
        if (code == 200) {
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] b = new byte[512];
                int n;
                while (out.size() < 4096 && (n = in.read(b)) != -1) out.write(b, 0, n);
                body = out.toString("UTF-8");
            }
        }
        c.disconnect();
        return new Response(code, loc, body);
    }

    boolean online(Response r) {
        return r.code == 200 && r.body.trim().equals("Microsoft Connect Test");
    }

    boolean schoolRedirect(Response r) {
        try {
            URL u = new URL(r.location);
            return u.getHost().equalsIgnoreCase("wlan.njmu.edu.cn") && u.getPath().equals("/guest/wifi_login.php");
        } catch (Exception e) { return false; }
    }

    Response followPortal(Network net, Response r) throws Exception {
        for (int i = 0; i < 4 && !online(r) && !schoolRedirect(r); i++) {
            String next = r.location;
            if (next == null && r.code == 200) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)url\\s*=\\s*([^'\"<>\\s]+)").matcher(r.body);
                if (m.find()) next = m.group(1).replace("&amp;", "&");
            }
            if (next == null) break;
            URL u = new URL(next);
            String h = u.getHost();
            if (!(u.getProtocol().equals("http") || u.getProtocol().equals("https"))) break;
            if (!(h.equals("www.msftconnecttest.com") || h.equals("wlan.njmu.edu.cn") || h.equals("captiveportal-login.njmu.edu.cn"))) break;
            r = request(net, next, null);
        }
        return r;
    }

    private static final java.util.concurrent.atomic.AtomicBoolean CHECK_BUSY = new java.util.concurrent.atomic.AtomicBoolean(false);
    void check() {
        if (!CHECK_BUSY.compareAndSet(false, true)) return;
        try { checkOnce(); }
        finally { CHECK_BUSY.set(false); }
    }
    private void checkOnce() {
        try {
            if (requiredQuiet && !AuthScheduler.isQuietMode(context)) return;
            if (!requiredQuiet && AuthScheduler.isQuietMode(context)) return;
            if (!context.getSharedPreferences("auth", 0).getBoolean("enabled", false)) {
                status("自动认证已暂停", "DISABLED");
                return;
            }
            Network wifi = preferredWifi;
            if (wifi == null) {
                for (Network n : cm.getAllNetworks()) {
                    NetworkCapabilities c = cm.getNetworkCapabilities(n);
                    if (c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        wifi = n;
                        break;
                    }
                }
            }
            if (wifi == null) {
                context.getSharedPreferences("auth", 0).edit().putString("wifiIpv4", "未获取").apply();
                status("等待 Wi-Fi，未连接校园网", "NO_WIFI");
                return;
            }
            LinkProperties props = cm.getLinkProperties(wifi);
            if (props != null) {
                for (LinkAddress addr : props.getLinkAddresses()) {
                    if (addr.getAddress() instanceof Inet4Address && !addr.getAddress().isLoopbackAddress()) {
                        context.getSharedPreferences("auth", 0).edit().putString("wifiIpv4", addr.getAddress().getHostAddress()).apply();
                        break;
                    }
                }
            }
            Response probe = request(wifi, "http://www.msftconnecttest.com/connecttest.txt", null);
            if (online(probe)) {
                context.getSharedPreferences("auth", 0).edit().putLong("attempt", 0).apply();
                status("已验证联网（无需认证）", "ONLINE");
                return;
            }
            probe = followPortal(wifi, probe);
            if (!schoolRedirect(probe)) {
                status("未发现南医大认证跳转", "FOREIGN_PORTAL");
                return;
            }
            long last = context.getSharedPreferences("auth", 0).getLong("attempt", 0);
            long retryDelay = context.getSharedPreferences("auth", 0).getLong("retryDelay", 15000);
            if (System.currentTimeMillis() - last < retryDelay) {
                status("等待重试间隔冷却中", "RETRY");
                return;
            }

            JSONObject cred = new JSONObject(Vault.load(context));
            String carrier = context.getSharedPreferences("auth", 0).getString("carrier", "cmcc");
            String userFormatted = AuthService.formatUsername(cred.getString("username"), carrier);
            String carrierLabel = AuthService.getCarrierLabel(carrier);

            status("正在提交认证 (" + carrierLabel + ")...", "AUTHENTICATING");
            AuthLog.log(context, "AUTH", "向学校网关提交认证 · " + carrierLabel);

            request(wifi, probe.location, null);
            String body = "username=" + URLEncoder.encode(userFormatted, "UTF-8")
                + "&password=" + URLEncoder.encode(cred.getString("password"), "UTF-8");
            context.getSharedPreferences("auth", 0).edit().putLong("attempt", System.currentTimeMillis()).putLong("retryDelay", 15000).apply();

            // Pause can be requested while a previous portal request is in flight.
            if (!context.getSharedPreferences("auth", 0).getBoolean("enabled", false)
                    || (requiredQuiet != AuthScheduler.isQuietMode(context))) {
                status("认证模式已切换或已暂停", "DISABLED");
                return;
            }
            Response auth = request(wifi, "https://captiveportal-login.njmu.edu.cn/auth/index.html/u", body);
            boolean isDenied = auth.location != null && java.net.URLDecoder.decode(auth.location, "UTF-8").contains("Access denied");
            String resMsg = "HTTP " + auth.code + (isDenied ? " · 拒绝访问" : " · 已提交");
            context.getSharedPreferences("auth", 0).edit().putString("lastResult", resMsg).apply();

            Response follow = auth;
            for (int hop = 0; hop < 3 && follow.location != null; hop++) {
                URL next = new URL(new URL("https://captiveportal-login.njmu.edu.cn/auth/index.html/u"), follow.location);
                if (!next.getProtocol().equals("https") || !(next.getHost().equals("wlan.njmu.edu.cn") || next.getHost().equals("captiveportal-login.njmu.edu.cn"))) break;
                follow = request(wifi, next.toString(), null);
            }

            Thread.sleep(3000);
            if (online(request(wifi, "http://www.msftconnecttest.com/connecttest.txt", null))) {
                cm.reportNetworkConnectivity(wifi, true);
                String successTime = new SimpleDateFormat("MM-dd HH:mm:ss").format(new Date());
                context.getSharedPreferences("auth", 0).edit()
                    .putString("lastSuccess", successTime)
                    .putLong("attempt", 0)
                    .apply();
                status("已验证联网 · " + carrierLabel, "ONLINE");
                AuthLog.log(context, "SUCCESS", "认证成功，外网已连通 · " + carrierLabel);
            } else {
                if (isDenied) {
                    context.getSharedPreferences("auth", 0).edit().putLong("retryDelay", 600000).apply();
                    status("学校拒绝认证，十分钟后重试", "DENIED");
                    AuthLog.log(context, "ERROR", "学校服务器拒绝本次认证 (Access denied)");
                } else {
                    status("学校尚未确认联网，15 秒后重试", "RETRY");
                    AuthLog.log(context, "WARN", "网关已提交，等待连通性生效");
                }
            }
        } catch (Exception e) {
            status("等待连接（" + e.getClass().getSimpleName() + "）", "ERROR");
        }
    }

}
