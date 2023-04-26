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

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParentViewBlackList extends Operation {

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

                log.info("[" + request.getTransactionId() + "] Subscriber want to view his Black List");
                qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_VIEW_BLACKLIST")
                        //getQRY_VIEW_BLACKLIST()
                        .replace("{TNUM}", request.getMsisdn());

                // TODO: fetch black list from DB
                List<String> res = jdbcTemplate.queryForList(qry_after_param_replacement, String.class);
                if (res.isEmpty()) {
                    // NO black list maintained,inform customer accordingly
                    commonOperations.writeActivityLog(request, "ViewBlackList", "No List maintained");
                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_NO_BLACK_LIST_" + request.getLanguage());
                } else {
                    // send black list
                    String black_list = "";
                    for (String bl : res) {
                        black_list += bl + "\n";
                    }
                    commonOperations.writeActivityLog(request, "ViewBlackList", "list sent as SMS");
                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_VIEW_BLACK_LIST_" + request.getLanguage()).replace("{LIST}", "\n" + black_list);
                }

            } else {
                log.error("[" + request.getTransactionId() + "] Not A Subscriber");
                commonOperations.writeActivityLog(request, "ViewBlackList", "NonSub");
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
