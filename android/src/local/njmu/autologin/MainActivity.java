package local.njmu.autologin;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.InputType;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    int cBg, cCard, cBorder, cTextPrimary, cTextMuted, cActionBlue, cActionBg, cGreen, cAmber, cRed, cGray;
    float density;
    private OneUiPalette.PaletteColors palette;
    private OneUiBackdrop backdrop;
    private EditText userField, passwordField;
    private Spinner carrierField;
    private ScrollView scrollField;
    private boolean changingAppearance = false;

    GradientDrawable statusDotDrawable;
    TextView heroStatusText, heroSubText, heroStatePill;
    Button heroActionButton, pauseResumeButton;
    Switch quietModeSwitch;
    TextView modeDescription, permissionOverview, batteryCheck, scheduleCheck;
    TextView valIpv4, valLastSuccess, valLastResult, valServiceState;
    LinearLayout logContainer;
    TextView emptyLogText;

    Handler handler = new Handler();
    Runnable refreshRunnable = new Runnable() {
        public void run() {
            updateLiveStatus();
            handler.postDelayed(this, 2000);
        }
    };

    int dp(float v) { return (int) (v * density + 0.5f); }

    GradientDrawable makeCard(int bgColor, int strokeColor, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(bgColor);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), strokeColor);
        return d;
    }

    GradientDrawable makeSurface(int color, float radiusDp) {
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(color);
        surface.setCornerRadius(dp(radiusDp));
        return surface;
    }

    android.graphics.drawable.Drawable makeInteractive(int background, int border, float radius) {
        int ripple = palette.isDark ? 0x2AFFFFFF : 0x21000000;
        return new RippleDrawable(ColorStateList.valueOf(ripple),
                makeCard(background, border, radius), null);
    }

    void applyExcludeFromRecents(boolean hide) {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.AppTask> tasks = am.getAppTasks();
                if (tasks != null) {
                    for (ActivityManager.AppTask t : tasks) {
                        t.setExcludeFromRecents(hide);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OneUiAppearance.clearLegacyNamePreference(this);
        density = getResources().getDisplayMetrics().density;
        palette = OneUiAppearance.palette(this);
        cBg = palette.background;
        cCard = palette.surface;
        cBorder = palette.outlineVariant;
        cTextPrimary = palette.textPrimary;
        cTextMuted = palette.textSecondary;
        cActionBlue = palette.primary;
        cActionBg = palette.primaryContainer;
        cGreen = palette.success;
        cAmber = palette.warning;
        cRed = palette.error;
        cGray = palette.neutralDot;
        getWindow().setStatusBarColor(cBg);
        getWindow().setNavigationBarColor(cBg);
        int barFlags = palette.isDark ? 0 :
            (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().getDecorView().setSystemUiVisibility(barFlags);

        ScrollView scrollView = new ScrollView(this);
        scrollField = scrollView;
        scrollView.setBackgroundColor(Color.TRANSPARENT);
        scrollView.setFillViewport(true);

        scrollView.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER_HORIZONTAL);

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int windowWidthDp = getResources().getConfiguration().screenWidthDp;
        int maxW = dp(windowWidthDp >= 840 ? 1120 : (windowWidthDp >= 600 ? 720 : 620));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(40));
        if (screenWidth > maxW) {
            LinearLayout.LayoutParams rootLp = new LinearLayout.LayoutParams(maxW, -2);
            rootLp.gravity = Gravity.CENTER_HORIZONTAL;
            root.setLayoutParams(rootLp);
        } else {
            root.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        }
        outer.addView(root);
        scrollView.addView(outer);
        FrameLayout scene = new FrameLayout(this);
        scene.setBackgroundColor(cBg);
        backdrop = new OneUiBackdrop(this);
        backdrop.setPalette(palette, OneUiPalette.MODE_AMOLED.equals(OneUiAppearance.mode(this)));
        backdrop.setWatermarkText(OneUiAppearance.campusAccount(this));
        backdrop.setAuroraEnabled(OneUiAppearance.ambient(this));
        // Always draw the required campus-account watermark, even if the
        // optional animated aurora background has been switched off.
        scene.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));
        scene.addView(scrollView, new FrameLayout.LayoutParams(-1, -1));
        setContentView(scene);

        // 1. Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(18));

        TextView logo = new TextView(this);
        logo.setText("NJMU");
        logo.setTextSize(13);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setTextColor(0xFFFFFFFF);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setColor(cActionBlue);
        logoBg.setCornerRadius(dp(10));
        logo.setBackground(logoBg);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        logoLp.setMargins(0, 0, dp(12), 0);
        logo.setLayoutParams(logoLp);
        logo.setVisibility(View.GONE);
        header.addView(logo);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView titleText = new TextView(this);
        titleText.setText("我爱校园网");
        titleText.setTextSize(30);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setTextColor(cTextPrimary);
        titleBox.addView(titleText);

        TextView subTitleText = new TextView(this);
        subTitleText.setText("自动认证 · 南京医科大学");
        subTitleText.setTextSize(12);
        subTitleText.setTextColor(cTextMuted);
        titleBox.addView(subTitleText);
        header.addView(titleBox);

        TextView badge = new TextView(this);
        badge.setText("v1.8.6");
        badge.setTextSize(11);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(cActionBlue);
        badge.setPadding(dp(8), dp(4), dp(8), dp(4));
        badge.setBackground(makeCard(cActionBg, cActionBlue, 12));
        header.addView(badge);
        ImageButton appearanceButton = new ImageButton(this);
        appearanceButton.setImageResource(android.R.drawable.ic_menu_manage);
        appearanceButton.setColorFilter(cTextPrimary);
        appearanceButton.setContentDescription("外观设置");
        appearanceButton.setBackground(makeInteractive(cCard, cBorder, 16));
        appearanceButton.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams appearanceLp = new LinearLayout.LayoutParams(dp(48), dp(48));
        appearanceLp.setMargins(dp(10), 0, 0, 0);
        appearanceButton.setLayoutParams(appearanceLp);
        appearanceButton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            OneUiAppearance.show(this, this::applyAppearanceChange);
        });
        header.addView(appearanceButton);
        root.addView(header);

        // 2. Hero Card
        LinearLayout heroCard = new LinearLayout(this);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setBackground(makeSurface(cCard, 22));
        heroCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, -2);
        heroLp.setMargins(0, 0, 0, dp(16));
        heroCard.setLayoutParams(heroLp);

        LinearLayout heroTop = new LinearLayout(this);
        heroTop.setOrientation(LinearLayout.HORIZONTAL);
        heroTop.setGravity(Gravity.CENTER_VERTICAL);

        TextView heroEyebrow = new TextView(this);
        heroEyebrow.setText("连接状态");
        heroEyebrow.setTextSize(12);
        heroEyebrow.setTypeface(Typeface.DEFAULT_BOLD);
        heroEyebrow.setTextColor(cTextMuted);
        heroEyebrow.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        heroTop.addView(heroEyebrow);

        heroStatePill = new TextView(this);
        heroStatePill.setText("监控中");
        heroStatePill.setTextSize(11);
        heroStatePill.setPadding(dp(8), dp(3), dp(8), dp(3));
        heroStatePill.setBackground(makeCard(cActionBg, cActionBlue, 10));
        heroStatePill.setTextColor(cActionBlue);
        heroTop.addView(heroStatePill);
        heroCard.addView(heroTop);

        LinearLayout heroStatusRow = new LinearLayout(this);
        heroStatusRow.setOrientation(LinearLayout.HORIZONTAL);
        heroStatusRow.setGravity(Gravity.CENTER_VERTICAL);
        heroStatusRow.setPadding(0, dp(12), 0, dp(6));

        View dot = new View(this);
        statusDotDrawable = new GradientDrawable();
        statusDotDrawable.setShape(GradientDrawable.OVAL);
        statusDotDrawable.setColor(cGray);
        dot.setBackground(statusDotDrawable);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(12), dp(12));
        dotLp.setMargins(0, 0, dp(10), 0);
        dot.setLayoutParams(dotLp);
        heroStatusRow.addView(dot);

        heroStatusText = new TextView(this);
        heroStatusText.setText("正在检测网络...");
        heroStatusText.setTextSize(27);
        heroStatusText.setTypeface(Typeface.DEFAULT_BOLD);
        heroStatusText.setTextColor(cTextPrimary);
        heroStatusRow.addView(heroStatusText);
        heroCard.addView(heroStatusRow);

        heroSubText = new TextView(this);
        heroSubText.setText("自动识别校园网连接状态");
        heroSubText.setTextSize(13);
        heroSubText.setTextColor(cTextMuted);
        heroSubText.setPadding(0, 0, 0, dp(16));
        heroCard.addView(heroSubText);

        heroActionButton = new Button(this);
        heroActionButton.setText("立即检查并认证");
        heroActionButton.setTextSize(15);
        heroActionButton.setTextColor(palette.onPrimary);
        heroActionButton.setTypeface(Typeface.DEFAULT_BOLD);
        heroActionButton.setBackground(makeInteractive(cActionBlue, cActionBlue, 16));
        heroActionButton.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(54)));
        heroActionButton.setOnClickListener(v -> {
            boolean enabled = getSharedPreferences("auth", 0).getBoolean("enabled", false);
            if (!enabled) {
                try {
                    JSONObject existing = new JSONObject(Vault.load(this));
                    if (existing.optString("username", "").isEmpty() ||
                            existing.optString("password", "").isEmpty()) {
                        Toast.makeText(this, "请先保存校园网账号和密码", Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (Exception noCredentials) {
                    Toast.makeText(this, "请先在下方保存校园网账号和密码", Toast.LENGTH_LONG).show();
                    return;
                }
                getSharedPreferences("auth", 0).edit()
                    .putBoolean("enabled", true).putLong("attempt", 0).apply();
                if (!activateAccordingToMode()) { updateLiveStatus(); return; }
                AuthLog.log(this, AuthLog.TYPE_INFO, "用户启用自动认证");
                Toast.makeText(this, "自动认证已启用", Toast.LENGTH_SHORT).show();
            } else {
                getSharedPreferences("auth", 0).edit().putLong("attempt", 0).apply();
                if (!activateAccordingToMode()) { updateLiveStatus(); return; }
                AuthLog.log(this, AuthLog.TYPE_INFO, "用户手动检查网络");
                Toast.makeText(this, AuthScheduler.isQuietMode(this)
                    ? "已发起一次检查；后台周期任务由系统调度"
                    : "已发起即时检查", Toast.LENGTH_LONG).show();
            }
            updateLiveStatus();
        });
        heroCard.addView(heroActionButton);
        root.addView(heroCard);

        // 3. Configuration Card
        LinearLayout configCard = new LinearLayout(this);
        configCard.setOrientation(LinearLayout.VERTICAL);
        configCard.setBackground(makeSurface(cCard, 22));
        configCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        LinearLayout.LayoutParams cfgLp = new LinearLayout.LayoutParams(-1, -2);
        cfgLp.setMargins(0, 0, 0, dp(16));
        configCard.setLayoutParams(cfgLp);

        LinearLayout cfgHeader = new LinearLayout(this);
        cfgHeader.setOrientation(LinearLayout.HORIZONTAL);
        cfgHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView cfgTitle = new TextView(this);
        cfgTitle.setText("认证配置");
        cfgTitle.setTextSize(17);
        cfgTitle.setTypeface(Typeface.DEFAULT_BOLD);
        cfgTitle.setTextColor(cTextPrimary);
        cfgTitle.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        cfgHeader.addView(cfgTitle);

        TextView cfgHint = new TextView(this);
        cfgHint.setText("本机加密保存");
        cfgHint.setTextSize(12);
        cfgHint.setTextColor(cGreen);
        cfgHeader.addView(cfgHint);
        configCard.addView(cfgHeader);

        TextView carrierLabel = new TextView(this);
        carrierLabel.setText("网络运营商");
        carrierLabel.setTextSize(13);
        carrierLabel.setTextColor(cTextMuted);
        carrierLabel.setPadding(0, dp(12), 0, dp(6));
        configCard.addView(carrierLabel);

        Spinner carrierSpinner = new Spinner(this);
        carrierField = carrierSpinner;
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CarrierHelper.CARRIER_LABELS);
        carrierSpinner.setAdapter(spinnerAdapter);
        carrierSpinner.setBackground(makeCard(cBg, cBorder, 10));
        carrierSpinner.setPadding(dp(12), dp(10), dp(12), dp(10));
        String curCarrier = getSharedPreferences("auth", 0).getString("carrier", CarrierHelper.CARRIER_CMCC);
        carrierSpinner.setSelection(CarrierHelper.getCarrierIndex(curCarrier));
        configCard.addView(carrierSpinner);

        TextView userLabel = new TextView(this);
        userLabel.setText("校园网账号");
        userLabel.setTextSize(13);
        userLabel.setTextColor(cTextMuted);
        userLabel.setPadding(0, dp(12), 0, dp(6));
        configCard.addView(userLabel);

        EditText userEdit = new EditText(this);
        userField = userEdit;
        userEdit.setHint("输入学号/工号 (无需输后缀)");
        userEdit.setSingleLine(true);
        userEdit.setTextSize(15);
        userEdit.setTextColor(cTextPrimary);
        userEdit.setHintTextColor(cTextMuted);
        userEdit.setBackground(makeCard(cBg, cBorder, 10));
        userEdit.setPadding(dp(14), dp(12), dp(14), dp(12));
        configCard.addView(userEdit);

        TextView passLabel = new TextView(this);
        passLabel.setText("校园网密码");
        passLabel.setTextSize(13);
        passLabel.setTextColor(cTextMuted);
        passLabel.setPadding(0, dp(12), 0, dp(6));
        configCard.addView(passLabel);

        LinearLayout passBox = new LinearLayout(this);
        passBox.setOrientation(LinearLayout.HORIZONTAL);
        passBox.setBackground(makeCard(cBg, cBorder, 10));
        passBox.setGravity(Gravity.CENTER_VERTICAL);

        EditText passEdit = new EditText(this);
        passwordField = passEdit;
        passEdit.setHint("输入校园网密码");
        passEdit.setSingleLine(true);
        passEdit.setTextSize(15);
        passEdit.setTextColor(cTextPrimary);
        passEdit.setHintTextColor(cTextMuted);
        passEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passEdit.setBackground(null);
        passEdit.setPadding(dp(14), dp(12), dp(14), dp(12));
        passEdit.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        passBox.addView(passEdit);

        TextView eyeBtn = new TextView(this);
        eyeBtn.setText("显示");
        eyeBtn.setTextSize(13);
        eyeBtn.setTextColor(cActionBlue);
        eyeBtn.setPadding(dp(12), dp(12), dp(14), dp(12));
        eyeBtn.setOnClickListener(v -> {
            if (passEdit.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                passEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                eyeBtn.setText("隐藏");
            } else {
                passEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                eyeBtn.setText("显示");
            }
            passEdit.setSelection(passEdit.getText().length());
        });
        passBox.addView(eyeBtn);
        configCard.addView(passBox);

        try {
            JSONObject v = new JSONObject(Vault.load(this));
            String rawU = v.optString("username", "");
            userEdit.setText(CarrierHelper.cleanUsername(rawU));
            passEdit.setText(v.optString("password", ""));
        } catch (Exception ignored) {}
        // Theme changes are view-only: never overwrite unsaved form edits from Vault.
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("pending_user")) {
                userEdit.setText(savedInstanceState.getString("pending_user", ""));
                passEdit.setText(savedInstanceState.getString("pending_pass", ""));
                carrierSpinner.setSelection(savedInstanceState.getInt("pending_carrier", 0));
                int restoredScroll = savedInstanceState.getInt("pending_scroll", 0);
                scrollView.post(() -> scrollView.scrollTo(0, restoredScroll));
            }
        }

        Button saveBtn = new Button(this);
        saveBtn.setText("保存配置并启用");
        saveBtn.setTextSize(15);
        saveBtn.setTextColor(palette.onPrimary);
        saveBtn.setTypeface(Typeface.DEFAULT_BOLD);
        saveBtn.setBackground(makeInteractive(cActionBlue, cActionBlue, 16));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, dp(54));
        saveLp.setMargins(0, dp(16), 0, 0);
        saveBtn.setLayoutParams(saveLp);

        saveBtn.setOnClickListener(v -> {
            String u = userEdit.getText().toString().trim();
            String p = passEdit.getText().toString();
            if (TextUtils.isEmpty(u) || TextUtils.isEmpty(p)) {
                Toast.makeText(this, "请填写完整的账号与密码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!CarrierHelper.isValidUsernameInput(u)) {
                userEdit.setError("账号只能输入学号，或附带 @cmcc / @telecom / @unicom 后缀");
                return;
            }
            try {
                String selCarrier = CarrierHelper.CARRIER_KEYS[carrierSpinner.getSelectedItemPosition()];
                String cleanUser = CarrierHelper.cleanUsername(u);
                JSONObject cred = new JSONObject();
                cred.put("username", cleanUser);
                cred.put("password", p);
                Vault.save(this, cred.toString());
                getSharedPreferences("auth", 0).edit()
                    .putString("carrier", selCarrier)
                    .putBoolean("enabled", true)
                    .putLong("attempt", 0)
                    .putLong("retryDelay", 15000)
                    .apply();
                if (backdrop != null)
                    backdrop.setWatermarkText(OneUiAppearance.campusAccount(this));
                if (!activateAccordingToMode()) { updateLiveStatus(); return; }
                Toast.makeText(this, "配置已保存，已启用自动认证", Toast.LENGTH_SHORT).show();
                AuthLog.log(this, "CONFIG", "已更新并保存凭据 · " + CarrierHelper.getCarrierLabel(selCarrier));
                updateLiveStatus();
            } catch (Exception e) {
                Toast.makeText(this, "保存失败：" + e.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
            }
        });
        configCard.addView(saveBtn);
        root.addView(configCard);

        // 4. Status Dashboard Card
        LinearLayout dashCard = new LinearLayout(this);
        dashCard.setOrientation(LinearLayout.VERTICAL);
        dashCard.setBackground(makeSurface(cCard, 22));
        dashCard.setPadding(dp(20), dp(18), dp(20), dp(18));
        LinearLayout.LayoutParams dashLp = new LinearLayout.LayoutParams(-1, -2);
        dashLp.setMargins(0, 0, 0, dp(16));
        dashCard.setLayoutParams(dashLp);

        TextView dashTitle = new TextView(this);
        dashTitle.setText("运行看板");
        dashTitle.setTextSize(17);
        dashTitle.setTypeface(Typeface.DEFAULT_BOLD);
        dashTitle.setTextColor(cTextPrimary);
        dashTitle.setPadding(0, 0, 0, dp(12));
        dashCard.addView(dashTitle);

        valIpv4 = addDashboardRow(dashCard, "Wi-Fi IPv4", "未获取");
        valLastSuccess = addDashboardRow(dashCard, "最近认证成功", "暂无");
        valLastResult = addDashboardRow(dashCard, "接口响应", "暂无");
        valServiceState = addDashboardRow(dashCard, "后台服务", "检测中");
        root.addView(dashCard);

        // 5. Surfboard 同款防误杀 & 系统保活 Card
        LinearLayout keepAliveCard = new LinearLayout(this);
        keepAliveCard.setOrientation(LinearLayout.VERTICAL);
        keepAliveCard.setBackground(makeSurface(cCard, 22));
        keepAliveCard.setPadding(dp(20), dp(18), dp(20), dp(18));
        LinearLayout.LayoutParams keepLp = new LinearLayout.LayoutParams(-1, -2);
        keepLp.setMargins(0, 0, 0, dp(16));
        keepAliveCard.setLayoutParams(keepLp);

        TextView keepTitle = new TextView(this);
        keepTitle.setText("运行设置");
        keepTitle.setTextSize(17);
        keepTitle.setTypeface(Typeface.DEFAULT_BOLD);
        keepTitle.setTextColor(cTextPrimary);
        keepAliveCard.addView(keepTitle);

        TextView modeHeading = new TextView(this);
        modeHeading.setText("运行方式");
        modeHeading.setTextSize(15);
        modeHeading.setTextColor(cTextPrimary);
        modeHeading.setTypeface(Typeface.DEFAULT_BOLD);
        modeHeading.setPadding(0, dp(12), 0, dp(5));
        keepAliveCard.addView(modeHeading);

        quietModeSwitch = new Switch(this);
        quietModeSwitch.setText("隐藏后台");
        quietModeSwitch.setTextSize(14);
        quietModeSwitch.setTextColor(cTextPrimary);
        quietModeSwitch.setPadding(0, dp(6), 0, dp(6));
        quietModeSwitch.setMinHeight(dp(48));
        quietModeSwitch.setChecked(AuthScheduler.isQuietMode(this));
        // A single hide switch controls the actual service mode AND recents policy.
        applyExcludeFromRecents(AuthScheduler.isQuietMode(this));
        keepAliveCard.addView(quietModeSwitch);

        modeDescription = new TextView(this);
        modeDescription.setTextSize(12);
        modeDescription.setTextColor(cTextMuted);
        modeDescription.setPadding(0, 0, 0, dp(14));
        keepAliveCard.addView(modeDescription);
        quietModeSwitch.setOnCheckedChangeListener((b, checked) -> {
            AuthScheduler.changeMode(this, checked);
            getSharedPreferences("auth", 0).edit().putBoolean("hideRecents", checked).apply();
            applyExcludeFromRecents(checked);
            if (!checked && AuthScheduler.isEnabled(this)) {
                startAuthServiceSafely();
            }
            AuthLog.log(this, AuthLog.TYPE_INFO,
                    checked ? "已开启隐藏后台" : "已切换至持续监控");
            updateLiveStatus();
        });

        TextView guideHeading = new TextView(this);
        guideHeading.setText("运行保障");
        guideHeading.setTextSize(15);
        guideHeading.setTextColor(cTextPrimary);
        guideHeading.setTypeface(Typeface.DEFAULT_BOLD);
        guideHeading.setPadding(0, dp(8), 0, dp(6));
        keepAliveCard.addView(guideHeading);
        permissionOverview = addRequirementRow(keepAliveCard, "账号与自动认证", "必选", "检查中", true);
        scheduleCheck = addRequirementRow(keepAliveCard, "运行状态", "", "检查中", false);
        batteryCheck = addRequirementRow(keepAliveCard, "电池不受限制  ›", "建议", "检查中", false);
        // Tap the whole row, not a tiny secondary button. OEM settings differ,
        // so battery setup uses stock Android intents with safe fallbacks.
        batteryCheck.setOnClickListener(v -> openBatterySettings());
        View batteryRow = (View) batteryCheck.getParent();
        batteryRow.setOnClickListener(v -> openBatterySettings());
        batteryRow.setContentDescription("电池不受限制，点击打开电池设置");
        batteryRow.setMinimumHeight(dp(56));
        batteryRow.setBackground(makeInteractive(cCard, cBorder, 14));
        batteryRow.setPadding(dp(12), dp(9), dp(12), dp(9));
        View credentialRow = (View) permissionOverview.getParent();
        credentialRow.setMinimumHeight(dp(56));
        credentialRow.setBackground(makeInteractive(cCard, cBorder, 14));
        credentialRow.setPadding(dp(12), dp(9), dp(12), dp(9));
        credentialRow.setContentDescription("账号与自动认证，必选，点击跳转账号配置");
        credentialRow.setOnClickListener(v -> {
            scrollView.post(() -> {
                int[] viewPos = new int[2], scrollPos = new int[2];
                configCard.getLocationOnScreen(viewPos);
                scrollView.getLocationOnScreen(scrollPos);
                scrollView.smoothScrollTo(0, scrollView.getScrollY() + viewPos[1] - scrollPos[1] - dp(20));
            });
        });
        View scheduleRow = (View) scheduleCheck.getParent();
        scheduleRow.setMinimumHeight(dp(56));
        scheduleRow.setBackground(makeInteractive(cCard, cBorder, 14));
        scheduleRow.setPadding(dp(12), dp(9), dp(12), dp(9));
        scheduleRow.setContentDescription("运行状态，点击检查或重新安排后台任务");
        scheduleRow.setOnClickListener(v -> {
            if (!AuthScheduler.isEnabled(this)) {
                Toast.makeText(this, "请先保存账号并启用认证", Toast.LENGTH_SHORT).show();
                return;
            }
            if (AuthScheduler.isQuietMode(this)) AuthScheduler.trigger(this);
            else startAuthServiceSafely();
            updateLiveStatus();
            Toast.makeText(this, "已请求更新运行状态", Toast.LENGTH_SHORT).show();
        });

        pauseResumeButton = new Button(this);
        pauseResumeButton.setTextSize(14);
        pauseResumeButton.setTextColor(cTextPrimary);
        pauseResumeButton.setBackground(makeInteractive(cBg, cBorder, 14));
        LinearLayout.LayoutParams pauseLp = new LinearLayout.LayoutParams(-1, dp(48));
        pauseLp.setMargins(0, dp(12), 0, dp(8));
        pauseResumeButton.setLayoutParams(pauseLp);
        pauseResumeButton.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("auth", 0);
            if (prefs.getBoolean("enabled", false)) {
                prefs.edit().putBoolean("enabled", false).apply();
                AuthScheduler.pause(this);
                AuthLog.log(this, AuthLog.TYPE_INFO, "用户手动暂停自动认证");
                Toast.makeText(this, "已暂停自动认证", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    JSONObject creds = new JSONObject(Vault.load(this));
                    if (TextUtils.isEmpty(creds.optString("username", ""))
                            || TextUtils.isEmpty(creds.optString("password", ""))) {
                        Toast.makeText(this, "请先保存完整账号和密码", Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "请先保存校园网账号和密码", Toast.LENGTH_LONG).show();
                    return;
                }
                prefs.edit().putBoolean("enabled", true).putLong("attempt", 0L).apply();
                if (!activateAccordingToMode()) { updateLiveStatus(); return; }
                AuthLog.log(this, AuthLog.TYPE_INFO, "用户恢复自动认证");
                Toast.makeText(this, "自动认证已恢复", Toast.LENGTH_SHORT).show();
            }
            updateLiveStatus();
        });
        keepAliveCard.addView(pauseResumeButton);


        root.addView(keepAliveCard);

        // 6. History Logs Card
        LinearLayout logCard = new LinearLayout(this);
        logCard.setOrientation(LinearLayout.VERTICAL);
        logCard.setBackground(makeSurface(cCard, 22));
        logCard.setPadding(dp(20), dp(18), dp(20), dp(18));
        LinearLayout.LayoutParams logLp = new LinearLayout.LayoutParams(-1, -2);
        logLp.setMargins(0, 0, 0, dp(20));
        logCard.setLayoutParams(logLp);

        LinearLayout logHeader = new LinearLayout(this);
        logHeader.setOrientation(LinearLayout.HORIZONTAL);
        logHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView logTitle = new TextView(this);
        logTitle.setText("认证记录");
        logTitle.setTextSize(17);
        logTitle.setTypeface(Typeface.DEFAULT_BOLD);
        logTitle.setTextColor(cTextPrimary);
        logTitle.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        logHeader.addView(logTitle);

        Button clearLogBtn = new Button(this);
        clearLogBtn.setText("清空");
        clearLogBtn.setTextSize(12);
        clearLogBtn.setTextColor(cTextMuted);
        clearLogBtn.setBackground(null);
        clearLogBtn.setPadding(dp(8), 0, dp(8), 0);
        clearLogBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("清空记录")
                .setMessage("确定要清空历史运行日志吗？(不影响账号配置)")
                .setPositiveButton("清空", (d, w) -> {
                    AuthLog.clear(this);
                    renderLogs();
                })
                .setNegativeButton("取消", null)
                .show();
        });
        logHeader.addView(clearLogBtn);
        logCard.addView(logHeader);

        logContainer = new LinearLayout(this);
        logContainer.setOrientation(LinearLayout.VERTICAL);
        logContainer.setPadding(0, dp(8), 0, 0);
        logCard.addView(logContainer);

        emptyLogText = new TextView(this);
        emptyLogText.setText("暂无记录，连上校园网时将自动记录事件");
        emptyLogText.setTextSize(13);
        emptyLogText.setTextColor(cTextMuted);
        emptyLogText.setPadding(0, dp(8), 0, dp(8));
        logContainer.addView(emptyLogText);
        root.addView(logCard);

        // Screen hierarchy: immediate state, required settings, then detail;
        // first-time users get the credential form before advanced settings.
        boolean hasCredentials;
        try {
            JSONObject saved = new JSONObject(Vault.load(this));
            hasCredentials = !TextUtils.isEmpty(saved.optString("username", ""))
                    && !TextUtils.isEmpty(saved.optString("password", ""));
        } catch (Exception ignored) { hasCredentials = false; }
        if (windowWidthDp < 840 && hasCredentials) {
            root.removeView(keepAliveCard);
            root.removeView(dashCard);
            root.addView(keepAliveCard, 2);
            root.addView(dashCard, 3);
        }
        // One UI adaptive layout: two real content columns at >=840dp.
        if (windowWidthDp >= 840) {
            root.removeView(heroCard);
            root.removeView(configCard);
            root.removeView(dashCard);
            root.removeView(keepAliveCard);
            root.removeView(logCard);
            LinearLayout columns = new LinearLayout(this);
            columns.setOrientation(LinearLayout.HORIZONTAL);
            columns.setGravity(Gravity.TOP);
            columns.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
            LinearLayout right = new LinearLayout(this);
            right.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
            rightLp.setMargins(dp(20), 0, 0, 0);
            right.setLayoutParams(rightLp);
            left.addView(heroCard);
            left.addView(keepAliveCard);
            right.addView(configCard);
            right.addView(dashCard);
            right.addView(logCard);
            columns.addView(left);
            columns.addView(right);
            root.addView(columns);
        }

        // Reproduce WoAiQianDao's fixed One UI bottom action bar on compact phones.
        // The action exists only once: moved, not duplicated.
        if (windowWidthDp < 600) {
            heroCard.removeView(heroActionButton);
            LinearLayout dock = new LinearLayout(this);
            dock.setOrientation(LinearLayout.VERTICAL);
            dock.setBackground(makeSurface(cCard, 0));
            dock.setElevation(dp(8));
            dock.setPadding(dp(20), dp(12), dp(20), dp(12));
            heroActionButton.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(52)));
            dock.addView(heroActionButton);
            dock.setOnApplyWindowInsetsListener((v, insets) -> {
                v.setPadding(dp(20), dp(12), dp(20),
                    dp(12) + insets.getSystemWindowInsetBottom());
                return insets;
            });
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                -1, -2, Gravity.BOTTOM);
            scene.addView(dock, lp);
            root.setPadding(dp(20), dp(20), dp(20), dp(130));
        }

        if (getSharedPreferences("auth", 0).getBoolean("enabled", false) &&
            (savedInstanceState == null || !savedInstanceState.getBoolean("appearance_only", false))) {
            activateAccordingToMode();
        }
        updateLiveStatus();
        renderLogs();
    }

    TextView addRequirementRow(LinearLayout parent, String label, String mark,
                               String initialValue, boolean required) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, dp(9), 0, dp(9));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextSize(14);
        title.setTextColor(cTextPrimary);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        top.addView(title);
        if (!mark.isEmpty()) {
            TextView chip = new TextView(this);
            chip.setText(mark);
            chip.setTextSize(11);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setMinWidth(dp(46));
            chip.setMinHeight(dp(25));
            chip.setPadding(dp(8), dp(3), dp(8), dp(3));
            chip.setTextColor(required ? cActionBlue : cTextMuted);
            chip.setBackground(makeSurface(required ? cActionBg : cBg, 10));
            top.addView(chip);
        }
        block.addView(top);
        TextView status = new TextView(this);
        status.setText(initialValue);
        status.setTextSize(12);
        status.setTextColor(cTextMuted);
        status.setPadding(0, dp(4), 0, 0);
        block.addView(status);
        parent.addView(block);
        return status;
    }

    TextView addDashboardRow(LinearLayout parent, String label, String defaultVal) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(14);
        l.setTextColor(cTextMuted);
        l.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        row.addView(l);

        TextView v = new TextView(this);
        v.setText(defaultVal);
        v.setTextSize(14);
        v.setTextColor(cTextPrimary);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(v);

        parent.addView(row);
        return v;
    }

    /** Portable settings navigation: request exemption -> optimization list -> app info. */
    void openBatterySettings() {
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        boolean exempt=pm!=null && pm.isIgnoringBatteryOptimizations(getPackageName());
        ArrayList<Intent> choices = new ArrayList<>();
        if (!exempt) {
            choices.add(new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(android.net.Uri.parse("package:" + getPackageName())));
        }
        choices.add(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        choices.add(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.parse("package:" + getPackageName())));
        for (Intent intent : choices) {
            try {
                startActivity(intent);
                return;
            } catch (RuntimeException ignored) {
                // Android manufacturers may block a setting activity.
            }
        }
        Toast.makeText(this, "系统未提供设置入口，请在应用信息中调整电池使用限制",
                Toast.LENGTH_LONG).show();
    }

    boolean activateAccordingToMode() {
        if (AuthScheduler.isQuietMode(this)) {
            AuthScheduler.trigger(this);
            // A user-visible screen can perform an immediate one-off check without an FGS.
            // After leaving the app, periodic checks remain subject to Android scheduling.
            new Thread(() -> new AuthEngine(getApplicationContext(), null, true).check(),
                    "NJMU-UserCheck").start();
            return AuthScheduler.isScheduled(this);
        }
        AuthScheduler.cancel(this);
        return startAuthServiceSafely();
    }

    void refreshRequirementStatus() {
        if (permissionOverview == null) return;
        boolean enabled=AuthScheduler.isEnabled(this);
        boolean quiet=AuthScheduler.isQuietMode(this);
        boolean credentialsAvailable=false;
        try {
            JSONObject cred=new JSONObject(Vault.load(this));
            credentialsAvailable=!TextUtils.isEmpty(cred.optString("username",""))
                    && !TextUtils.isEmpty(cred.optString("password",""));
        } catch(Exception ignored){}
        permissionOverview.setText(!credentialsAvailable?"需先保存账号密码"
                : enabled?"已启用":"已暂停（点击恢复）");
        if(scheduleCheck!=null){
            if(!enabled) scheduleCheck.setText("已暂停");
            else if(quiet) scheduleCheck.setText(AuthScheduler.isScheduled(this)
                    ? "系统调度已登记（不保证准时）":"未登记定时任务，尝试重新启用");
            else scheduleCheck.setText("可靠前台服务模式，状态见运行看板");
        }
        if(modeDescription!=null) modeDescription.setText(quiet
            ? "不常驻前台服务，通常不显示在运行应用列表；认证检查可能延迟。"
            : "持续监控更及时，系统可能显示后台活动。");
        if(batteryCheck!=null){
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            boolean allowed=pm!=null && pm.isIgnoringBatteryOptimizations(getPackageName());
            batteryCheck.setText(allowed ? "已豁免" : "未豁免（建议设置）");
        }

    }

    boolean startAuthServiceSafely() {
        try {
            Intent intent = new Intent(this, AuthService.class);
            intent.setAction("local.njmu.autologin.CHECK_NOW");
            startForegroundService(intent);
            return true;
        } catch (RuntimeException ex) {
            getSharedPreferences("auth", 0).edit().putBoolean("enabled", false).apply();
            AuthLog.log(this, AuthLog.TYPE_ERROR, "后台服务启动失败：" + ex.getClass().getSimpleName());
            Toast.makeText(this, "后台服务启动失败，请检查系统限制后重试", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    void updateLiveStatus() {
        SharedPreferences sp = getSharedPreferences("auth", 0);
        boolean enabled = sp.getBoolean("enabled", false);
        boolean quiet = AuthScheduler.isQuietMode(this);
        refreshRequirementStatus();
        long heartbeat = sp.getLong("serviceHeartbeatMs", 0L);
        long now = System.currentTimeMillis();
        boolean serviceHealthy = enabled && sp.getBoolean("serviceRunning", false)
                && heartbeat > 0L && heartbeat <= now && now - heartbeat <= 40000L;
        String stateCode = sp.getString("stateCode", enabled ? "CHECKING" : "DISABLED");
        String statusMsg = sp.getString("status", "等待检测");
        String ipv4 = sp.getString("wifiIpv4", "未获取");
        String lastSuccess = sp.getString("lastSuccess", "暂无");
        String lastResult = sp.getString("lastResult", "暂无");

        valIpv4.setText(ipv4);
        valLastSuccess.setText(lastSuccess);
        valLastResult.setText(lastResult);
        valServiceState.setText(!enabled ? "已暂停" : quiet
                ? (AuthScheduler.isScheduled(this) ? "系统已安排任务（非实时）" : "调度未成功")
                : serviceHealthy ? "前台监控正在运行" : heartbeat == 0L ? "等待后台启动" : "后台未响应");
        if (pauseResumeButton != null) {
            pauseResumeButton.setText(enabled ? "暂停自动认证" : "恢复自动认证");
        }

        if (!enabled) {
            heroStatusText.setText("自动认证已暂停");
            heroSubText.setText("点击下方按钮开启自动连接与静默认证");
            statusDotDrawable.setColor(cGray);
            heroStatePill.setText("已暂停");
            heroStatePill.setTextColor(cTextMuted);
            heroStatePill.setBackground(makeCard(cBg, cBorder, 10));
            heroActionButton.setText("启用自动认证");
        } else if (quiet) {
            heroActionButton.setText("立即检查（应用打开时）");
            heroStatusText.setText("隐藏后台已开启");
            heroSubText.setText("后台周期检查由系统安排，可能延迟");
            heroStatePill.setText(AuthScheduler.isScheduled(this) ? "已调度" : "待调度");
            heroStatePill.setTextColor(cAmber);
            heroStatePill.setBackground(makeCard(cBg, cBorder, 10));
            statusDotDrawable.setColor(cAmber);
        } else if (!serviceHealthy) {
            heroActionButton.setText("尝试重新启动");
            heroStatusText.setText(heartbeat == 0L ? "等待后台启动" : "后台未响应");
            heroSubText.setText("未检测到有效的后台服务心跳，可点击按钮尝试启动");
            statusDotDrawable.setColor(cAmber);
            heroStatePill.setText("待恢复");
            heroStatePill.setTextColor(cAmber);
            heroStatePill.setBackground(makeCard(cBg, cBorder, 10));
        } else {
            heroActionButton.setText("立即检查网络");
            heroStatePill.setText("监控中");
            heroStatePill.setTextColor(cActionBlue);
            heroStatePill.setBackground(makeCard(cActionBg, cActionBlue, 10));

            if ("ONLINE".equals(stateCode)) {
                heroStatusText.setText("已验证联网");
                heroSubText.setText("当前 Wi-Fi 已通过外网连通性测试（不代表校园网身份已核实）");
                statusDotDrawable.setColor(cGreen);
            } else if ("AUTHENTICATING".equals(stateCode)) {
                heroStatusText.setText("正在提交认证...");
                heroSubText.setText("正在向校园网 Aruba 网关提交账号密码");
                statusDotDrawable.setColor(cAmber);
            } else if ("RETRY".equals(stateCode)) {
                heroStatusText.setText("等待重试冷却");
                heroSubText.setText(statusMsg);
                statusDotDrawable.setColor(cAmber);
            } else if ("DENIED".equals(stateCode)) {
                heroStatusText.setText("学校拒绝认证");
                heroSubText.setText("网关返回 Access denied，请核对学号密码");
                statusDotDrawable.setColor(cRed);
            } else if ("NO_WIFI".equals(stateCode)) {
                heroStatusText.setText("等待 Wi-Fi");
                heroSubText.setText("未检测到 NJMU_WLAN 连接");
                statusDotDrawable.setColor(cGray);
            } else {
                heroStatusText.setText(statusMsg);
                heroSubText.setText("后台正在静默轮询与事件监听");
                statusDotDrawable.setColor(cAmber);
            }
        }
        renderLogs();
    }

    void renderLogs() {
        if (logContainer == null) return;
        List<AuthLog.Entry> logs = AuthLog.loadEntries(this);
        logContainer.removeAllViews();
        if (logs.isEmpty()) {
            logContainer.addView(emptyLogText);
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
        for (AuthLog.Entry item : logs) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(4), 0, dp(4));
            row.setGravity(Gravity.CENTER_VERTICAL);

            String type = item.type;
            int dotColor = cGray;
            if (AuthLog.TYPE_SUCCESS.equals(type)) dotColor = cGreen;
            else if ("AUTH".equals(type) || AuthLog.TYPE_WARN.equals(type)) dotColor = cAmber;
            else if (AuthLog.TYPE_ERROR.equals(type)) dotColor = cRed;
            else if ("CONFIG".equals(type)) dotColor = cActionBlue;

            View d = new View(this);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(dotColor);
            d.setBackground(gd);
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(dp(6), dp(6));
            dLp.setMargins(0, 0, dp(8), 0);
            d.setLayoutParams(dLp);
            row.addView(d);

            TextView time = new TextView(this);
            time.setText(sdf.format(new Date(item.timestamp)));
            time.setTextSize(12);
            time.setTextColor(cTextMuted);
            time.setPadding(0, 0, dp(8), 0);
            row.addView(time);

            TextView msg = new TextView(this);
            msg.setText(item.summary);
            msg.setTextSize(13);
            msg.setTextColor(cTextPrimary);
            msg.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
            row.addView(msg);

            logContainer.addView(row);
        }
    }

    private void applyAppearanceChange() {
        changingAppearance = true;
        recreate();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (userField != null && passwordField != null && carrierField != null) {
            outState.putString("pending_user", userField.getText().toString());
            outState.putString("pending_pass", passwordField.getText().toString());
            outState.putInt("pending_carrier", carrierField.getSelectedItemPosition());
            if (scrollField != null) outState.putInt("pending_scroll", scrollField.getScrollY());
            outState.putBoolean("appearance_only", changingAppearance);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (getSharedPreferences("auth", 0).getBoolean("hideRecents", false)) {
            finishAndRemoveTask();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Enforce one canonical mode state; older hidden-recents setting is migrated.
        applyExcludeFromRecents(AuthScheduler.isQuietMode(this));
        updateLiveStatus();
        if (backdrop != null) {
            backdrop.setWatermarkText(OneUiAppearance.campusAccount(this));
            backdrop.setActivityActive(true);
        }
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backdrop != null) backdrop.setActivityActive(false);
        handler.removeCallbacks(refreshRunnable);
    }
}
