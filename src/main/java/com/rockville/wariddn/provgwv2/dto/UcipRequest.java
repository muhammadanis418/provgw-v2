package com.rockville.wariddn.provgwv2.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UcipRequest {
    private String msisdn;
    private int deductionAmount;
    private String transactionId;
    private String responseCode;
    private String responseCodeDescription;
    private String originNodeType;
    private String originHostName;
    private String transactionCode;
    private String transactionType;
    private String transactionCurrency;
    private String externalData1;
    private String externalData2;
}
