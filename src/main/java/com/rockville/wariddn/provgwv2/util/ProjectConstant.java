package com.rockville.wariddn.provgwv2.util;

public enum ProjectConstant {

    BUNDLE_BASIC("100"),
    BUNDLE_PLUS("101");

    public String value;

    private ProjectConstant(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public ProjectConstant getValue(String value) {
        for (ProjectConstant e : ProjectConstant.values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;// not found
    }
}
