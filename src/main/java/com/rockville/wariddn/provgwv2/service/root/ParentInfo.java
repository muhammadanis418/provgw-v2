package com.rockville.wariddn.provgwv2.service.root;

import com.rockville.wariddn.provgwv2.dto.Request;
import com.rockville.wariddn.provgwv2.dto.RuleCollectionDto;
import com.rockville.wariddn.provgwv2.dto.VnDetails;
import com.rockville.wariddn.provgwv2.enums.VnStatus;
import com.rockville.wariddn.provgwv2.service.Operation;
import com.rockville.wariddn.provgwv2.service.SmsSendingService;
import com.rockville.wariddn.provgwv2.util.CommonOperations;
import com.rockville.wariddn.provgwv2.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParentInfo extends Operation {

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

            VnDetails vnd = commonOperations.fetchSubscriptionDetailsByTelcoNum(request.getMsisdn());
            if (vnd != null) {
                log.info(vnd.toString());
            }

            if (vnd != null && vnd.getVirtualMSISDN() > 0 && vnd.getTelcoMSISDN() > 0) {
                // TODO: reply according to subscriber status
                //&& (vnd.getStatus()==1 || vnd.getStatus()==2 || vnd.getStatus()==3)) {
                log.error("[" + request.getTransactionId() + "] Subscriber called for INFO");
                commonOperations.writeActivityLog(request, "INFO", "Sub");

                List<RuleCollectionDto> ruleCollection = jdbcTemplate.query(
                        constants.getQRY_TEMPLATE().get("QRY_FETCH_RULE_COLLECTION_FOR_INFO_HANDLER")
                                //getQRY_FETCH_RULE_COLLECTION_FOR_INFO_HANDLER()
                                .replace("{telcoMsisdn}", request.getMsisdn()),
                        new BeanPropertyRowMapper<>(RuleCollectionDto.class)
                );
                String hrFilter = "";
                if (!ruleCollection.isEmpty()) {
                    hrFilter = "from :" + ruleCollection.get(0).getStartTimeOfDay() + " to : " + ruleCollection.get(0).getEndTimeOfDay();
                } else {
                    hrFilter = "N/A";
                }
                int status = vnd.getStatus();
                if (vnd.getTelcoType() == 1 || vnd.getTelcoType() == 3) // 1 means Waridpost 3 means jazzpost 
                {
                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_INFO_POSTPAID_" + request.getLanguage());
                    sms_string_after_replacing_params = sms_string_after_replacing_params
                            .replace("{VN}", "" + vnd.getVirtualMSISDN())
                            .replace("{EXPIRY_DATE}", "" + vnd.getBilledUpto())
                            .replace("{DN_STATUS}", "" + VnStatus.values()[status])
                            .replace("{HR_FILTER}", "" + hrFilter);
                } else // else user is prepaid 
                {
                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_INFO_" + request.getLanguage());
                    sms_string_after_replacing_params = sms_string_after_replacing_params
                            .replace("{VN}", "" + vnd.getVirtualMSISDN())
                            .replace("{EXPIRY_DATE}", "" + vnd.getBilledUpto())
                            .replace("{DN_STATUS}", "" + VnStatus.values()[status])
                            .replace("{HR_FILTER}", "" + hrFilter);
                }
                smsSendingService.sendSms(
                        constants.getSHORT_CODE(), request.getMsisdn(),
                        sms_string_after_replacing_params,
                        request.getTransactionId(), request.getSendSms());
            } else {
                log.error("[" + request.getTransactionId() + "] Not A Subscriber");
                commonOperations.writeActivityLog(request, "INFO", "NonSub");
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get(" MSG_NOT_SUB_" + request.getLanguage());
                smsSendingService.sendSms(
                        constants.getSHORT_CODE(), request.getMsisdn(),
                        sms_string_after_replacing_params,
                        request.getTransactionId(), request.getSendSms());
            }
        } catch (Exception ex) {
            log.error("Exception: " + ex, ex);
        }
    }
}
