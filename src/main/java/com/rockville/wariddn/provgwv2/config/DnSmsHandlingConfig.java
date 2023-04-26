package com.rockville.wariddn.provgwv2.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sms")
public class DnSmsHandlingConfig {

    @Value("#{${dn.sms.commands}}")
    private Map<String, String> commands;

    @Value("#{${provgw.dbss.swap_channels}}")
    private Map<String, String> dbssSwapChannels;
    
    private String defaultCommand;
    private String receivingSmsQueue;
    private String outSmsExchange;
    private String outSmsRoutingKey;
    private String stringBody;
    private String altCmdExchange;
    private String altCmdRoutingKey;

}
