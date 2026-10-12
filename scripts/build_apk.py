# -*- coding: utf-8 -*-
"""
NJMU 校园网自动认证 - Android APK 轻量极速编译与打包脚本
直接基于本地 Android SDK（javac + d8 + aapt2 + zipalign + apksigner）构建，
无需笨重的 Gradle，数秒内即可完成极简 33KB 原生 APK 的编译与签名。
"""
import os
import sys
import shutil
import zipfile
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ANDROID_DIR = ROOT / "android"
BUILD_DIR = ROOT / "build"
RELEASE_DIR = ROOT / "release"

def find_android_sdk():
    candidates = [
        Path(r'D:\Android\Sdk'),
        Path(os.environ.get("ANDROID_HOME", "")),
        Path(os.environ.get("ANDROID_SDK_ROOT", "")),
        Path("D:/Android/Sdk"),
        Path(os.environ.get("LOCALAPPDATA", "")) / "Android" / "Sdk",
    ]
    for cand in candidates:
        if cand and cand.exists():
            return cand
    raise RuntimeError("未找到 Android SDK，请检查 %LOCALAPPDATA%\\Android\\Sdk 或设置 ANDROID_HOME")

def run_cmd(args, desc=""):
    cmd_str = " ".join(str(x) for x in args)
    print(f"[{desc}] 正在执行: {cmd_str[:120]}...")
    r = subprocess.run([str(x) for x in args], capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode != 0:
        print(f"❌ {desc} 失败 (退出码 {r.returncode}):")
        if r.stdout:
            print("STDOUT:", r.stdout[-1500:])
        if r.stderr:
            print("STDERR:", r.stderr[-1500:])
        sys.exit(1)
    print(f"✔ {desc} 完成")

def main():
    print("=== 开始构建 NJMU 校园网自动认证 Android APK ===")
    sdk = find_android_sdk()
    bt = sdk / "build-tools" / "37.0.0"
    if not bt.exists():
        bt_candidates = sorted((sdk / "build-tools").glob("*"), reverse=True)
        if bt_candidates:
            bt = bt_candidates[0]
        else:
            raise RuntimeError("未找到 Android build-tools")
    print(f"使用 Android SDK build-tools: {bt}")

    jar = sdk / "platforms" / "android-37.0" / "android.jar"
    if not jar.exists():
        jar_candidates = sorted((sdk / "platforms").glob("android-*/android.jar"), reverse=True)
        if jar_candidates:
            jar = jar_candidates[0]
        else:
            raise RuntimeError("未找到 android.jar platform SDK")
    print(f"使用 platform jar: {jar}")

    shutil.rmtree(BUILD_DIR, ignore_errors=True)
    classes_dir = BUILD_DIR / "classes"
    dex_dir = BUILD_DIR / "dex"
    classes_dir.mkdir(parents=True, exist_ok=True)
    dex_dir.mkdir(parents=True, exist_ok=True)
    RELEASE_DIR.mkdir(parents=True, exist_ok=True)

    java_files = list(ANDROID_DIR.rglob("*.java"))
    manifest = ANDROID_DIR / "AndroidManifest.xml"
    key = ANDROID_DIR / "keystore" / "signing.jks"
    pwfile = ANDROID_DIR / "keystore" / "signing-password.txt"

    # 1. 编译 Java 源码为 class
    run_cmd(["javac", "-encoding", "UTF-8", "-source", "8", "-target", "8", "-classpath", jar, "-d", classes_dir, *java_files], desc="Java 编译 (javac)")

    # 2. 将 class 转换为 Dalvik Executable (classes.dex)
    run_cmd([bt / "d8.bat", "--lib", jar, "--output", dex_dir, *classes_dir.rglob("*.class")], desc="生成 DEX (d8)")

    # 3. 链接资源与 Manifest 生成初始 base.apk
    base_apk = BUILD_DIR / "base.apk"
    run_cmd([bt / "aapt2.exe", "link", "-I", jar, "--manifest", manifest, "-o", base_apk], desc="资源打包 (aapt2)")

    # 4. 将 classes.dex 写入 APK
    with zipfile.ZipFile(base_apk, "a") as z:
        z.write(dex_dir / "classes.dex", "classes.dex")

    # 5. 对齐 APK (zipalign 4 字节对齐)
    out_apk = RELEASE_DIR / "NJMU-AutoAuth.apk"
    idsig_file = RELEASE_DIR / "NJMU-AutoAuth.apk.idsig"
    try:
        if out_apk.exists(): out_apk.unlink()
        if idsig_file.exists(): idsig_file.unlink()
    except Exception:
        pass
    aligned_apk = BUILD_DIR / "aligned.apk"
    run_cmd([bt / "zipalign.exe", "-f", "4", base_apk, aligned_apk], desc="APK 对齐 (zipalign)")

    # 6. 使用本地专用 JKS 签名 APK
    out_apk = RELEASE_DIR / "NJMU-AutoAuth.apk"
    run_cmd([bt / "apksigner.bat", "sign", "--ks", key, "--ks-pass", f"file:{pwfile}", "--out", out_apk, aligned_apk], desc="APK 签名 (apksigner)")
    run_cmd([bt / "apksigner.bat", "verify", out_apk], desc="验证 APK 签名")
    # Preserve all previous versioned APKs. Release name comes from the manifest.
    manifest_text = manifest.read_text(encoding="utf-8")
    match = re.search(r'android:versionName="([0-9]+(?:[.][0-9]+)*)"', manifest_text)
    if not match: raise RuntimeError("Missing or unsafe Android versionName")
    version_name = match.group(1)
    if version_name in {"1.4", "1.5", "1.5.1", "1.6", "1.6.1"}:
        raise RuntimeError("Refusing to overwrite an earlier release archive")
    versioned_apk = RELEASE_DIR / f"NJMU-AutoAuth-v{version_name}-OneUI.apk"
    if versioned_apk.exists(): raise RuntimeError("Versioned release exists: bump the version first")
    shutil.copy2(out_apk, versioned_apk)
    generated_idsig = RELEASE_DIR / "NJMU-AutoAuth.apk.idsig"
    if generated_idsig.exists():
        shutil.copy2(generated_idsig, RELEASE_DIR / f"{versioned_apk.name}.idsig")
    run_cmd([bt / "apksigner.bat", "verify", versioned_apk], desc=f"验证 v{version_name} 发布版签名")

    print(f"\n🎉 恭喜！构建成功！产物路径: {out_apk} (大小: {out_apk.stat().st_size} 字节)")

if __name__ == "__main__":
    main()
