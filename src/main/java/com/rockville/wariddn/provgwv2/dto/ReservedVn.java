package com.rockville.wariddn.provgwv2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ReservedVn {
    private String id;
    @JsonProperty("telcoMSISDN")
    private String telcoMsisdn;
    @JsonProperty("virtualMSISDN")
    private String virtualMsisdn;
    private String sequence;
    private String language;
    @JsonProperty("TStamp")
    private String tStamp;
    private String ctype;
    private String dtype;
}
