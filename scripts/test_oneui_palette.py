# -*- coding: utf-8 -*-
"""Execute actual OneUiPalette.java without running Android or using the check-in project."""
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "android/src/local/njmu/autologin/OneUiPalette.java"
JAR = Path("D:/Android/Sdk/platforms/android-37.0/android.jar")
CODE = r'''
import local.njmu.autologin.OneUiPalette;
public class OneUiPaletteTest {
  static int cases = 0;
  static void ok(boolean condition, String label) {
    cases++;
    if (!condition) throw new AssertionError(label);
  }
  public static void main(String[] a) {
    ok(OneUiPalette.PRESET_SWATCHES.length == 8, "8 color presets");
    ok(OneUiPalette.THEME_MODES.length == 4, "4 appearance modes");
    ok("#2189FF".equals(OneUiPalette.DEFAULT_SEED_HEX), "default color");
    ok(OneUiPalette.isValidHex("#28A879"), "hex valid");
    ok(!OneUiPalette.isValidHex("#X28A879"), "hex invalid");
    ok(OneUiPalette.parseColor("#2189FF", 0) == 0xFF2189FF, "parse hex");
    int seed=0xFF2189FF;
    OneUiPalette.PaletteColors light=OneUiPalette.resolvePalette("LIGHT",seed,false);
    OneUiPalette.PaletteColors dark=OneUiPalette.resolvePalette("DARK",seed,false);
    OneUiPalette.PaletteColors oled=OneUiPalette.resolvePalette("AMOLED",seed,false);
    ok(light.background==0xFFF7F9FB,"light reference base");
    ok(dark.background==0xFF131418,"dark reference base");
    ok(oled.background==0xFF000000,"AMOLED true black");
    ok(oled.surface==0xFF101012,"AMOLED surface");
    ok(!light.isDark && dark.isDark && oled.isDark,"dark flags");
    ok(OneUiPalette.resolvePalette("SYSTEM",seed,false).background==light.background,"system light");
    ok(OneUiPalette.resolvePalette("SYSTEM",seed,true).background==dark.background,"system dark");
    for(OneUiPalette.Swatch swatch:OneUiPalette.PRESET_SWATCHES) {
      for(String mode:new String[]{"LIGHT","DARK","AMOLED"}) {
        OneUiPalette.PaletteColors p=OneUiPalette.resolvePalette(mode,swatch.color,false);
        ok(OneUiPalette.calculateContrastRatio(p.onPrimary,p.primary)>=4.5,
            "primary button contrast "+swatch.name+" "+mode);
      }
    }
    System.out.println("PASS: "+cases+" real Java One UI palette assertions");
  }
}
'''
with tempfile.TemporaryDirectory() as td:
    wd=Path(td)
    test=wd/"OneUiPaletteTest.java"
    test.write_text(CODE,encoding="utf-8")
    subprocess.run(["javac","-encoding","UTF-8","-cp",str(JAR),"-d",str(wd),str(SOURCE),str(test)],check=True)
    subprocess.run(["java","-cp",str(wd),"OneUiPaletteTest"],check=True)
