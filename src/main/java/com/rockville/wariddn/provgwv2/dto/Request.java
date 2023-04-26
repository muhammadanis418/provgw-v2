package com.rockville.wariddn.provgwv2.dto;

import lombok.ToString;

@ToString
public class Request {
    private String methodName; // the name of Class to run/invoke
    private String msisdn;
    private String destination;
    private String service; // IVR, SMS, USSD, DigitalCampaign,
    private String transactionId; // unique ID to identify each request
    private String originalRequest;
    private String language;
    private String description;
    private String virtualNumber;
    private String command;
    private boolean sendSms;
    private String groupName;
    private String digit;
    private boolean sigRequest;
    private boolean statusRequest;
    private SubAdditionalParameters subAdditionalParameters;

    public SubAdditionalParameters getSubAdditionalParameters() {
        return subAdditionalParameters;
    }

    public void setSubAdditionalParameters(SubAdditionalParameters subAdditionalParameters) {
        this.subAdditionalParameters = subAdditionalParameters;
    }

    public boolean isSigRequest() {
        return sigRequest;
    }

    public void setSigRequest(boolean sigRequest) {
        this.sigRequest = sigRequest;
    }

    public boolean isStatusRequest() {
        return statusRequest;
    }

    public void setStatusRequest(boolean statusRequest) {
        this.statusRequest = statusRequest;
    }

    public String getDigit() {
        return digit;
    }

    public void setDigit(String digit) {
        this.digit = digit;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public boolean getSendSms() {
        return sendSms;
    }

    public void setSendSms(boolean sendSms) {
        this.sendSms = sendSms;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getOriginalRequest() {
        return originalRequest;
    }

    public void setOriginalRequest(String originalRequest) {
        this.originalRequest = originalRequest;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVirtualNumber() {
        return virtualNumber;
    }

    public void setVirtualNumber(String virtualNumber) {
        this.virtualNumber = virtualNumber;
    }
}
