package com.rockville.wariddn.provgwv2.enums;

public enum TelcoType {
    postpaid(1),
    prepaid(2);

    private final int ordinal;
    private TelcoType(int ordinal) {
        this.ordinal = ordinal;
    }
    public int getIntOrdinal() {
        return ordinal;
    }
}
