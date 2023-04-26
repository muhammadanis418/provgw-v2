package com.rockville.wariddn.provgwv2.service.root;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockville.wariddn.provgwv2.dto.IncomingSMS;
import com.rockville.wariddn.provgwv2.dto.Request;
import com.rockville.wariddn.provgwv2.dto.User;
import com.rockville.wariddn.provgwv2.dto.VnDetails;
import com.rockville.wariddn.provgwv2.service.Operation;
import com.rockville.wariddn.provgwv2.service.SmsSendingService;
import com.rockville.wariddn.provgwv2.util.CommonOperations;
import com.rockville.wariddn.provgwv2.util.Constants;
import com.rockville.wariddn.provgwv2.util.HttpOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParentSig extends Operation {
    private final Constants constants;
    private final CommonOperations commonOperations;
    private final SmsSendingService smsSendingService;
    private final ObjectMapper objectMapper;
    private final HttpOperation httpOperation;


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
                String msg = new String(sms.getMessageBody()).substring(3).trim();
                if (!msg.isEmpty()) {
                    log.info("[" + request.getTransactionId() + "] Subscriber want set Signature:" + msg);
                    try {
                        MultiValueMap<String, String> getParams = new LinkedMultiValueMap<>();
                        getParams.add("msisdn", request.getMsisdn());
                        String jsonResponse = httpOperation.sendGet(constants.getGET_USER_DETAILS_API(), getParams);
                        log.info("Get User Response: " + jsonResponse);

                        User user = objectMapper.readValue(jsonResponse, User.class);
                        user.setSmsSignature(msg);

                        MultiValueMap<String, String> updateParams = new LinkedMultiValueMap<String, String>();
                        updateParams.add("msisdn", request.getMsisdn());
                        updateParams.add("firstName", user.getFirstName());
                        updateParams.add("lastName", user.getLastName());
                        updateParams.add("dob", user.getDob());
                        updateParams.add("profession", user.getProfession());
                        updateParams.add("province", user.getProvince());
                        updateParams.add("city", user.getCity());
                        updateParams.add("smsSignature", user.getSmsSignature());
                        updateParams.add("myStatus", user.getMyStatus());
                        jsonResponse = httpOperation.sendGet(constants.getUPDATE_USER_DETAILS_API(), updateParams);

                        log.info("Update User Response: " + jsonResponse);

                        commonOperations.writeActivityLog(request, "Signature", "SUCCESS");
                        sms_string_after_replacing_params
                                = constants.getMESSAGE_TEMPLATE().get("MSG_SIG_SET_" + request.getLanguage());

                    } catch (Exception ex) {
                        log.error("Exception: " + ex, ex);
                        // send white list
                        commonOperations.writeActivityLog(request, "Signature", "HTTPFailure");
                        log.error("[" + request.getTransactionId() + "] Set Sig. HTTP Failure");
                    }
                } else {
                    log.error("[" + request.getTransactionId() + "] Invalid Command");
                    commonOperations.writeActivityLog(request, "Signature", "Invalid Command");
                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_INVALID_CMD_SIG_HELP_" + request.getLanguage());
                }

            } else {
                log.error("[" + request.getTransactionId() + "] Not A Subscriber");
                commonOperations.writeActivityLog(request, "Signature", "NonSub");
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
