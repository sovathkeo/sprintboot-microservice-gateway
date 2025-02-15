package kh.com.cellcard.common.wrapper;

import java.util.UUID;

public class UuidWrapper {

    public static UUID uuid() {
        return UUID.randomUUID();
    }

    public static String uuidAsString() {
        return uuid().toString();
    }
}
