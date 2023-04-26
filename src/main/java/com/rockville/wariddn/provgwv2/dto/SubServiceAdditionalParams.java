/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rockville.wariddn.provgwv2.dto;

/**
 *
 * @author Nauman
 */
public class SubServiceAdditionalParams {
    private boolean sigRequest;
    private boolean statusRequest;

    public SubServiceAdditionalParams(boolean sigRequest, boolean statusRequest) {
        this.sigRequest = sigRequest;
        this.statusRequest = statusRequest;
    }

    public SubServiceAdditionalParams() {
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
    
}
