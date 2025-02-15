package kh.com.cellcard.common.helper;

public abstract class NumberHelper {

    public static int toIntOrDefault(String value, int defaultValue) {
        return StringHelper.isNullOrEmpty(value) ? defaultValue : parseOrDefault(value, defaultValue);
    }

    public static int parseOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception Ignore) {
            return defaultValue;
        }
    }
}
