/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rockville.wariddn.provgwv2.dto;

/**
 *
 * @author Danish
 */
public class RabbitModel {

    private String productID;
    private String msisdn;
    private String channel;
    private String type;
    private String event;
    private int userType;
    private String provisioningStatus;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public String getProvisioningStatus() {
        return provisioningStatus;
    }

    public void setProvisioningStatus(String provisioningStatus) {
        this.provisioningStatus = provisioningStatus;
    }
    
    

    @Override
    public String toString() {
        return "{" +
                "productID='" + productID + '\'' +
                ", msisdn='" + msisdn + '\'' +
                ", channel='" + channel + '\'' +
                ", type='" + type + '\'' +
                ", event='" + event + '\'' +
                ", userType=" + userType +
                ", provisioningStatus='" + provisioningStatus + '\'' +
                '}';
    }
}
