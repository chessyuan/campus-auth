# -*- coding: utf-8 -*-
"""Check read-only watermark and actionable, manufacturer-safe settings."""
from pathlib import Path
import xml.etree.ElementTree as E
R=Path(__file__).resolve().parents[1]/"android/src/local/njmu/autologin"
a=(R/"OneUiAppearance.java").read_text(encoding="utf-8")
u=(R/"MainActivity.java").read_text(encoding="utf-8")
b=(R/"OneUiBackdrop.java").read_text(encoding="utf-8")
tests=0
def check(ok,reason):
    global tests
    tests+=1
    if not ok:raise AssertionError(reason)
for disallowed in ("姓名水印","输入姓名","showWatermark(","watermarkName("):
    check(disallowed not in a and disallowed not in u and disallowed not in b,"Unexpected editable personal watermark: "+disallowed)
for token in ('new org.json.JSONObject(Vault.load(context))','CarrierHelper.formatUsername(raw, carrier)','clearLegacyNamePreference','校园网账号未配置'):
    check(token in a,"Campus-only watermark "+token)
check('watermark_name' in a and '.remove("watermark_name")' in a,"Legacy preference cleanup")
check('setWatermarkText(OneUiAppearance.campusAccount(this))' in u,"Account watermark not applied")
check('backdrop.setAuroraEnabled(OneUiAppearance.ambient(this));' in u,"Aurora toggle independent of watermark")
check('if (!auroraEnabled)' in b and 'drawWatermark(c, w, h);' in b,"Watermark must survive optional aurora off")
for absent in ("低可见度","可选","通知使用权","普通通知权限","btnPostNotice","btnNotif","hideSwitch","姓名水印"):
    check(absent not in u,"Unwanted optional or obsolete UI: "+absent)
for token in ('quietModeSwitch.setText("隐藏后台")','applyExcludeFromRecents(checked)','AuthScheduler.changeMode(this, checked)',
              'getSharedPreferences("auth", 0).edit().putBoolean("hideRecents", checked).apply()'):
    check(token in u,"Actual hide switch "+token)
for token in ('"账号与自动认证", "必选"','"电池不受限制  ›", "建议"','batteryRow.setOnClickListener','scheduleRow.setOnClickListener','credentialRow.setOnClickListener',
              'Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS','Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS',
              'Settings.ACTION_APPLICATION_DETAILS_SETTINGS','startActivity(intent)','catch (RuntimeException ignored)'):
    check(token in u,"Portable actionable requirement row "+token)
check('resolveActivity(getPackageManager())' not in u,"Avoid fragile OEM resolveActivity preflight")
check('startForegroundService(' not in (R/"AuthScheduler.java").read_text(encoding="utf-8"),"Hidden mode must not start FGS")
manifest=(ROOT:=Path(__file__).resolve().parents[1])/"android/AndroidManifest.xml"
mt=manifest.read_text(encoding="utf-8")
check("POST_NOTIFICATIONS" not in mt,"Optional notification display permission must not be declared")
check("BIND_NOTIFICATION_LISTENER_SERVICE" not in mt,"Optional notification listener must not be registered")
check(not (R/"CampusNotificationService.java").exists(),"Unused listener class should not be packaged")
print(f"PASS: {tests} v1.8.5 settings/watermark static contracts")
