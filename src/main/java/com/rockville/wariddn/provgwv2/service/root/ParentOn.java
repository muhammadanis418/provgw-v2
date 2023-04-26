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
public class ParentOn extends Operation {

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

            //List<String[]> res = DBManager.getInstance().Fetch(Constants.DB_OPERATOR, Constants.DB_SERVICE, Constants.GET_STATUS_LANGUAGE.replace("{MSISDN}", request.getMsisdn()));
            // TODO: Check Status in NumberMapping
            VnDetails vnd = commonOperations.fetchSubscriptionDetailsByTelcoNum(request.getMsisdn());
            if (vnd != null) {
                log.info(vnd.toString());
            }

            if (vnd == null || vnd.getVirtualMSISDN() <= 0 || vnd.getStatus() < 1 || vnd.getStatus() >= 4) {
                // no record found
                log.debug("[" + request.getTransactionId() + "] Switch ON. Not a Subscriber");

                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_NOT_SUB_" + request.getLanguage());

                commonOperations.writeActivityLog(request, "ON", "NonSub");

            } else if (vnd.getStatus() == VnStatus.ACTIVE.ordinal()) {
                // ALREADY ACTIVE
                log.debug("[" + request.getTransactionId() + "] Switch ON. Already ON");
                //request.setLanguage(res.get(0)[1]);
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_ALREADY_ON_" + request.getLanguage());

                commonOperations.writeActivityLog(request, "ON", "AlreadyON");

            } else if (vnd.getStatus() == VnStatus.POWERED_OFF.ordinal()) {
                // DN is OFF
//                if (Constants.IS_MEMCACHE_ENABLED) {
//                    log.debug("[" + request.getTransactionId() + "] DelMemCache");
//                    MemcacheClient.getInstance().client.delete(request.getMsisdn());
//                }

                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_ON_SUCCESS_" + request.getLanguage());

                if (jdbcTemplate.update(constants.getON_SUCCESS_QUERY().replace("{MSISDN}", request.getMsisdn())) < 1) {
                    commonOperations.writeActivityLog(request, "ON", "LocalDBFailure");
                    log.error("[" + request.getTransactionId() + "] Switch ON. DB Failure");
                } else {
                    String query = constants.getQRY_TEMPLATE().get("QRY_UPDATE_VN_ON_ALERT")
                            //getQRY_UPDATE_VN_ON_ALERT()
                            .replace("MSISDN", request.getMsisdn());
                    jdbcTemplate.update(query);
                    commonOperations.writeActivityLog(request, "ON", "SUCCESS");
                    log.debug("[" + request.getTransactionId() + "] Switch ON. updated in DB");
                }
            } else {
                // ALL other status
            }

            smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                    sms_string_after_replacing_params,
                    request.getTransactionId(), request.getSendSms());

        } catch (Exception ex) {
            log.error("Exception: " + ex);
        }
    }
}
