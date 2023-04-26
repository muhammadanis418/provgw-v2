package com.rockville.wariddn.provgwv2.service.root;

import com.rockville.wariddn.provgwv2.dto.Request;
import com.rockville.wariddn.provgwv2.dto.VnDetails;
import com.rockville.wariddn.provgwv2.service.Operation;
import com.rockville.wariddn.provgwv2.service.SmsSendingService;
import com.rockville.wariddn.provgwv2.util.CommonOperations;
import com.rockville.wariddn.provgwv2.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParentMeeting extends Operation {
    private final Constants constants;
    private final CommonOperations commonOperations;
    private final SmsSendingService smsSendingService;
    private final JdbcTemplate jdbcTemplate;

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
                String pin = generatePinCode();
                log.info("[" + request.getTransactionId() + "] Subscriber want initiate Conference Call");
                qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_GEN_CONFERENCE_CALL")
                        //getQRY_GEN_CONFERENCE_CALL()
                        .replace("{TNUM}", request.getMsisdn())
                        .replace("{PIN}", pin)
                        .replace("{ORG}", request.getService());

                int res = jdbcTemplate.update(qry_after_param_replacement);
                if (res > 0) {
                    sms_string_after_replacing_params
                            = constants.getMESSAGE_TEMPLATE().get("MSG_CONFERENCE_CALL_ADDED_" + request.getLanguage()).replace("{PIN}", pin);
                    commonOperations.writeActivityLog(request, "Meeting", "SUCCESS");
                } else {
                    commonOperations.writeActivityLog(request, "Meeting", "FAILED");
                }
            } else {
                log.error("[" + request.getTransactionId() + "] Not A Subscriber");
                commonOperations.writeActivityLog(request, "Meeting", "NonSub");
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_HELP_NOT_SUB_" + request.getLanguage());
            }
            
            smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                    sms_string_after_replacing_params,
                    request.getTransactionId(), request.getSendSms());
        } catch (Exception ex) {
            log.error("Exception: " + ex, ex);
        }
    }

    public String generatePinCode() {
        Random r = new Random(System.currentTimeMillis());
        return (1 + r.nextInt(2)) * 10000 + r.nextInt(10000) + "";
    }
}
