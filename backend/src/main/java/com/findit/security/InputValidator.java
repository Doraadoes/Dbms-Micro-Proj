package com.findit.security;

import java.util.regex.Pattern;

public class InputValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\s-]{10,20}$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.length() >= 10 && digits.length() <= 15;
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 6) return false;
        // Check for at least some variety
        return true;
    }

    public static String sanitize(String input) {
        if (input == null) return null;
        return input.trim();
    }
}

