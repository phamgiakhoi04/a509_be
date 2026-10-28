package com.A509.Security;
import java.nio.charset.StandardCharsets;
public final class PasswordPolicy {
    private PasswordPolicy() {}
    public static void validate(String value) {
        if (value == null || value.length() < 8 || value.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Mật khẩu cần ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
    }
}
