package kh.com.cellcard.common.wrapper;


import org.springframework.security.crypto.codec.Utf8;

import java.util.Base64;

public class Base64Wrapper {
    public static String decode(String encoded) {
        return Utf8.decode(Base64.getDecoder().decode(encoded));
    }
    public static String encode(String plainText) {
        return Base64.getEncoder().encodeToString(plainText.getBytes());
    }
}
