package com.rockville.wariddn.provgwv2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CallFilterDto {
    @JsonProperty("MemberMSISDN")
    private String MemberMSISDN;
    @JsonProperty("CallStatus")
    private String CallStatus;
    @JsonProperty("SMSStatus")
    private String SMSStatus;
    @JsonProperty("Status")
    private String Status;
}
