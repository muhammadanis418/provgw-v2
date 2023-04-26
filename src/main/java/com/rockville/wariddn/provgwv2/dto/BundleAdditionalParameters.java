/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rockville.wariddn.provgwv2.dto;

/**
 *
 * @author Usman
 */
public class BundleAdditionalParameters {
    
    private boolean plusSubscriptionRequest = false;
    private boolean basicSubscription = false;
    private boolean subOnChargingFailure = false;
    private boolean sendSms = true;
    private boolean sendAkkaReply = false;
    private String logStr = "";
    private boolean newSubscriber = false;
    private String goldenDN = "";
    private String userNetworkAndPackage="";


    
    /**
     * <pre>
     * Either a user can subscribe to
     * - <b>Basic Bundle DN</b> (isBasic=False, isPlus=False)
     * - <b>Basic Bundle DN</b> (isBasic=True, isPlus=False)
     * - <b>Plus Bundle DN</b> (isBasic=False, isPlus=True)
     * </pre>
     * @param sendSms 
     * @param sendAkkaReply 
     * @param smsLogStr 
     * @param isBasic
     * @param isPlus
     * @param subOnLowBal
     */
    public BundleAdditionalParameters(boolean sendSms, boolean sendAkkaReply,
            String smsLogStr, boolean isBasic, boolean isPlus,
            boolean subOnLowBal) {
        setBasicSubscription(isBasic);
        setPlusSubscriptionRequest(isPlus);
        this.subOnChargingFailure = subOnLowBal;
        this.sendSms = sendSms;
        this.sendAkkaReply = sendAkkaReply;
        this.logStr = smsLogStr;
    }
    
    public boolean isPlusSubscriptionRequest() {
        return plusSubscriptionRequest;
    }
    
    /**
     * 
     * <pre>
     * Default is false
     * If set to True @see basicSubscription will be set to False
     * </pre>
     * @param plusSubscriptionRequest 
     */
    public void setPlusSubscriptionRequest(boolean plusSubscriptionRequest) {
        this.plusSubscriptionRequest = plusSubscriptionRequest;
        if(this.plusSubscriptionRequest) {
            this.basicSubscription = false;
        }
    }

    public boolean isBasicSubscription() {
        return basicSubscription;
    }

    /**
     * <pre>
     * Default is false
     * If set to True @see plusSubscriptionRequest will be set to False
     * </pre>
     * @param basicSubscription 
     */
    public void setBasicSubscription(boolean basicSubscription) {
        this.basicSubscription = basicSubscription;
        if (this.basicSubscription) {
            this.plusSubscriptionRequest = false;
        }
    }

    public boolean isSubOnChargingFailure() {
        return subOnChargingFailure;
    }

    /**
     * Default is false
     * @param subOnChargingFailure 
     */
    public void setSubOnChargingFailure(boolean subOnChargingFailure) {
        this.subOnChargingFailure = subOnChargingFailure;
    }

    public boolean isSendSms() {
        return sendSms;
    }

    /**
     * Default is true
     * @param sendSms 
     */
    public void setSendSms(boolean sendSms) {
        this.sendSms = sendSms;
    }

    public boolean isSendAkkaReply() {
        return sendAkkaReply;
    }

    /**
     * Default is false
     * @param sendAkkaReply 
     */
    public void setSendAkkaReply(boolean sendAkkaReply) {
        this.sendAkkaReply = sendAkkaReply;
    }

    public String getLogStr() {
        return logStr;
    }

    /**
     * Default is empty
     * @param logStr 
     */
    public void setLogStr(String logStr) {
        this.logStr = logStr;
    }

    public boolean isNewSubscriber() {
        return newSubscriber;
    }

    /**
     * Default is false
     * @param newSubscriber 
     */
    public void setNewSubscriber(boolean newSubscriber) {
        this.newSubscriber = newSubscriber;
    }

    public String getGoldenDN() {
        return goldenDN;
    }

    public void setGoldenDN(String goldenDN) {
        this.goldenDN = goldenDN;
    }

    public String getUserNetworkAndPackage() {
        return userNetworkAndPackage;
    }

    public void setUserNetworkAndPackage(String userNetworkAndPackage) {
        this.userNetworkAndPackage = userNetworkAndPackage;
    }
    
    
    

    @Override
    public String toString() {
        return "BundleAdditionalParameters{"
                + "Plus=" + plusSubscriptionRequest
                + ", Basic=" + basicSubscription 
                + ", subOnChargingFailure=" + subOnChargingFailure 
                + ", sendSms=" + sendSms
                + ", sendAkkaReply=" + sendAkkaReply 
                + ", logStr=" + logStr
                + ", newSubscriber=" + newSubscriber
                + ", goldenDN=" + goldenDN
                + "}";
    }
    
}
