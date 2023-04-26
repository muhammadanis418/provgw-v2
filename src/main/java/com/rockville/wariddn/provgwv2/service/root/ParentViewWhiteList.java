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
public class ParentViewWhiteList extends Operation {

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

                log.info("[" + request.getTransactionId() + "] Subscriber want to view his White List");
                qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_VIEW_WHITELIST")
                        //getQRY_VIEW_WHITELIST()
                        .replace("{TNUM}", request.getMsisdn());

                // TODO: fetch white list from DB
                List<String> res = jdbcTemplate.queryForList(qry_after_param_replacement, String.class);
                if (res.isEmpty()) {
                    // NO white list maintained,inform customer accordingly
                    commonOperations.writeActivityLog(request, "ViewWhiteList", "No List maintained");
                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_NO_WHITE_LIST_" + request.getLanguage());
                } else {
                    // send white list
                    String white_list = "";
                    for (String wl : res) {
                        white_list += wl + "\n";
                    }
                    commonOperations.writeActivityLog(request, "ViewWhiteList", "list sent as SMS");
                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_VIEW_WHITE_LIST_" + request.getLanguage()).replace("{LIST}", "\n" + white_list);
                }

            } else {
                log.error("[" + request.getTransactionId() + "] Not A Subscriber");
                commonOperations.writeActivityLog(request, "ViewWhiteList", "NonSub");
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
