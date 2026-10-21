package com.A509.Security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Stores new passwords with BCrypt while allowing existing plaintext records
 * created by the old NoOpPasswordEncoder to keep working during migration.
 */
public final class LegacyCompatiblePasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        if (encodedPassword.startsWith("$2a$")
                || encodedPassword.startsWith("$2b$")
                || encodedPassword.startsWith("$2y$")) {
            return bcrypt.matches(rawPassword, encodedPassword);
        }
        return rawPassword.toString().equals(encodedPassword);
    }
}
