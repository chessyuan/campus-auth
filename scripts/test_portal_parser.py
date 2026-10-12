# -*- coding: utf-8 -*-
"""
NJMU 校园网重定向解析回归测试
模拟学校 Aruba 认证网关的 HTML Meta Refresh 跳转与 HTTP 302 重定向，
验证 AuthService 内部的正则表达式与逻辑是否准确解析真实的校园网跳转 URL。
"""
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
AUTH_JAVA = ROOT / "android" / "src" / "local" / "njmu" / "autologin" / "AuthEngine.java"

def test_portal_parser():
    java_src = AUTH_JAVA.read_text(encoding="utf-8")
    match = re.search(r"Pattern\.compile\((.*?)\)\.matcher", java_src)
    if not match:
        raise AssertionError("未在 AuthService.java 中找到跳转解析正则！")
    expr = match.group(1)
    print("提取到的真实跳转解析正则:", expr)

    test_code = f"""
public class PortalParserCheck {{
    public static void main(String[] args) {{
        String body = "<html><head><meta http-equiv='refresh' content='1; url=http://www.msftconnecttest.com/connecttest.txt?cmd=redirect&arubalp=12345'></head></html>";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile({expr}).matcher(body);
        if (!m.find() || !m.group(1).equals("http://www.msftconnecttest.com/connecttest.txt?cmd=redirect&arubalp=12345")) {{
            throw new AssertionError("真实校园网重定向解析失败！");
        }}
        System.out.println("✔ Aruba 校园网 HTML 重定向解析回归测试: PASS");
    }}
}}
"""
    temp_java = ROOT / "scripts" / "PortalParserCheck.java"
    temp_java.write_text(test_code, encoding="utf-8")
    try:
        subprocess.run(["javac", str(temp_java)], check=True)
        subprocess.run(["java", "-cp", str(ROOT / "scripts"), "PortalParserCheck"], check=True)
    finally:
        if temp_java.exists():
            temp_java.unlink()
        class_file = ROOT / "scripts" / "PortalParserCheck.class"
        if class_file.exists():
            class_file.unlink()

if __name__ == "__main__":
    test_portal_parser()
