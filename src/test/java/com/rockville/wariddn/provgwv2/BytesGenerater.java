package com.rockville.wariddn.provgwv2;

import java.util.Arrays;

public class BytesGenerater {

    public static void main(String[] args) {
        String str = "UPDATE NumberMapping set language={lang} where TelcoMSISDN={msisdn}";
        str = str.replace("{lang}", "en").replace("{msisdn}", "3035033985");
        System.out.println("Value : " + str);
    }
}
