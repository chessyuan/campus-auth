# -*- coding: utf-8 -*-
"""Read-only source hash + visual design contract for WoAiQianDao Android port.
Does not mutate the original Kotlin files. Static parity is not screenshot parity."""
from pathlib import Path
import hashlib,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
REF=Path(r"D:\Desktop\UI背景水印动态极光_复刻参考")
EXPECTED={
"components/InteriorUi.kt":"64D1774051B2BC1D2ACE6B3F5D10960E61608802A0468C2D44F1555A3A3BC4EF",
"ui/background/AppTechBackground.kt":"6CCB1E0DBD0C2127AEAD71ED0F9C93C6E4DB0A44C035F4B9258118D7FA605B0A",
"ui/design/AccessibleButtons.kt":"230FBA4E9AC6687ED25514ED04A8CE2DC7A75FE1273D271945001615EFD657AB",
"ui/design/DesignTokens.kt":"B5403956F369185C12F9511A310660F1C6679F660B359FA13302B9EFBCB6BF04",
"ui/design/TaskScaffold.kt":"6A50F772E165A022583CAE745360C8D3DA2D6C67C8E2744F35EA6D4270AD0E6B",
"ui/theme/Color.kt":"1529E2BE747A93C21C4FD90BD6ED0C3713456C1DAE7EC80761DFA905C17F2EC9",
"ui/theme/PresetColors.kt":"25D50853712AB0B3C13F34B4C0EF0D0FA9D35FBFC8C6681D8C434B1018E0D8FD",
"ui/theme/Theme.kt":"DD0A5558D22065FAABEBB4EBB4A170CC3A91EDD04573C48FCB276F3AA3A1CEA1",
"ui/theme/ThemeColorGenerator.kt":"AE092603F8E1E8E527BE4AF306B6385F160195CB40D67A36790D6700B3FFBA3C",
"ui/theme/ThemeManager.kt":"1426AB7A7816E9D061EF2A68A3500B418C8E3997732DA43F128AF00876952A72",
"ui/theme/ThemeMode.kt":"8F202C3D9D2DF83290466936F022C7792AD7F6F1B0D76E33754F2502262A7248",
"ui/theme/Type.kt":"C3C772E3B7DD3002AC38CA3B25F448F47544F45E1EF792E90767C797C64C502D"
}
def check(cond,label):
    global checks
    checks+=1
    if not cond: raise AssertionError(label)
checks=0
reference_files=sorted(p.relative_to(REF).as_posix() for p in REF.rglob("*.kt"))
check(reference_files==sorted(EXPECTED),"Unexpected added/removed Kotlin references")
for name,digest in EXPECTED.items():
    file=REF/name
    check(file.is_file() and hashlib.sha256(file.read_bytes()).hexdigest().upper()==digest,"Reference modified! "+name)
bg=(REF/"ui/background/AppTechBackground.kt").read_text(encoding="utf-8")
b=(ROOT/"android/src/local/njmu/autologin/OneUiBackdrop.java").read_text(encoding="utf-8")
u=(ROOT/"android/src/local/njmu/autologin/MainActivity.java").read_text(encoding="utf-8")
a=(ROOT/"android/src/local/njmu/autologin/OneUiAppearance.java").read_text(encoding="utf-8")
ps=(ROOT/"android/src/local/njmu/autologin/OneUiPalette.java").read_text(encoding="utf-8")
for token in ["durationMillis = 24000","durationMillis = 18000","drawCircle(","upperAurora = Path()","lowerAurora = Path()","angleDegrees: Float = -30f","fontSize = 12.sp","86.dp.toPx()","66.dp.toPx()","170.dp.toPx()","88.dp.toPx()"]:
    check(token in bg,"No matching original reference token "+token)
for token in ["timeSec%24.0","timeSec%18.0","radial(c,x1","radial(c,x2","radial(c,x3","Path upper","Path lower","new LinearGradient","Math.max(textWidth+86f*density,170f*density)","Math.max(textHeight+66f*density,88f*density)","canvas.rotate(-30f","ink.setTextSize(12f*scaled)","mixOklab","ACTION_POWER_SAVE_MODE_CHANGED","ANIMATOR_DURATION_SCALE","postInvalidateDelayed(33L)"]:
    check(token in b,"Missing native reference drawing pass "+token)
for token in [".78f+.18f", ".14f+.11f", ".15f+.16f", ".83f+.10f", ".30f+.07f", ".69f+.07f", "bandTeal=dark?.50f:.33f", "bandViolet=dark?.48f:.33f", "radius*.90f", "radius*.60f"]:
    check(token in b,"Motion geometry or alpha discrepancy "+token)
for token in ['OneUiAppearance.campusAccount(this)','badge.setText("v1.8.5")','"必选"','"建议"','windowWidthDp < 600','windowWidthDp >= 840','new LinearLayout.LayoutParams(-1, dp(52))','scene.addView(dock, lp)','setBackground(makeSurface(cCard, 22))']:
    check(token in u,"Responsive layout/required tag not wired "+token)
for token in ['OneUiPalette.MODE_AMOLED.equals(mode(c))','"system_accent1"','"system_accent2_200"','"动态极光   ·   "','clearLegacyNamePreference','"system_neutral1_900"']:
    check(token in a+b,"Four modes/dynamic material roles/watermark menu missing "+token)
for token in ['"跟随系统"','"浅色模式"','"深色模式"','"AMOLED 纯黑"','"#2189FF"','"#28A879"','"#8570E8"','"#EC789A"','"#E8A13B"','"#16A6B6"','"#687684"','"#DA626B"']:
    check(token in ps,"Reference preset or theme mode missing "+token)
manifest=ET.parse(ROOT/"android/AndroidManifest.xml").getroot()
check(manifest.attrib["{http://schemas.android.com/apk/res/android}versionName"]=="1.8.5","manifest version")
check(manifest.attrib["{http://schemas.android.com/apk/res/android}versionCode"]=="17","manifest versionCode")
print(f"PASS: {checks} read-only hash and UI reference contract checks; 12 source files unchanged")
