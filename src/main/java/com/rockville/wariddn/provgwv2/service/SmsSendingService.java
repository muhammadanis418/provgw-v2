package com.rockville.wariddn.provgwv2.service;

import com.rockville.wariddn.provgwv2.config.DnSmsHandlingConfig;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsSendingService {

    private final AmqpTemplate amqpTemplate;
    private final DnSmsHandlingConfig smsHandlingConfig;

    public void sendSms(String Source, String Destination, String message, String TransactionId, boolean sendSMS) {
        if (!sendSMS) {
            log.info("NOT Sending SMS: { [" + TransactionId + "] " + Source + " > " + Destination + " :: " + message + "}");
        } else {
            try {
                byte[] body = message.getBytes();
                String sms = smsHandlingConfig.getStringBody()
                        .replace("DESTINATION", Destination)
                        .replace("SOURCE", Source)
                        .replace("BODY", Arrays.toString(body))
                        //                        .replace("BODY", message)
                        .replace("LENGHT", body.length + "");
                log.debug("Sending sms : {}", sms);
                amqpTemplate.convertAndSend(smsHandlingConfig.getOutSmsExchange(), smsHandlingConfig.getOutSmsRoutingKey(), sms);
            } catch (Exception e) {
                log.error("Exception in sendSms for source : {}, destination : {}", Source, Destination, e);
            }
        }
    }

    @Deprecated
    public void sendSms(String Source, String Destination, String message, String TransactionId) {
        sendSms(Source, Destination, message, TransactionId, true);
    }
}
