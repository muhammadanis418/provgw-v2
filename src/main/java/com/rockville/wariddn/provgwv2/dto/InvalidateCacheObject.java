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

public class InvalidateCacheObject { 
    private final String telcoMSISDN;

    public InvalidateCacheObject(String telcoMSISDN) {
        this.telcoMSISDN = telcoMSISDN;
    }

    public String getTelcoMSISDN() {
        return telcoMSISDN;
    }

}
