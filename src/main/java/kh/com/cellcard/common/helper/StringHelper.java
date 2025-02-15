package kh.com.cellcard.common.helper;

public abstract class StringHelper {

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty() || s.isBlank();
    }

    public static String getValueOrEmpty(String value) {
        return isNullOrEmpty(value) ? "" : value;
    }


}
