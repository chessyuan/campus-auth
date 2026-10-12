package local.njmu.autologin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.Color;

/** A self-contained native appearance surface inspired by the user's read-only One UI design tokens. */
public final class OneUiAppearance {
    public static final String PREFS = "appearance";
    private static final String MODE = "mode";
    private static final String HEX = "seed_hex";
    private static final String DYNAMIC = "dynamic_color";
    private static final String AMBIENT = "ambient_background";
    private OneUiAppearance() {}

    public static String mode(Context c) {
        String stored = c.getSharedPreferences(PREFS, 0).getString(MODE, OneUiPalette.MODE_SYSTEM);
        for (String valid : OneUiPalette.THEME_MODES) if (valid.equals(stored)) return stored;
        return OneUiPalette.MODE_SYSTEM;
    }
    public static String seedHex(Context c) {
        String s = c.getSharedPreferences(PREFS, 0).getString(HEX, OneUiPalette.DEFAULT_SEED_HEX);
        return OneUiPalette.isValidHex(s) ? s : OneUiPalette.DEFAULT_SEED_HEX;
    }
    public static boolean useDynamic(Context c) {
        return Build.VERSION.SDK_INT >= 31 && c.getSharedPreferences(PREFS, 0).getBoolean(DYNAMIC, false);
    }
    /**
     * Fixed, read-only watermark: the exact campus login identifier that will
     * be sent to the captive portal. Never derived from a name preference.
     * A missing/decryption-failed account must never fall back to personal data.
     */
    public static String campusAccount(Context context) {
        try {
            org.json.JSONObject saved = new org.json.JSONObject(Vault.load(context));
            String raw = saved.optString("username", "").trim();
            if (raw.isEmpty()) return "校园网账号未配置";
            String carrier = context.getSharedPreferences("auth", 0)
                    .getString("carrier", CarrierHelper.CARRIER_CMCC);
            String account = CarrierHelper.formatUsername(raw, carrier);
            return account.trim().isEmpty() ? "校园网账号未配置" : account;
        } catch (Exception ignored) {
            return "校园网账号未配置";
        }
    }

    static void clearLegacyNamePreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, 0);
        if (prefs.contains("watermark_name"))
            prefs.edit().remove("watermark_name").apply();
    }
    public static boolean ambient(Context c) {
        return c.getSharedPreferences(PREFS, 0).getBoolean(AMBIENT, true);
    }
    static int readSystemColor(Context c, String name, int fallback) {
        int id = c.getResources().getIdentifier(name, "color", "android");
        if (id == 0) return fallback;
        try { return c.getResources().getColor(id, c.getTheme()); }
        catch (RuntimeException ex) { return fallback; }
    }

    static OneUiPalette.PaletteColors dynamicPalette(Context c,
            OneUiPalette.PaletteColors base) {
        boolean dark = base.isDark;
        String suffix = dark ? "_200" : "_600";
        int primary=readSystemColor(c,"system_accent1"+suffix,base.primary);
        int onPrimary=dark
                ? readSystemColor(c,"system_accent1_800",base.onPrimary)
                : Color.WHITE;
        if (OneUiPalette.calculateContrastRatio(onPrimary,primary)<4.5)
            onPrimary=OneUiPalette.calculateContrastRatio(Color.WHITE,primary)
                    >= OneUiPalette.calculateContrastRatio(Color.BLACK,primary)
                    ? Color.WHITE:Color.BLACK;
        return new OneUiPalette.PaletteColors(
            readSystemColor(c,dark?"system_neutral1_900":"system_neutral1_10",base.background),
            readSystemColor(c,dark?"system_neutral1_800":"system_neutral1_10",base.surface),
            readSystemColor(c,dark?"system_neutral1_800":"system_neutral1_50",base.surfaceContainer),
            readSystemColor(c,dark?"system_neutral1_700":"system_neutral1_100",base.surfaceContainerHigh),
            readSystemColor(c,"system_neutral2_500",base.outline),
            readSystemColor(c,dark?"system_neutral2_700":"system_neutral2_200",base.outlineVariant),
            readSystemColor(c,dark?"system_neutral1_100":"system_neutral1_900",base.textPrimary),
            readSystemColor(c,dark?"system_neutral2_200":"system_neutral2_700",base.textSecondary),
            primary,onPrimary,
            readSystemColor(c,dark?"system_accent1_700":"system_accent1_100",base.primaryContainer),
            readSystemColor(c,dark?"system_accent1_100":"system_accent1_900",base.onPrimaryContainer),
            readSystemColor(c,dark?"system_accent2_700":"system_accent2_100",base.secondaryContainer),
            readSystemColor(c,dark?"system_accent2_100":"system_accent2_900",base.onSecondaryContainer),
            readSystemColor(c,dark?"system_accent3_700":"system_accent3_100",base.tertiaryContainer),
            readSystemColor(c,dark?"system_accent3_100":"system_accent3_900",base.onTertiaryContainer),
            base.success,base.warning,base.error,base.neutralDot,base.isDark);
    }

    public static OneUiPalette.PaletteColors palette(Context c) {
        boolean sysDark = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int seed = OneUiPalette.parseColor(seedHex(c), 0xFF2189FF);
        OneUiPalette.PaletteColors base = OneUiPalette.resolvePalette(mode(c), seed, sysDark);
        // Like WoAiQianDao: AMOLED ignores dynamic colors, all other modes
        // pick platform Material dynamic tonal roles on Android 12+.
        return useDynamic(c) && !OneUiPalette.MODE_AMOLED.equals(mode(c))
                ? dynamicPalette(c,base) : base;
    }

    private static int dp(Context c, float v) { return (int)(v * c.getResources().getDisplayMetrics().density + 0.5f); }
    private static Context dialogContext(Activity a) {
        boolean dark = palette(a).isDark;
        return new android.view.ContextThemeWrapper(a,
            dark ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert);
    }
    public static void show(Activity activity, Runnable changed) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, 0);
        String[] options = new String[] {
            "显示模式   ·   " + currentModeLabel(activity),
            "主题颜色   ·   " + (useDynamic(activity) ? "系统动态取色" : seedHex(activity)),
            "系统动态取色   ·   " + (useDynamic(activity) ? "开启" : "关闭"),
            "动态极光   ·   " + (ambient(activity) ? "开启" : "关闭")
        };
        new AlertDialog.Builder(dialogContext(activity))
            .setTitle("外观")
            .setItems(options, (dialog, selected) -> {
                if (selected == 0) showMode(activity, changed);
                else if (selected == 1) showColors(activity, changed);
                else if (selected == 2) {
                    if (Build.VERSION.SDK_INT < 31) {
                        Toast.makeText(activity, "系统动态取色需要 Android 12 或更高版本", Toast.LENGTH_LONG).show();
                    } else {
                        prefs.edit().putBoolean(DYNAMIC, !useDynamic(activity)).apply();
                        changed.run();
                    }
                } else if (selected == 3) {
                    prefs.edit().putBoolean(AMBIENT, !ambient(activity)).apply();
                    changed.run();
                }
            })
            .setNegativeButton("关闭", null).show();
    }
    private static String currentModeLabel(Context c) {
        String m = mode(c);
        for (int i = 0; i < OneUiPalette.THEME_MODES.length; i++) {
            if (OneUiPalette.THEME_MODES[i].equals(m)) return OneUiPalette.THEME_MODE_LABELS[i];
        }
        return "跟随系统";
    }
    private static void showMode(Activity activity, Runnable changed) {
        String cur = mode(activity);
        int idx = 0;
        for (int i=0;i<OneUiPalette.THEME_MODES.length;i++)
            if (OneUiPalette.THEME_MODES[i].equals(cur)) idx=i;
        new AlertDialog.Builder(dialogContext(activity))
            .setTitle("显示模式")
            .setSingleChoiceItems(OneUiPalette.THEME_MODE_LABELS, idx, (dialog, which)-> {
                activity.getSharedPreferences(PREFS,0).edit().putString(MODE,OneUiPalette.THEME_MODES[which]).apply();
                dialog.dismiss();
                changed.run();
            })
            .setNegativeButton("取消",null).show();
    }
    private static void showColors(Activity activity, Runnable changed) {
        OneUiPalette.PaletteColors p = palette(activity);
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(activity,16),dp(activity,12),dp(activity,16),dp(activity,8));

        TextView heading = new TextView(activity);
        heading.setText("选择强调色");
        heading.setTextSize(14);
        heading.setTextColor(p.textPrimary);
        heading.setPadding(dp(activity,5),0,0,dp(activity,12));
        layout.addView(heading);

        GridLayout grid = new GridLayout(activity);
        grid.setColumnCount(4);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);
        String current = seedHex(activity);
        final AlertDialog[] holder = new AlertDialog[1];
        for (OneUiPalette.Swatch swatch : OneUiPalette.PRESET_SWATCHES) {
            LinearLayout item = new LinearLayout(activity);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setMinimumHeight(dp(activity,80));
            GridLayout.LayoutParams gridLp = new GridLayout.LayoutParams();
            gridLp.width = 0;
            gridLp.height = dp(activity,88);
            gridLp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED,1f);
            item.setLayoutParams(gridLp);

            TextView dot = new TextView(activity);
            dot.setGravity(Gravity.CENTER);
            boolean selected = current.equalsIgnoreCase(swatch.hex) && !useDynamic(activity);
            dot.setText(selected ? "✓" : "");
            dot.setTextColor(OneUiPalette.calculateContrastRatio(Color.WHITE,swatch.color)>=4.5 ? Color.WHITE : Color.BLACK);
            dot.setTextSize(20);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(swatch.color);
            circle.setStroke(dp(activity,selected ? 3 : 1), selected ? p.textPrimary : p.outlineVariant);
            dot.setBackground(circle);
            item.addView(dot,new LinearLayout.LayoutParams(dp(activity,43),dp(activity,43)));

            TextView name = new TextView(activity);
            name.setText(swatch.name);
            name.setTextColor(p.textSecondary);
            name.setTextSize(11);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            item.addView(name,new LinearLayout.LayoutParams(-1,dp(activity,30)));
            item.setContentDescription(swatch.name + (selected ? "，已选择" : ""));
            item.setOnClickListener(v -> {
                activity.getSharedPreferences(PREFS,0).edit()
                    .putString(HEX,swatch.hex).putBoolean(DYNAMIC,false).apply();
                if (holder[0]!=null) holder[0].dismiss();
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                changed.run();
            });
            grid.addView(item);
        }
        layout.addView(grid);
        TextView custom = new TextView(activity);
        custom.setText("使用自定义 HEX 颜色");
        custom.setTextSize(14);
        custom.setTextColor(p.primary);
        custom.setGravity(Gravity.CENTER);
        custom.setMinHeight(dp(activity,48));
        custom.setOnClickListener(v -> {
            if (holder[0]!=null) holder[0].dismiss();
            showCustom(activity,changed);
        });
        layout.addView(custom);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(layout);
        holder[0]=new AlertDialog.Builder(dialogContext(activity))
            .setTitle("主题颜色").setView(scroll).setNegativeButton("取消",null).create();
        holder[0].show();
    }
    private static void showCustom(Activity activity, Runnable changed) {
        EditText edit = new EditText(activity);
        edit.setSingleLine(true);
        edit.setText(seedHex(activity));
        edit.setSelectAllOnFocus(true);
        edit.setHint("#2189FF");
        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setPadding(dp(activity,20),dp(activity,4),dp(activity,20),dp(activity,2));
        wrapper.addView(edit,new LinearLayout.LayoutParams(-1,dp(activity,56)));
        AlertDialog dialog = new AlertDialog.Builder(dialogContext(activity))
            .setTitle("自定义强调色").setView(wrapper)
            .setNegativeButton("取消",null)
            .setPositiveButton("应用",null).create();
        dialog.setOnShowListener(ignored-> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String value=edit.getText().toString().trim();
            if (!value.startsWith("#")) value="#" + value;
            if (value.length()!=7 || !OneUiPalette.isValidHex(value)) {
                edit.setError("请输入 6 位 HEX 颜色，例如 #2189FF");
                return;
            }
            activity.getSharedPreferences(PREFS,0).edit()
                .putString(HEX,value.toUpperCase(java.util.Locale.ROOT)).putBoolean(DYNAMIC,false).apply();
            dialog.dismiss();
            changed.run();
        }));
        dialog.show();
    }
}
