package com.rockville.wariddn.provgwv2.enums;

public enum ResponseEnum {
    FAILURE,
    ALREADY_SUB,
    ALREADY_ON,
    ALREADY_OFF,
    NOT_SUB,
    SUCCESS,
    FAILURE_LOW_BAL,
    FAILURE_POSTPAID,
    FAILURE_MYFI,
    FAILURE_OFFNET,
    SUB_NOT_INITIATED,
    UCIP_NO_RESPONSE,
    GOLDEN, // Used in ParentSubAllNewImpl
    NORMAL,  // Used in ParentSubAllNewImpl
    BUNDLE_BASIC,
    BUNDLE_PLUS,
    ALREADY_SENT_GOLDEN_NUMBERS, //used in parentRawSub
    DBSS_SUB_FAILED, // Used in ParentSubAllNewImpl
    DBSS_UNSUB_FAILED // Used in ParentUnsub and ParentUnsubBundle
    ;
}
