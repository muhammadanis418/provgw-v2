package com.rockville.wariddn.provgwv2.service.root;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockville.wariddn.provgwv2.dto.IncomingSMS;
import com.rockville.wariddn.provgwv2.dto.Request;
import com.rockville.wariddn.provgwv2.dto.VnDetails;
import com.rockville.wariddn.provgwv2.dto.WhiteListDto;
import com.rockville.wariddn.provgwv2.enums.ResponseEnum;
import com.rockville.wariddn.provgwv2.service.Operation;
import com.rockville.wariddn.provgwv2.service.SmsSendingService;
import com.rockville.wariddn.provgwv2.util.CommonOperations;
import com.rockville.wariddn.provgwv2.util.Constants;
import com.rockville.wariddn.provgwv2.util.ProjectConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParentAddRemoveWhitelist extends Operation {

    private final Constants constants;
    private final CommonOperations commonOperations;
    private final SmsSendingService smsSendingService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void run(Request request) {
        try {

            if (request.getMsisdn() == null) {
                log.error("[" + request.getTransactionId() + "] MSISDN can not be null:" + request.getMsisdn());
                return;
            }
            request.setMsisdn(Operation.normalizeMsisdn(request.getMsisdn()));

            request = validateLanguage(request);

            log.info("Request : {}", request);

            String sms_string_after_replacing_params = "";
            String qry_after_param_replacement = "";

            VnDetails vnd = commonOperations.fetchSubscriptionDetailsByTelcoNum(request.getMsisdn());
            if (vnd != null) {
                log.info(vnd.toString());
            }

            if (vnd != null && vnd.getVirtualMSISDN() > 0 && vnd.getStatus() > 0 && vnd.getStatus() < 4) {
                IncomingSMS sms = objectMapper.readValue(request.getOriginalRequest(), IncomingSMS.class);
                List<String> msg = Arrays.asList(new String(sms.getMessageBody()).split("\\s+"));
                log.info("SMS" + msg);
                if (msg.size() > 1) {
                    if (Pattern.compile("^((\\+|\\+92|92|0|)(3\\d{9}\\b))+$").matcher(msg.get(2)).matches() == true) {
                        String memberMSISDN = Operation.normalizeMsisdn(msg.get(2));
                        log.info("MemberMSISDN = " + memberMSISDN);
                        List<WhiteListDto> whitelisted = jdbcTemplate.query(constants.getQRY_TEMPLATE().get("QRY_GET_WHITE_LIST")
                                      //  getQRY_GET_WHITE_LIST()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{WHITE_NUM}", memberMSISDN),
                                new BeanPropertyRowMapper<>(WhiteListDto.class)
                        );

                        log.info("User aksed to " + request.getCommand() + " " + memberMSISDN);
                        Boolean execQuery = false;
                        switch (request.getCommand()) {
                            case "ADD":
                                log.info("[" + request.getTransactionId() + "] Subscriber want to Add Whitelist:" + memberMSISDN);
                                if (!whitelisted.isEmpty() && memberMSISDN.equals(whitelisted.get(0).getWhitelisted())
                                        && whitelisted.get(0).getIsActive() == 1) {
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_ALREADY_WHITE_LISTED_" + request.getLanguage());
                                    break;
                                } else if (!whitelisted.isEmpty() && memberMSISDN.equals(whitelisted.get(0).getWhitelisted())
                                        && whitelisted.get(0).getIsActive() == 0) {
                                    qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_ACTIVE_WHITE_LIST")
                                            //getQRY_ACTIVE_WHITE_LIST()
                                            .replace("{TNUM}", request.getMsisdn())
                                            .replace("{WHITE_NUM}", memberMSISDN);
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_ADDED_WHITE_LIST_" + request.getLanguage());
                                    execQuery = true;
                                    break;
                                } else {
                                    qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_ADD_WHITE_LIST")
                                            //getQRY_ADD_WHITE_LIST()
                                            .replace("{TNUM}", request.getMsisdn())
                                            .replace("{WHITE_NUM}", memberMSISDN);
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_ADDED_WHITE_LIST_" + request.getLanguage());
                                    execQuery = true;
                                    break;
                                }
                            case "REMOVE":
                                log.info("[" + request.getTransactionId() + "] Subscriber want to REMOVE Whitelist:" + msg.get(1) + " " + memberMSISDN);
                                if (!whitelisted.isEmpty() && memberMSISDN.equals(whitelisted.get(0).getWhitelisted())
                                        && whitelisted.get(0).getIsActive() == 1) {
                                    qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_REMOVE_WHITE_LIST")
                                            //getQRY_REMOVE_WHITE_LIST()
                                            .replace("{TNUM}", request.getMsisdn())
                                            .replace("{WHITE_NUM}", memberMSISDN);
                                    execQuery = true;
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_REMOVED_WHITE_LIST_" + request.getLanguage());

                                } else {
                                    //DON't do anything user hasn't whitelisted any number
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_NOT_IN_WHITE_LIST_" + request.getLanguage());
                                    break;
                                }
                                break;
                        }
                        if (execQuery) {
                            log.info("Query : " + qry_after_param_replacement);
                            int res = jdbcTemplate.update(qry_after_param_replacement);
                            log.info("Result of query : " + res);
                            if (res > 0) {
                                smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                                        sms_string_after_replacing_params, request.getTransactionId(), request.getSendSms()
                                );
                                commonOperations.writeActivityLog(request, "ADDREMOVEWHITELIST", "SUCCESS");
                            } else {
                                commonOperations.writeActivityLog(request, "ADDREMOVEWHITELIST", "FAILED");
                            }
                        }
                    } else {
                        log.error("[" + request.getTransactionId() + "] Invalid Number");
                        commonOperations.writeActivityLog(request, "ADDREMOVEWHITELIST", "Invalid");
                        sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_INVALID_CMD_ADD_REMOVE_WHITELIST_HELP_" + request.getLanguage());
                    }
                } else {
                    log.error("[" + request.getTransactionId() + "] Invalid Command");
                    commonOperations.writeActivityLog(request, "ADDREMOVEWHITELIST", "Invalid Command");
                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_INVALID_CMD_ADD_REMOVE_WHITELIST_HELP_" + request.getLanguage());
                }

            } else {
                log.error("[" + request.getTransactionId() + "] Not A Subscriber");
                commonOperations.writeActivityLog(request, "BlockUnblock", "NonSub");
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_HELP_NOT_SUB_" + request.getLanguage());
            }

            smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                    sms_string_after_replacing_params,
                    request.getTransactionId(), request.getSendSms());
        } catch (Exception ex) {
            log.error("Exception: " + ex, ex);
        }
    }
}
