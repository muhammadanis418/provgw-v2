package com.rockville.wariddn.provgwv2.service.root;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockville.wariddn.provgwv2.dto.CallFilterDto;
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
public class ParentBlockUnblock extends Operation {

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
                // TODO: reply according to subscriber status
                //&& (vnd.getStatus()==1 || vnd.getStatus()==2 || vnd.getStatus()==3)) {
                IncomingSMS sms = objectMapper.readValue(request.getOriginalRequest(), IncomingSMS.class);
                List<String> msg = Arrays.asList(new String(sms.getMessageBody()).split("\\s+"));
                log.info("SMS" + msg);
                if (msg.size() > 2) {
                    if (Pattern.compile("^((\\+|\\+92|92|0|)(3\\d{9}\\b))+$").matcher(msg.get(2)).matches() == true) {
                        String memberMSISDN = Operation.normalizeMsisdn(msg.get(2));
                        log.info("MemberMSISDN = " + memberMSISDN);
                        //msg.add(2, Operation.normalizeMsisdn(msg.get(2)));
                        // msg.add(2, Operation.normalizeMsisdn(memberMSISDN));
                        List<CallFilterDto> callFilters = jdbcTemplate.query(constants.getQRY_TEMPLATE().get("QRY_GET_CALL_FILTER")
                                //        getQRY_GET_CALL_FILTER()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{MEMNUM}", memberMSISDN),
                                new BeanPropertyRowMapper<>(CallFilterDto.class)
                        );

                        // subscriber sent number to block/unblock call/msg
                        log.info("User aksed to " + request.getCommand() + " " + memberMSISDN);
                        Boolean execQuery = false;
                        switch (request.getCommand()) {
                            case "BLOCK":
                                log.info("[" + request.getTransactionId() + "] Subscriber want to Block:" + msg.get(1) + " " + memberMSISDN);
                                //TODO : Member Number is  already in call filter | check if sms already blocked and block command is not for sms but for call (vice versa)
                                //if member number is not already then simply block 

                                if (!callFilters.isEmpty() && memberMSISDN.equals(callFilters.get(0).getMemberMSISDN())) {
                                    //already present
                                    if ("CALL".equalsIgnoreCase(msg.get(1))) {
                                        //check if call already blocked
                                        log.info("Call Filter : {}", callFilters.get(0));
                                        if ("0".equalsIgnoreCase(callFilters.get(0).getCallStatus())) {
                                            log.info("User has already blocked all calls");
                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_ALREADY_BLACK_LISTED_" + request.getLanguage());
                                            break;
                                        } else {
                                            log.info("Not already black listed");
                                            qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_UPDATE_CALL_FILTER")
                                                    //.getQRY_UPDATE_CALL_FILTER()
                                                    .replace("{CALLSTATUS}", "" + 0)
                                                    .replace("{SMSSTATUS}", callFilters.get(0).getSMSStatus())
                                                    .replace("{MEMNUM}", memberMSISDN)
                                                    .replace("{TNUM}", request.getMsisdn());
                                            execQuery = true;
                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_ADDED_BLACK_LIST_" + request.getLanguage());
                                            break;
                                        }
                                    } else if ("SMS".equalsIgnoreCase(msg.get(1))) {
                                        log.info("SMS Filter " + callFilters.get(0).getSMSStatus());

                                        if ("0".equalsIgnoreCase(callFilters.get(0).getSMSStatus())) {
                                            log.info("User has already blocked all sms");
                                            //SMS::  FILTER ALREADY APPLIED
                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_ALREADY_BLACK_LISTED_" + request.getLanguage());
                                            break;
                                        } else {
                                            qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_UPDATE_CALL_FILTER")
                                                    //.getQRY_UPDATE_CALL_FILTER()
                                                    .replace("{CALLSTATUS}", callFilters.get(0).getCallStatus())
                                                    .replace("{SMSSTATUS}", "" + 0)
                                                    .replace("{MEMNUM}", memberMSISDN)
                                                    .replace("{TNUM}", request.getMsisdn());
                                            execQuery = true;
                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_ADDED_BLACK_LIST_" + request.getLanguage());
                                            break;
                                        }
                                    }

                                } else {
                                    if ("CALL".equalsIgnoreCase(msg.get(1))) {
                                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SET_CALL_FILTER")
                                                //getQRY_SET_CALL_FILTER()
                                                .replace("{VNUM}", "" + vnd.getVirtualMSISDN())
                                                .replace("{TNUM}", request.getMsisdn())
                                                .replace("{MEMNUM}", memberMSISDN)
                                                .replace("{CALLSTATUS}", "0")
                                                .replace("{SMSSTATUS}", "1")
                                                .replace("{STATUS}", "1");
                                        execQuery = true;

                                    } else if ("SMS".equalsIgnoreCase(msg.get(1))) {
                                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SET_CALL_FILTER")
                                                //.getQRY_SET_CALL_FILTER()
                                                .replace("{VNUM}", "" + vnd.getVirtualMSISDN())
                                                .replace("{TNUM}", request.getMsisdn())
                                                .replace("{MEMNUM}", memberMSISDN)
                                                .replace("{CALLSTATUS}", "1")
                                                .replace("{SMSSTATUS}", "0")
                                                .replace("{STATUS}", "1");
                                        execQuery = true;
                                    }
                                    //" VALUES({VNUM},{TNUM},{MEMNUM},{CALLSTATUS},{SMSSTATUS},{STATUS},CURDATE()"
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_ADDED_BLACK_LIST_" + request.getLanguage());
                                    break;
                                }
                            case "UNBLOCK":
                                log.info("[" + request.getTransactionId() + "] Subscriber want to UnBlock:" + msg.get(1) + " " + memberMSISDN);
                                //TODO : UnBlock all the numbers

                                if (!callFilters.isEmpty() && memberMSISDN.equals(callFilters.get(0).getMemberMSISDN())) {
                                    //Found filter with given telco 
                                    if ("CALL".equalsIgnoreCase(msg.get(1))) {
                                        //check if call already blocked
                                        if ("1".equalsIgnoreCase(callFilters.get(0).getCallStatus())) {
                                            //SMS :: FILTER ALREADY UNBLOCKED
                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_NOT_IN_BLACK_LIST_" + request.getLanguage());
                                            break;
                                        } else {
                                            qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_UPDATE_CALL_FILTER")
                                                    //.getQRY_UPDATE_CALL_FILTER()
                                                    .replace("{CALLSTATUS}", "" + 1)
                                                    .replace("{SMSSTATUS}", callFilters.get(0).getSMSStatus())
                                                    .replace("{TNUM}", request.getMsisdn())
                                                    .replace("{MEMNUM}", memberMSISDN);
                                            execQuery = true;

                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_REMOVED_BLACK_LIST_" + request.getLanguage());
                                            break;
                                        }
                                    } //cjeck if call already blocked
                                    else if ("SMS".equalsIgnoreCase(msg.get(1))) {
                                        if ("1".equalsIgnoreCase(callFilters.get(0).getSMSStatus())) {
                                            //SMS::  FILTER ALREADY APPLIED
                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_NOT_IN_BLACK_LIST_" + request.getLanguage());
                                            break;
                                        } else {
                                            qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_UPDATE_CALL_FILTER")
                                                    //.getQRY_UPDATE_CALL_FILTER()
                                                    .replace("{CALLSTATUS}", callFilters.get(0).getCallStatus())
                                                    .replace("{SMSSTATUS}", "" + 1)
                                                    .replace("{TNUM}", request.getMsisdn())
                                                    .replace("{MEMNUM}", memberMSISDN);
                                            execQuery = true;
                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_REMOVED_BLACK_LIST_" + request.getLanguage());
                                            break;
                                        }
                                    }
                                } else {
                                    //DON't do anything user hasn't blocked any number
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_NOT_IN_BLACK_LIST_" + request.getLanguage());
                                    break;
                                }
                                break;
                        }

                        if (execQuery) {
                            log.info("Query : {}", qry_after_param_replacement);
                            int res = jdbcTemplate.update(qry_after_param_replacement);
                            if (res > 0) {
                                // NO white list maintained,inform customer accordingly
                                commonOperations.writeActivityLog(request, "BlockUnblock", "SUCCESS");

                            } else {
                                // send white list
                                commonOperations.writeActivityLog(request, "BlockUnblock", "FAILED");
                                sms_string_after_replacing_params
                                        = constants.getMESSAGE_TEMPLATE().get("MSG_NOT_IN_BLACK_LIST_" + request.getLanguage());
                            }
                        }
                    } else if ("ALL".equalsIgnoreCase(msg.get(2))) {
                        log.info("User aksed to " + request.getCommand() + "  all .");
                        List<CallFilterDto> callFilters = jdbcTemplate.query(constants.getQRY_TEMPLATE().get("QRY_GET_CALL_FILTER")
                                        //getQRY_GET_CALL_FILTER()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{MEMNUM}", "*"),
                                new BeanPropertyRowMapper<>(CallFilterDto.class)
                        );
                        //  subscriber sent ALL to block/unblock call/msg
                        Boolean execQuery = false;
                        switch (request.getCommand()) {
                            case "BLOCK":
                                log.info("[" + request.getTransactionId() + "] Subscriber want to Block:" + msg.get(1) + " " + msg.get(2));
                                //TODO : Block all the numbers
                                //previous query
//                                qry_after_param_replacement = Constants.QRY_SET_BLOCK
//                                        .replace("{TNUM}", request.getMsisdn())
//                                        .replace("{BNUM}", msg.get(1));

                                //check if user has already applied call filter? 
                                //if yes update otherwise insert
                                if (!callFilters.isEmpty() && "*".equals(callFilters.get(0).getMemberMSISDN())) {
                                    //Found filter with given telco 
                                    if ("CALL".equalsIgnoreCase(msg.get(1))) {
                                        //check if call already blocked
                                        if ("0".equalsIgnoreCase(callFilters.get(0).getCallStatus())) {
                                            //SMS :: FILTER ALREADY APPLIED
                                            log.info("User has already blocked all calls");
                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_ALREADY_BLACK_LISTED_" + request.getLanguage());
                                            break;
                                        } else {
                                            log.info("Not already black listed");
                                            qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_UPDATE_CALL_FILTER")
                                                    //.getQRY_UPDATE_CALL_FILTER()
                                                    .replace("{CALLSTATUS}", "" + 0)
                                                    .replace("{SMSSTATUS}", callFilters.get(0).getSMSStatus())
                                                    .replace("{MEMNUM}", "*")
                                                    .replace("{TNUM}", request.getMsisdn());
                                            execQuery = true;
                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_ADDED_BLACK_LIST_" + request.getLanguage());
                                            break;
                                        }
                                    } //cjeck if call already blocked
                                    else if ("SMS".equalsIgnoreCase(msg.get(1))) {
                                        if ("0".equalsIgnoreCase(callFilters.get(0).getSMSStatus())) {
                                            log.info("User has already blocked all sms");
                                            //SMS::  FILTER ALREADY APPLIED
                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_ALREADY_BLACK_LISTED_" + request.getLanguage());
                                            break;
                                        } else {
                                            qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_UPDATE_CALL_FILTER")
                                                    //.getQRY_UPDATE_CALL_FILTER()
                                                    .replace("{CALLSTATUS}", callFilters.get(0).getCallStatus())
                                                    .replace("{SMSSTATUS}", "" + 0)
                                                    .replace("{MEMNUM}", "*")
                                                    .replace("{TNUM}", request.getMsisdn());
                                            execQuery = true;
                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_ADDED_BLACK_LIST_" + request.getLanguage());
                                            break;
                                        }
                                    }
                                } else {
                                    if ("CALL".equalsIgnoreCase(msg.get(1))) {
                                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SET_CALL_FILTER")
                                                //.getQRY_SET_CALL_FILTER()
                                                .replace("{VNUM}", "" + vnd.getVirtualMSISDN())
                                                .replace("{TNUM}", request.getMsisdn())
                                                .replace("{MEMNUM}", "*")
                                                .replace("{CALLSTATUS}", "0")
                                                .replace("{SMSSTATUS}", "1")
                                                .replace("{STATUS}", "1");
                                        execQuery = true;

                                    } else if ("SMS".equalsIgnoreCase(msg.get(1))) {
                                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SET_CALL_FILTER")
                                                //.getQRY_SET_CALL_FILTER()
                                                .replace("{VNUM}", "" + vnd.getVirtualMSISDN())
                                                .replace("{TNUM}", request.getMsisdn())
                                                .replace("{MEMNUM}", "*")
                                                .replace("{CALLSTATUS}", "1")
                                                .replace("{SMSSTATUS}", "0")
                                                .replace("{STATUS}", "1");
                                        execQuery = true;

                                    }
                                    //" VALUES({VNUM},{TNUM},{MEMNUM},{CALLSTATUS},{SMSSTATUS},{STATUS},CURDATE()"
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_ADDED_BLACK_LIST_" + request.getLanguage());
                                    break;
                                }
//                                sms_string_after_replacing_params
//                                        = Constants.MESSAGE_TEMPLATE.get("MSG_ADDED_BLACK_LIST_" + request.getLanguage());
//                                break;
                            case "UNBLOCK":
                                log.info("[" + request.getTransactionId() + "] Subscriber want to UnBlock:" + msg.get(1) + " " + msg.get(2));
                                //TODO : UnBlock all the numbers

                                if (!callFilters.isEmpty() && "*".equals(callFilters.get(0).getMemberMSISDN())) {
                                    //Found filter with given telco 
                                    if ("CALL".equalsIgnoreCase(msg.get(1))) {
                                        //check if call already blocked
                                        if ("1".equalsIgnoreCase(callFilters.get(0).getCallStatus())) {
                                            //SMS :: FILTER ALREADY UNBLOCKED
                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_NOT_IN_BLACK_LIST_" + request.getLanguage());
                                            break;
                                        } else {
                                            qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_UPDATE_CALL_FILTER")
                                                    //.getQRY_UPDATE_CALL_FILTER()
                                                    .replace("{CALLSTATUS}", "" + 1)
                                                    .replace("{SMSSTATUS}", callFilters.get(0).getSMSStatus())
                                                    .replace("{TNUM}", request.getMsisdn())
                                                    .replace("{MEMNUM}", "*");
                                            execQuery = true;

                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_REMOVED_BLACK_LIST_" + request.getLanguage());
                                            break;
                                        }
                                    } //cjeck if call already blocked
                                    else if ("SMS".equalsIgnoreCase(msg.get(1))) {
                                        if ("1".equalsIgnoreCase(callFilters.get(0).getSMSStatus())) {
                                            //SMS::  FILTER ALREADY APPLIED
                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_NOT_IN_BLACK_LIST_" + request.getLanguage());
                                            break;
                                        } else {
                                            qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_UPDATE_CALL_FILTER")
                                                    //.getQRY_UPDATE_CALL_FILTER()
                                                    .replace("{CALLSTATUS}", callFilters.get(0).getCallStatus())
                                                    .replace("{SMSSTATUS}", "" + 1)
                                                    .replace("{TNUM}", request.getMsisdn())
                                                    .replace("{MEMNUM}", "*");
                                            execQuery = true;

                                            sms_string_after_replacing_params
                                                    = constants.getMESSAGE_TEMPLATE().get("MSG_REMOVED_BLACK_LIST_" + request.getLanguage());
                                            break;
                                        }
                                    }
                                } else {
                                    //DON't do anything user hasn't blocked any number
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_NOT_IN_BLACK_LIST_" + request.getLanguage());
                                    break;
                                }

//                                qry_after_param_replacement = Constants.QRY_SET_UNBLOCK
//                                        .replace("{TNUM}", request.getMsisdn())
//                                        .replace("{BNUM}", msg.get(1));
//                                sms_string_after_replacing_params
//                                        = Constants.MESSAGE_TEMPLATE.get("MSG_REMOVED_BLACK_LIST_" + request.getLanguage());
                                break;
                        }
                        if (execQuery) {
                            log.info("Query : " + qry_after_param_replacement);
                            int res = jdbcTemplate.update(qry_after_param_replacement);
                            if (res > 0) {
                                // NO white list maintained,inform customer accordingly
                                commonOperations.writeActivityLog(request, "BlockUnblock", "SUCCESS");

                            } else {
                                // send white list
                                commonOperations.writeActivityLog(request, "BlockUnblock", "FAILED");
                                sms_string_after_replacing_params
                                        = constants.getMESSAGE_TEMPLATE().get("MSG_NOT_IN_BLACK_LIST_" + request.getLanguage());
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
