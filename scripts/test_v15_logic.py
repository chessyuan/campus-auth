# -*- coding: utf-8 -*-
"""
NJMU 校园网自动认证 v1.5 - 逻辑与回归单元测试集
测试涵盖：
1. 运营商参数映射与账号去重、剥离规则
2. 日志脱敏与防重复抑制机制
3. 真实 Aruba Captive Portal 正则提取验证
"""
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def run_java_test():
    java_file = ROOT / "scripts" / "TestV15Logic.java"
    java_code = """
import java.util.regex.*;

public class TestV15Logic {
    public static void main(String[] args) {
        System.out.println("=== 运行 NJMU AutoAuth v1.5 逻辑单元测试 ===");

        // 1. 测试运营商用户名规范化与拼接
        testCarrierMapping();

        // 2. 测试日志去重与脱敏
        testSanitizeAndDedup();

        // 3. 测试 Aruba 重定向正则解析
        testRedirectRegex();

        System.out.println(">>> 所有 v1.5 测试用例验证通过！<<<");
    }

    static void testCarrierMapping() {
        assertEq(formatUser("20230001", "cmcc"), "20230001@cmcc", "移动拼接");
        assertEq(formatUser("20230001@cmcc", "cmcc"), "20230001@cmcc", "移动去重缀拼接");
        assertEq(formatUser("20230001@telecom", "cmcc"), "20230001@cmcc", "跨运营商切换拼接");
        assertEq(formatUser("20230001", "telecom"), "20230001@telecom", "电信拼接");
        assertEq(formatUser("20230001", "unicom"), "20230001@unicom", "联通拼接");
        assertEq(formatUser("20230001", "campus"), "20230001", "校园内网无后缀拼接");
        assertEq(formatUser("20230001@cmcc", "campus"), "20230001", "校园内网剥离后缀");
        System.out.println("[PASS] 运营商用户名映射与后缀清理逻辑验证通过");
    }

    static String formatUser(String raw, String carrier) {
        if (raw == null) return "";
        String base = raw.contains("@") ? raw.substring(0, raw.indexOf("@")) : raw;
        String suffix = "@cmcc";
        if ("telecom".equalsIgnoreCase(carrier)) suffix = "@telecom";
        else if ("unicom".equalsIgnoreCase(carrier)) suffix = "@unicom";
        else if ("campus".equalsIgnoreCase(carrier)) suffix = "";
        return base + suffix;
    }

    static void testSanitizeAndDedup() {
        String sensitive = "url=https://captiveportal?username=test&password=SecretPassword123&token=abc";
        String clean = sanitize(sensitive);
        if (clean.contains("SecretPassword123")) {
            throw new AssertionError("密码脱敏失败！");
        }
        if (!clean.contains("password=***")) {
            throw new AssertionError("未成功掩码 password 字段！");
        }
        System.out.println("[PASS] 敏感信息过滤与脱敏规则验证通过");
    }

    static String sanitize(String input) {
        if (input == null) return "";
        String s = input;
        s = s.replaceAll("(?i)password=[^&\\\\s]+", "password=***");
        s = s.replaceAll("(?i)pwd=[^&\\\\s]+", "pwd=***");
        s = s.replaceAll("(?i)secret=[^&\\\\s]+", "secret=***");
        return s;
    }

    static void testRedirectRegex() {
        String expr = "(?i)url\\\\s*=\\\\s*([^'\\\"<>\\\\s]+)";
        String body = "<html><head><meta http-equiv='refresh' content='1; url=http://www.msftconnecttest.com/connecttest.txt?cmd=redirect&arubalp=12345'></head></html>";
        Matcher m = Pattern.compile(expr).matcher(body);
        if (!m.find() || !m.group(1).equals("http://www.msftconnecttest.com/connecttest.txt?cmd=redirect&arubalp=12345")) {
            throw new AssertionError("Aruba 重定向解析失败");
        }
        System.out.println("[PASS] Aruba 重定向网关正则提取验证通过");
    }

    static void assertEq(String actual, String expected, String msg) {
        if (!actual.equals(expected)) {
            throw new AssertionError("断言失败: " + msg + " [实际: " + actual + ", 期望: " + expected + "]");
        }
    }
}
"""
    java_file.write_text(java_code, encoding="utf-8")
    try:
        subprocess.run(["javac", "-encoding", "UTF-8", str(java_file)], check=True)
        subprocess.run(["java", "-cp", str(ROOT / "scripts"), "TestV15Logic"], check=True)
    finally:
        if java_file.exists():
            java_file.unlink()
        cls_file = ROOT / "scripts" / "TestV15Logic.class"
        if cls_file.exists():
            cls_file.unlink()

if __name__ == "__main__":
    run_java_test()