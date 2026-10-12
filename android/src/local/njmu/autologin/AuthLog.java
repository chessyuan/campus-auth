package local.njmu.autologin;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class AuthLog {
    public static final int MAX_ENTRIES = 50;

    public static final String TYPE_SUCCESS = "SUCCESS";
    public static final String TYPE_INFO    = "INFO";
    public static final String TYPE_WARN    = "WARN";
    public static final String TYPE_ERROR   = "ERROR";

    public static class Entry {
        public final long timestamp;
        public final String type;
        public final String summary;

        public Entry(long timestamp, String type, String summary) {
            this.timestamp = timestamp;
            this.type = type != null ? type : TYPE_INFO;
            this.summary = summary != null ? summary : "";
        }

        public JSONObject toJson() {
            try {
                JSONObject o = new JSONObject();
                o.put("t", timestamp);
                o.put("k", type);
                o.put("m", summary);
                return o;
            } catch (Exception e) {
                return new JSONObject();
            }
        }

        public static Entry fromJson(JSONObject o) {
            if (o == null) return null;
            long t = o.optLong("t", 0L);
            String k = o.optString("k", TYPE_INFO);
            String m = o.optString("m", "");
            return new Entry(t, k, m);
        }
    }

    /**
     * Sanitizes any accidental password or credentials query parameters.
     */
    public static String sanitize(String input) {
        if (input == null) return "";
        String s = input;
        s = s.replaceAll("(?i)password=[^&\\s]+", "password=***");
        s = s.replaceAll("(?i)pwd=[^&\\s]+", "pwd=***");
        s = s.replaceAll("(?i)secret=[^&\\s]+", "secret=***");
        return s;
    }

    public static synchronized void log(Context c, String type, String summary) {
        if (c == null || summary == null || summary.trim().isEmpty()) return;
        String clean = sanitize(summary.trim());

        List<Entry> list = loadEntries(c);
        long now = System.currentTimeMillis();

        // Dedup: if the most recent event has the exact same type & summary, and occurred within 10 minutes, ignore
        if (!list.isEmpty()) {
            Entry last = list.get(0);
            if (last.type.equals(type) && last.summary.equals(clean) && (now - last.timestamp < 10 * 60 * 1000L)) {
                return;
            }
        }

        list.add(0, new Entry(now, type, clean));
        while (list.size() > MAX_ENTRIES) {
            list.remove(list.size() - 1);
        }

        saveEntries(c, list);
    }

    public static synchronized List<Entry> loadEntries(Context c) {
        List<Entry> res = new ArrayList<>();
        if (c == null) return res;
        SharedPreferences p = c.getSharedPreferences("auth", 0);
        String raw = p.getString("auth_history_json", "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    res.add(Entry.fromJson(obj));
                }
            }
        } catch (Exception ignored) {
        }
        return res;
    }

    private static synchronized void saveEntries(Context c, List<Entry> list) {
        if (c == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (Entry e : list) {
                arr.put(e.toJson());
            }
            c.getSharedPreferences("auth", 0).edit()
                    .putString("auth_history_json", arr.toString())
                    .apply();
        } catch (Exception ignored) {
        }
    }

    // Backward-compatible view adapter for the lightweight dashboard.
    public static synchronized List<JSONObject> getLogs(Context c) {
        List<JSONObject> rows = new ArrayList<>();
        for (Entry entry : loadEntries(c)) {
            try {
                JSONObject row = new JSONObject();
                row.put("time", new java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date(entry.timestamp)));
                row.put("type", entry.type);
                row.put("msg", entry.summary);
                row.put("count", 1);
                rows.add(row);
            } catch (Exception ignored) {}
        }
        return rows;
    }

    public static synchronized void clear(Context c) {
        if (c == null) return;
        c.getSharedPreferences("auth", 0).edit()
                .putString("auth_history_json", "[]")
                .apply();
    }
}