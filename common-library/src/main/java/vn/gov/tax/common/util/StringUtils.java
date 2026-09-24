package vn.gov.tax.common.util;

public final class StringUtils {
    private StringUtils() { }
    public static boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    public static String defaultIfBlank(String value, String fallback) { return isBlank(value) ? fallback : value; }
}
