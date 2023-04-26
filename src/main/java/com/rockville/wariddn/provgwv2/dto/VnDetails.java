/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rockville.wariddn.provgwv2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 * @author Usman
 */
public class VnDetails {

    @JsonProperty("TelcoMSISDN")
    private long virtualMSISDN;
    @JsonProperty("TelcoMSISDN")
    private long telcoMSISDN;
    @JsonProperty("TStamp")
    private Date tStamp;
    @JsonProperty("Status")
    private int status;
    @JsonProperty("WebPassword")
    private String webPassword;
    @JsonProperty("CarryForward")
    private int carryForward;
    @JsonProperty("BilledUpto")
    private Date billedUpto;
    @JsonProperty("BillingTries")
    private int billingTries;
    @JsonProperty("Region")
    private int region;
    @JsonProperty("SCPID")
    private int SCPID;
    @JsonProperty("SubscriptionDate")
    private Date subscriptionDate;
    @JsonProperty("SubReqCount")
    private int subReqCount;
    @JsonProperty("TelcoType")
    private int telcoType;
    @JsonProperty("Remarks")
    private String remarks;
    @JsonProperty("TypeOfNum")
    private int typeOfNum;
    @JsonProperty("UnSubDate")
    private Date unSubDate;

    public long getVirtualMSISDN() {
        return virtualMSISDN;
    }

    public void setVirtualMSISDN(long virtualMSISDN) {
        this.virtualMSISDN = virtualMSISDN;
    }

    public long getTelcoMSISDN() {
        return telcoMSISDN;
    }

    public void setTelcoMSISDN(long telcoMSISDN) {
        this.telcoMSISDN = telcoMSISDN;
    }

    public Date gettStamp() {
        return tStamp;
    }

    public void settStamp(Date tStamp) {
        this.tStamp = tStamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getWebPassword() {
        return webPassword;
    }

    public void setWebPassword(String webPassword) {
        this.webPassword = webPassword;
    }

    public int getCarryForward() {
        return carryForward;
    }

    public void setCarryForward(int carryForward) {
        this.carryForward = carryForward;
    }

    public Date getBilledUpto() {
        return billedUpto;
    }

    public void setBilledUpto(Date billedUpto) {
        this.billedUpto = billedUpto;
    }

    public int getBillingTries() {
        return billingTries;
    }

    public void setBillingTries(int billingTries) {
        this.billingTries = billingTries;
    }

    public int getRegion() {
        return region;
    }

    public void setRegion(int region) {
        this.region = region;
    }

    public int getSCPID() {
        return SCPID;
    }

    public void setSCPID(int SCPID) {
        this.SCPID = SCPID;
    }

    public Date getSubscriptionDate() {
        return subscriptionDate;
    }

    public void setSubscriptionDate(Date subscriptionDate) {
        this.subscriptionDate = subscriptionDate;
    }

    public int getSubReqCount() {
        return subReqCount;
    }

    public void setSubReqCount(int subReqCount) {
        this.subReqCount = subReqCount;
    }

    public int getTelcoType() {
        return telcoType;
    }

    public void setTelcoType(int telcoType) {
        this.telcoType = telcoType;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public int getTypeOfNum() {
        return typeOfNum;
    }

    public void setTypeOfNum(int typeOfNum) {
        this.typeOfNum = typeOfNum;
    }

    public Date getUnSubDate() {
        return unSubDate;
    }

    public void setUnSubDate(Date unSubDate) {
        this.unSubDate = unSubDate;
    }

    @Override
    public String toString() {
        return "VnDetails{" + "virtualMSISDN=" + virtualMSISDN + ", telcoMSISDN=" + telcoMSISDN + ", tStamp=" + tStamp + ", status=" + status + ", webPassword=" + webPassword + ", carryForward=" + carryForward + ", billedUpto=" + billedUpto + ", billingTries=" + billingTries + ", region=" + region + ", SCPID=" + SCPID + ", subscriptionDate=" + subscriptionDate + ", subReqCount=" + subReqCount + ", telcoType=" + telcoType + ", remarks=" + remarks + ", typeOfNum=" + typeOfNum + ", unSubDate=" + unSubDate + '}';
    }


}
