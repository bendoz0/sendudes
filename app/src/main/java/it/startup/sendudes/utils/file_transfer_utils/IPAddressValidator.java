package it.startup.sendudes.utils.file_transfer_utils;

import java.util.regex.Pattern;

public class IPAddressValidator {

    private static final String IPV4_PATTERN =
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    private static final Pattern pattern = Pattern.compile(IPV4_PATTERN);

    public static boolean isValidIPv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return pattern.matcher(ip).matches();
    }
}