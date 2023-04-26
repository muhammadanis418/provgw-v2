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
public class SubAdditionalParameters {
    
    private boolean goldenSubscriptionRequest = false;
    private boolean freeSubscription = false;
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
     * - <b>Normal DN</b> (isFree=False, isGolden=False)
     * - <b>Normal DN for FREE</b> (isFree=True, isGolden=False)
     * - <b>Golden DN</b> (isFree=False, isGolden=True)
     * user <b>can NOT</b> subscribe to <b>Golden DN</b> for <b>FREE</b> through SMS
     * </pre>
     * @param sendSms 
     * @param smsLogStr
     * @param isFree
     * @param isGolden
     * @param subOnLowBal
     */
    public SubAdditionalParameters(boolean sendSms,
            String smsLogStr, boolean isFree, boolean isGolden,
            boolean subOnLowBal) {
        setFreeSubscription(isFree);
        setGoldenSubscriptionRequest(isGolden);
        this.subOnChargingFailure = subOnLowBal;
        this.sendSms = sendSms;
        this.logStr = smsLogStr;
    }
    
    public boolean isGoldenSubscriptionRequest() {
        return goldenSubscriptionRequest;
    }
    
    /**
     * 
     * <pre>
     * Default is false
     * If set to True @see freeSubscription will be set to False
     * </pre>
     * @param goldenSubscriptionRequest 
     */
    public void setGoldenSubscriptionRequest(boolean goldenSubscriptionRequest) {
        this.goldenSubscriptionRequest = goldenSubscriptionRequest;
        if(this.goldenSubscriptionRequest) {
            this.freeSubscription = false;
        }
    }

    public boolean isFreeSubscription() {
        return freeSubscription;
    }

    /**
     * <pre>
     * Default is false
     * If set to True @see goldenSubscriptionRequest will be set to False
     * </pre>
     * @param freeSubscription 
     */
    public void setFreeSubscription(boolean freeSubscription) {
        this.freeSubscription = freeSubscription;
        if (this.freeSubscription) {
            this.goldenSubscriptionRequest = false;
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

//    public boolean isSendSms() {
//        return sendSms;
//    }

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
        return "SubAdditionalParameters{"
                + "Golden=" + goldenSubscriptionRequest
                + ", FREE=" + freeSubscription 
                + ", subOnChargingFailure=" + subOnChargingFailure 
                + ", sendSms=" + sendSms
                + ", sendAkkaReply=" + sendAkkaReply 
                + ", logStr=" + logStr
                + ", newSubscriber=" + newSubscriber
                + ", goldenDN=" + goldenDN
                + "}";
    }
    
}
