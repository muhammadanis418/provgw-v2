package com.rockville.wariddn.provgwv2.dto;

import com.cloudhopper.smpp.tlv.Tlv;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;

@Getter
@Setter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class IncomingSMS implements Serializable {

    @JsonProperty(value = "LA")
    private String LA;
    private String destination;
    private String source;
    private byte[] messageBody;
    private int messageLength;
    private int optionalParamCount;
    private int messageId;
    private int sequenceNum;
    private ArrayList<Tlv> optionalParams;
    private String esmClass;
    private String messageEncoding;
    private int sourceTon = -1;
    private int sourceNpi = -1;
    private int destinationTon = -1;
    private int destinationNpi = -1;
    private int priority = -1;
    private int registeredDelivery = -1;

    @Override
    public String toString() {
        for (byte b : messageBody) {
            System.out.println(b);
        }
        return "IncomingSMS{" + "LA=" + LA + ", destination=" + destination + ", source=" + source + ", messageBody=" + new String(messageBody) + ", messageLength=" + messageLength + ", optionalParamCount=" + optionalParamCount + ", messageId=" + messageId + ", sequenceNum=" + sequenceNum + ", optionalParams=" + optionalParams + ", esmClass=" + esmClass + ", messageEncoding=" + messageEncoding + ", sourceTon=" + sourceTon + ", sourceNpi=" + sourceNpi + ", destinationTon=" + destinationTon + ", destinationNpi=" + destinationNpi + ", priority=" + priority + ", registeredDelivery=" + registeredDelivery + '}';
    }
}
