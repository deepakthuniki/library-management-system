package com.library.util;

import java.util.regex.Pattern;

public final class ValidationUtil {
    private ValidationUtil() { }

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+]?[0-9]{10,15}$");
    private static final Pattern ISBN_PATTERN =
            Pattern.compile("^(97(8|9))?\\d{9}(\\d|X)$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.replaceAll("[\\s-]", "")).matches();
    }

    public static boolean isValidIsbn(String isbn) {
        return isbn != null && ISBN_PATTERN.matcher(isbn.replaceAll("-", "")).matches();
    }

    public static boolean isNonBlank(String s) {
        return s != null && !s.isBlank();
    }

    public static boolean isPositive(int n) {
        return n > 0;
    }
}
