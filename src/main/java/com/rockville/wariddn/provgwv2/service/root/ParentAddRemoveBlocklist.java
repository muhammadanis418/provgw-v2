package com.rockville.wariddn.provgwv2.service.root;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockville.wariddn.provgwv2.dto.BlockedDto;
import com.rockville.wariddn.provgwv2.dto.IncomingSMS;
import com.rockville.wariddn.provgwv2.dto.Request;
import com.rockville.wariddn.provgwv2.dto.VnDetails;
import com.rockville.wariddn.provgwv2.service.Operation;
import com.rockville.wariddn.provgwv2.service.SmsSendingService;
import com.rockville.wariddn.provgwv2.util.CommonOperations;
import com.rockville.wariddn.provgwv2.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParentAddRemoveBlocklist extends Operation {

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
                log.info("SMS : {}" + msg);
                if (msg.size() > 1) {
                    if (Pattern.compile("^((\\+|\\+92|92|0|)(3\\d{9}\\b))+$").matcher(msg.get(2)).matches() == true) {
                        String memberMSISDN = Operation.normalizeMsisdn(msg.get(2));
                        log.info("MemberMSISDN = " + memberMSISDN);
                        List<BlockedDto> blocked = jdbcTemplate.query(
                                constants.getQRY_TEMPLATE().get("QRY_GET_BLACKED_LIST")
                                        //getQRY_GET_BLACKED_LIST()
                                        .replace("{TNUM}", request.getMsisdn())
                                        .replace("{BLOCKED_NUM}", memberMSISDN),
                                new BeanPropertyRowMapper<>(BlockedDto.class)
                        );
                        log.info("User aksed to " + request.getCommand() + " " + memberMSISDN);
                        Boolean execQuery = false;
                        switch (request.getCommand()) {
                            case "ADD":
                                log.info("[" + request.getTransactionId() + "] Subscriber want to Block:" + memberMSISDN);
                                if (!blocked.isEmpty() && memberMSISDN.equals(blocked.get(0).getBlocked())
                                        && blocked.get(0).getIsActive() == 1) {
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_ALREADY_BLOCKED_LISTED_" + request.getLanguage());
                                    break;
                                } else if (!blocked.isEmpty() && memberMSISDN.equals(blocked.get(0).getBlocked())
                                        && blocked.get(0).getIsActive() == 0) {
                                    qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_ACTIVE_BLACKED_LIST")
                                            //getQRY_ACTIVE_BLACKED_LIST()
                                            .replace("{TNUM}", request.getMsisdn())
                                            .replace("{BLOCKED_NUM}", memberMSISDN);
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_ADDED_BLOCKED_LIST_" + request.getLanguage());
                                    execQuery = true;
                                    break;
                                } else {
                                    qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_ADD_BLACKED_LIST")
                                            //getQRY_ADD_BLACKED_LIST()
                                            .replace("{TNUM}", request.getMsisdn())
                                            .replace("{BLOCKED_NUM}", memberMSISDN);
                                    execQuery = true;
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_ADDED_BLOCKED_LIST_" + request.getLanguage());
                                    break;
                                }
                            case "REMOVE":
                                log.info("Handling: " + request.getCommand());
                                log.info("[" + request.getTransactionId() + "] Subscriber want to UnBlock:" + msg.get(2) + " " + memberMSISDN);
                                if (!blocked.isEmpty() && memberMSISDN.equals(blocked.get(0).getBlocked())
                                        && blocked.get(0).getIsActive() == 1) {
                                    qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_REMOVE_BLACKED_LIST")
                                            //getQRY_REMOVE_BLACKED_LIST()
                                            .replace("{TNUM}", request.getMsisdn())
                                            .replace("{BLOCKED_NUM}", memberMSISDN);
                                    execQuery = true;
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_REMOVED_BLOCKED_LIST_" + request.getLanguage());
                                    break;
                                } else {
                                    //DON't do anything user hasn't blocked any number
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_NOT_IN_BLOCKED_LIST_" + request.getLanguage());
                                    break;
                                }
                        }
                        if (execQuery) {
                            log.info("Query : " + qry_after_param_replacement);
                            int res = jdbcTemplate.update(qry_after_param_replacement);
                            if (res > 0) {
                                smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                                        sms_string_after_replacing_params, request.getTransactionId(), request.getSendSms()
                                );
                                // NO BLOCK list maintained,inform customer accordingly
                                commonOperations.writeActivityLog(request, "BlockUnblock", "SUCCESS");

                            } else {
                                commonOperations.writeActivityLog(request, "BlockUnblock", "FAILED");
                            }
                        }
                    } else {
                        log.error("[" + request.getTransactionId() + "] Invalid Number");
                        commonOperations.writeActivityLog(request, "BlockUnblock", "Invalid");
                        sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_INVALID_CMD_BLOCK_UNBLOCK_HELP_" + request.getLanguage());
                    }
                } else {
                    log.error("[" + request.getTransactionId() + "] Invalid Command");
                    commonOperations.writeActivityLog(request, "BlockUnblock", "Invalid Command");
                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_INVALID_CMD_BLOCK_UNBLOCK_HELP_" + request.getLanguage());
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
