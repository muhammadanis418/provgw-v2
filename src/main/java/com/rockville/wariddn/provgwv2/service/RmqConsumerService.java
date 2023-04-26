package com.rockville.wariddn.provgwv2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockville.wariddn.provgwv2.config.DnSmsHandlingConfig;
import com.rockville.wariddn.provgwv2.dto.IncomingSMS;
import com.rockville.wariddn.provgwv2.dto.Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RmqConsumerService {

    private final DnSmsHandlingConfig dnSmsHandlingConfig;
    private final ObjectMapper objectMapper;
    private final DroolsDecisionService droolsDecisionService;
    
    private final AmqpTemplate amqpTemplate;
    
    @RabbitListener(queues = "${sms.receivingSmsQueue}")
    public void receiveSms(String smsJson) {
        try {
            log.info("Sms Json Received : {}", smsJson);
            IncomingSMS sms = objectMapper.readValue(smsJson, IncomingSMS.class);
            log.info("IncomingSMS :" + sms);
            String raw_sms = new String(sms.getMessageBody());
            if (raw_sms.isBlank()) {
                /*
                2022-12-06 16:57:11,801 [org.springframework.amqp.rabbit.RabbitListenerEndpointContainer#0-1] INFO  c.r.w.p.service.RmqConsumerService - Sms Json Received : {"LA":"wdnvx1","destination":"4030","source":"923219473210","messageBody":[32],"messageLength":1,"optionalParamCount":0,"messageId":0,"sequenceNum":986,"esmClass":"0","messageEncoding":"0","sourceTon":1,"sourceNpi":1,"destinationTon":0,"destinationNpi":1,"priority":0,"registeredDelivery":0}
                2022-12-06 16:57:11,801 [org.springframework.amqp.rabbit.RabbitListenerEndpointContainer#0-1] INFO  c.r.w.p.service.RmqConsumerService - IncomingSMS :IncomingSMS{LA=wdnvx1, destination=4030, source=923219473210, messageBody= , messageLength=1, optionalParamCount=0, messageId=0, sequenceNum=986, optionalParams=null, esmClass=0, messageEncoding=0, sourceTon=1, sourceNpi=1, destinationTon=0, destinationNpi=1, priority=0, registeredDelivery=0}
                2022-12-06 16:57:11,801 [org.springframework.amqp.rabbit.RabbitListenerEndpointContainer#0-1] WARN  o.s.a.r.l.ConditionalRejectingErrorHandler - Execution of Rabbit message listener failed.

                Caused by: java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
                at com.rockville.wariddn.provgwv2.service.RmqConsumerService.receiveSms(RmqConsumerService.java:35)
                */
                raw_sms = "help";
            }
            String raw_sms_lower_case = raw_sms.toLowerCase();
            String command = "";
            String[] sms_arr = raw_sms_lower_case.split("\\s+");
            String channel = "SMS";
            // for null messages
            if (raw_sms_lower_case.length() > 0) {
                command = sms_arr[0].toLowerCase();
                // to get the OPTIN vchannel of the command
                // when provided, must be in following fromat
                // origin:IVR or origin:WS or origin:CSI
                if (raw_sms_lower_case.contains("origin:")) {
                    log.debug("XMS Contains Origin:");
                    for (int i = 0; i <= sms_arr.length - 1; i++) {
                        if (sms_arr[i].contains("origin:")) {
                            try {
                                channel = sms_arr[i].split(":")[1];
                                log.debug("Channel got in XMS: " + channel);
                            } catch (Exception e) {
                                log.warn("unable to extract Channel: " + channel);
                            }
                            break;
                        }
                    }
                }
            }
            log.debug("command[0] => " + command + " | SMS from RabbitMQ => " + smsJson);
            Request r = parseSMS(sms, channel.toUpperCase(), smsJson);
            r.setDescription(raw_sms_lower_case);
            
            if (dnSmsHandlingConfig.getCommands().containsKey(command)) {
                if (dnSmsHandlingConfig.getCommands().get(command).equalsIgnoreCase("EXT")) {
                    // verify if the array length is greater than 2
                    if (sms_arr.length > 1 && dnSmsHandlingConfig.getCommands().containsKey(command + " " + sms_arr[1])) {
                        r.setMethodName(dnSmsHandlingConfig.getCommands().get(command + " " + sms_arr[1]));
                    } else {
                        r.setMethodName(dnSmsHandlingConfig.getDefaultCommand());
                        log.warn("Invalid EXT Command");
                    }
                    log.debug("checking EXT cm.get(" + command + ") = " + dnSmsHandlingConfig.getCommands().get(command));
                } else {
                    r.setMethodName(dnSmsHandlingConfig.getCommands().get(command));
                    log.debug("not EXT");
                }
            } else {
                log.warn("command not found");
                r.setMethodName(dnSmsHandlingConfig.getDefaultCommand());
            }
            droolsDecisionService.processRequest(r);
        } catch (JsonProcessingException jsonProcessingException) {
            log.error(jsonProcessingException.getMessage());
            log.error("Re-Queuing SMS: {}", smsJson);
            amqpTemplate.convertAndSend(dnSmsHandlingConfig.getAltCmdExchange(), dnSmsHandlingConfig.getAltCmdRoutingKey(), smsJson);
        }
    }
    
    private Request parseSMS(IncomingSMS sms, String channel, String jsonSms) {
        Request request = new Request();
        request.setMsisdn(sms.getSource());
        request.setTransactionId(sms.getSource() + "_" + System.nanoTime());
        request.setService(channel);
        request.setDestination(sms.getDestination());
        request.setOriginalRequest(jsonSms);
        return request;
    }
}
