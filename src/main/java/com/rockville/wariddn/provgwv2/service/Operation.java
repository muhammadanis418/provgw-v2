package com.rockville.wariddn.provgwv2.service;

import com.rockville.wariddn.provgwv2.dto.Request;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class   Operation {

    public abstract void run(Request request);
    //---------THIS METHOD IS NOT USED-----------------
    @Deprecated
    public static boolean isParsableInt(String input) {
        boolean parsable = true;
        try {
            Integer.parseInt(input);
        } catch (NumberFormatException e) {
            parsable = false;
        }
        return parsable;
    }

    //---------THIS METHOD IS NOT USED-----------------//
    @Deprecated
    public static boolean isParsableLong(String input) {
        boolean parsable = true;
        try {
            Long.parseLong(input);
        } catch (NumberFormatException e) {
            parsable = false;
        }
        return parsable;
    }

    public static String normalizeMsisdn(String msisdn, String format) {
        if (msisdn == null) {
            return msisdn;
        }
        if (msisdn.startsWith("3") && msisdn.length() == 10) {
            return msisdn;
        } else if (msisdn.startsWith("03") && msisdn.length() == 11) {
            return msisdn.substring(1);
        } else if (msisdn.startsWith("92") && msisdn.length() == 12) {
            return msisdn.substring(2);
        } else if (msisdn.startsWith("+92") && msisdn.length() == 13) {
            return msisdn.substring(3);
        } else if (msisdn.startsWith("0092") && msisdn.length() == 14) {
            return msisdn.substring(4);
        } else {
            return null;
        }
    }

    public static String normalizeMsisdn(String msisdn) {
        if (msisdn == null) {
            return msisdn;
        }
        if (msisdn.startsWith("3") && msisdn.length() == 10) {
            return msisdn;
        } else if (msisdn.startsWith("03") && msisdn.length() == 11) {
            return msisdn.substring(1);
        } else if (msisdn.startsWith("92") && msisdn.length() == 12) {
            return msisdn.substring(2);
        } else if (msisdn.startsWith("+92") && msisdn.length() == 13) {
            return msisdn.substring(3);
        } else if (msisdn.startsWith("0092") && msisdn.length() == 14) {
            return msisdn.substring(4);
        } else {
            return null;
        }
    }

    public Request validateLanguage(Request request) {
        if (request.getLanguage() == null || request.getLanguage().isEmpty()) {
            request.setLanguage("UR");
        } else {
            request.setLanguage(request.getLanguage().toUpperCase());
        }
        return request;
    }

}
