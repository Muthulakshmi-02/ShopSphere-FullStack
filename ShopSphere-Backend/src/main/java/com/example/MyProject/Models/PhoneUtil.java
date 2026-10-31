package com.example.MyProject.Models;

/**
 * Stores every Indian mobile number in ONE format: the plain 10 digits.
 * "+91 98765-43210", "+919876543210" and "9876543210" all become "9876543210",
 * so the same customer never shows up with two different-looking numbers.
 * The format itself is already validated by the @Pattern on AddressRequest / OrderRequest.
 */
public final class PhoneUtil {

    private PhoneUtil() {}

    public static String normalize(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }
}