package com.rockville.wariddn.provgwv2.service.root;

import com.rockville.wariddn.provgwv2.dto.Request;
import com.rockville.wariddn.provgwv2.dto.VnDetails;
import com.rockville.wariddn.provgwv2.enums.VnStatus;
import com.rockville.wariddn.provgwv2.service.Operation;
import com.rockville.wariddn.provgwv2.service.SmsSendingService;
import com.rockville.wariddn.provgwv2.util.CommonOperations;
import com.rockville.wariddn.provgwv2.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParentOff extends Operation {

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

            // TODO: Check Status in NumberMapping
            VnDetails vnd = commonOperations.fetchSubscriptionDetailsByTelcoNum(request.getMsisdn());
            if (vnd != null) {
                log.info(vnd.toString());
            }

            if (vnd == null || vnd.getVirtualMSISDN() <= 0 || vnd.getStatus() < 1 || vnd.getStatus() >= 4) {
                //no record found
                commonOperations.writeActivityLog(request, "OFF", "NonSub");
                log.error("[" + request.getTransactionId() + "] Switch OFF. Not a Subscriber");

                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_NOT_SUB_" + request.getLanguage());
            } else if (vnd.getStatus() == VnStatus.POWERED_OFF.ordinal()) {
                //Already off
                commonOperations.writeActivityLog(request, "OFF", "AlreadyOff");
                //request.setLanguage(res.get(0)[1]);
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_ALREADY_OFF_" + request.getLanguage());
            } else { //OFF
//                if (constants.getIS_MEMCACHE_ENABLED) {
//                    log.debug("[" + request.getTransactionId() + "] DelMemCache");
//                    MemcacheClient.getInstance().client.delete(request.getMsisdn());
//                }
                // ADD TO LOCAL DB
                if (jdbcTemplate.update(constants.getOFF_SUCCESS_QUERY().replace("{MSISDN}", request.getMsisdn())) < 1) {
                    log.error("[" + request.getTransactionId() + "] Switch OFF DB Failure");
                    commonOperations.writeActivityLog(request, "OFF", "LocalDBFailure");
                } else {
                    String query = constants.getQRY_TEMPLATE().get("QRY_INSERT_ON_VN_OFF_FOR_ALERT")
                            //getQRY_INSERT_ON_VN_OFF_FOR_ALERT()
                            .replace("MSISDN", request.getMsisdn());

                    log.info("QRY_INSERT_ON_VN_OFF_FOR_ALERT: {}", query);
                    jdbcTemplate.update(query);
                    commonOperations.writeActivityLog(request, "OFF", "SUCCESS");
                    log.debug("[" + request.getTransactionId() + "] Switch OFF updated in DB");
                }

                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_OFF_SUCCESS_" + request.getLanguage());
            }

            smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                    sms_string_after_replacing_params,
                    request.getTransactionId(), request.getSendSms());
        } catch (Exception ex) {
            log.error("Exception: " + ex);
        }
    }
}
