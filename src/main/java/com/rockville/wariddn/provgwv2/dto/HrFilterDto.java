package com.rockville.wariddn.provgwv2.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class HrFilterDto {
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;
}
