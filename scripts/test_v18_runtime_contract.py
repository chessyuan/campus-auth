# -*- coding: utf-8 -*-
"""v1.7.1 static regression contract; NOT a substitute for device testing."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"android/src/local/njmu/autologin"
sources={p.name:p.read_text(encoding="utf-8") for p in SRC.glob("*.java")}
m=ET.parse(ROOT/"android/AndroidManifest.xml").getroot()
android="{http://schemas.android.com/apk/res/android}"
count=0
def check(cond,msg):
    global count
    if not cond:raise AssertionError(msg)
    count+=1
assert m.attrib[android+"versionName"]=="1.8.6"
check(m.attrib[android+"versionCode"]=="18","versionCode")
perms={x.attrib[android+"name"] for x in m.findall("uses-permission")}
for perm in ["android.permission.RECEIVE_BOOT_COMPLETED","android.permission.INTERNET","android.permission.FOREGROUND_SERVICE"]:
    check(perm in perms,"missing permission "+perm)
services={s.attrib[android+"name"]:s for s in m.find("application").findall("service")}
check(".AuthJobService" in services,"job service missing")
check(services[".AuthJobService"].attrib[android+"permission"]=="android.permission.BIND_JOB_SERVICE","job service binding")
check(services[".AuthService"].attrib[android+"exported"]=="false","fgs export")
scheduler=sources["AuthScheduler.java"]
for x in ["MODE_RELIABLE","MODE_QUIET","setPersisted(true)","setPeriodic(MIN_PERIOD_MS)","j.getPendingJob(JOB_PERIODIC)!=null","cancel(JOB_PERIODIC)","cancel(JOB_TRIGGER)","AuthScheduler"]:
    check(x in scheduler,"scheduler: "+x)
check("startForegroundService" not in scheduler,"quiet scheduler starts foreground service")
check("startForeground" not in sources["AuthJobService.java"],"quiet job uses foreground service")
check("new AuthEngine(" in sources["AuthJobService.java"],"job must call existing engine")
check("running.stopped.get()" in sources["AuthJobService.java"],"missing cancellation check")
check("shutdownNow()" in sources["AuthJobService.java"],"missing interruption")
check('AuthScheduler.isQuietMode(this)' in sources["AuthService.java"],"FGS missing quiet guard")
check("stopSelf();" in sources["AuthService.java"],"stopSelf absent")
check('"serviceHeartbeatMs"' in sources["AuthService.java"],"heartbeat absent")
check('AuthScheduler.trigger(c)' in sources["BootReceiver.java"],"boot quiet handling")
check('startForegroundService(new Intent(c,AuthService.class))' in sources["BootReceiver.java"],"reliable boot handling")
check("android.permission.POST_NOTIFICATIONS" not in perms,"optional display notifications permission removed")
check(".CampusNotificationService" not in services,"optional listener service removed")
engine=sources["AuthEngine.java"]
for x in ["CHECK_BUSY.compareAndSet(false, true)","requiredQuiet != AuthScheduler.isQuietMode(context)",'getBoolean("enabled", false)','c.setConnectTimeout(7000)','c.setReadTimeout(7000)','java.net.Proxy.NO_PROXY','"https://captiveportal-login.njmu.edu.cn/auth/index.html/u"']:
    check(x in engine,"engine: "+x)
ui=sources["MainActivity.java"]
for x in ['"隐藏后台"','"运行设置"','"电池不受限制  ›"','"账号与自动认证"','isIgnoringBatteryOptimizations','AuthScheduler.changeMode(this, checked)','AuthScheduler.pause(this)','AuthScheduler.isScheduled(this)','openBatterySettings()']:
    check(x in ui,"ui: "+x)
print(f"PASS: {count} v1.8.6 source/manifest contract checks (static only)")
