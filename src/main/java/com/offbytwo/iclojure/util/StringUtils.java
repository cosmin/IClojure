package com.offbytwo.iclojure.util;

public class StringUtils {
    public static String join(String separator, Object... members) {
        StringBuffer sb = new StringBuffer();

        switch (members.length) {
            case 0:
                break;
            case 1:
                sb.append(members[0].toString());
                break;
            default:
                for (int i = 0; i < members.length - 1; i++) {
                    sb.append(members[i]);
                    sb.append(separator);
                }

                sb.append(members[members.length - 1]);
        }

        return sb.toString();
    }
}
