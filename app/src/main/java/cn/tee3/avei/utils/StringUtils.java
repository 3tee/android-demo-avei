package cn.tee3.avei.utils;

import java.util.UUID;

public class StringUtils {
    public static boolean isEmpty(String str) {
        if (str != null && !"".equalsIgnoreCase(str.trim())
                && !"null".equalsIgnoreCase(str.trim())) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean isNotEmpty(String string) {
        return null != string && string.length() > 0 && !"".equals(string);
    }

    public static String getUUID() {
        UUID uuid = UUID.randomUUID();
        String uniqueId = uuid.toString();

        return uniqueId;
    }
}
