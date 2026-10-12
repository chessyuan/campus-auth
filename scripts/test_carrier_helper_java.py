# -*- coding: utf-8 -*-
"""Execute the *actual Java* carrier mapping/validation, not a Python shadow implementation."""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "android/src/local/njmu/autologin/CarrierHelper.java"
JAVA_TEST = r"""
import local.njmu.autologin.CarrierHelper;
public class CarrierHelperCheck {
    static void eq(String actual, String expected) {
        if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
    }
    static void valid(String user, boolean expected) {
        if (CarrierHelper.isValidUsernameInput(user) != expected)
            throw new AssertionError("validation mismatch: " + user);
    }
    public static void main(String[] a) {
        eq(CarrierHelper.formatUsername("2024001", "cmcc"), "2024001@cmcc");
        eq(CarrierHelper.formatUsername("2024001", "telecom"), "2024001@telecom");
        eq(CarrierHelper.formatUsername("2024001", "unicom"), "2024001@unicom");
        eq(CarrierHelper.formatUsername("2024001", "campus"), "2024001");
        eq(CarrierHelper.formatUsername("2024001@telecom", "cmcc"), "2024001@cmcc");
        eq(CarrierHelper.inferCarrier("2024001@unicom", null), "unicom");
        eq(CarrierHelper.inferCarrier("2024001", null), "cmcc");
        valid("2024001", true);
        valid("2024001@CMCC", true);
        valid("2024001@telecom", true);
        valid("2024001@unicom", true);
        valid("2024001@unknown", false);
        valid("2024001@cmcc@unicom", false);
        valid("2024001@", false);
        valid("2024 001", false);
        valid("", false);
        System.out.println("PASS: 16 real-Java carrier mapping and input validation assertions");
    }
}
"""
with TemporaryDirectory() as tmp:
    tmp_path = Path(tmp)
    test_file = tmp_path / "CarrierHelperCheck.java"
    test_file.write_text(JAVA_TEST, encoding="utf-8")
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(tmp_path), str(SOURCE), str(test_file)], check=True)
    subprocess.run(["java", "-cp", str(tmp_path), "CarrierHelperCheck"], check=True)
