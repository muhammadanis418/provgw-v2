package com.rockville.wariddn.provgwv2.service.root;

import com.rockville.wariddn.provgwv2.dto.Request;
import com.rockville.wariddn.provgwv2.dto.SubAdditionalParameters;
import com.rockville.wariddn.provgwv2.dto.VnDetails;
import com.rockville.wariddn.provgwv2.enums.ResponseEnum;
import com.rockville.wariddn.provgwv2.enums.VnStatus;
import com.rockville.wariddn.provgwv2.rest.RestClientService;
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
public class ParentSubRaw extends Operation {

    private final Constants constants;
    private final CommonOperations commonOperations;
    private final SmsSendingService smsSendingService;
    private final RestClientService restClientService;
    private final JdbcTemplate jdbcTemplate;
    private final ParentSubAllNewImpl parentSubAllNewImpl;

    @Override
    public void run(Request request) {
        try {
            log.info("Request : {}", request);
            if (request.getMsisdn() == null) {
                log.error("Request MUST be originated by a valid MSISDN ");
                return;
            }
            request.setMsisdn(Operation.normalizeMsisdn(request.getMsisdn()));

            request = validateLanguage(request);

            String sms_string_after_replacing_params = "";

            VnDetails vn = commonOperations.fetchSubscriptionDetailsByTelcoNum(request.getMsisdn());
            if (vn != null) {
                log.info(vn.toString());
            }

            // Handle subscriber that is already in VN Database
            String user_type = restClientService.getUserType(request.getMsisdn()).toUpperCase();
            log.info("Msisnd : {} User Type : {}", request.getMsisdn(), user_type);
            // Check if subscription against this user (operator_productType) is allowed or NOT
            if (constants.getALLOWED_SUBSCRIPTION_BY_TYPE().get("ALLOWED_TYPE_" + user_type) != null && constants.getALLOWED_SUBSCRIPTION_BY_TYPE().get("ALLOWED_TYPE_" + user_type)) {
                // Check if we can give this user GOLDEN DN
                if (constants.getALLOWED_SUBSCRIPTION_BY_TYPE().get("ALLOWED_TYPE_GOLDEN_" + user_type) != null && constants.getALLOWED_SUBSCRIPTION_BY_TYPE().get("ALLOWED_TYPE_GOLDEN_" + user_type)) {
                    // YES, give NORMAL/GOLDEN selection

                    // Check if customer has already sent SUB before
                    List<String> res = jdbcTemplate.queryForList(constants.getQRY_TEMPLATE().get("QRY_FETCH_SUB_REQUEST")
                            //.getQRY_FETCH_SUB_REQUEST()
                            .replace("{MSISDN}", request.getMsisdn()), String.class);
                    log.info("Result got from Sub Request Query" + res);
                    if (!res.isEmpty() && res.size() > 0 && res.get(0).equalsIgnoreCase("1")) {
                        //already sent numbers for subscription
                        commonOperations.writeActivityLog(request, "HandleRawSub:", ResponseEnum.ALREADY_SENT_GOLDEN_NUMBERS.name());

                        List<String> vn_list = jdbcTemplate.queryForList(constants.getQRY_TEMPLATE().get("QRY_GET_RESERVE_DOUBLE_NUMBERS")
                                        //.getQRY_GET_RESERVE_DOUBLE_NUMBERS()
                                .replace("{STATUS}", "" + VnStatus.RESERVED_FOR_OPT_IN.ordinal())
                                .replace("{MSISDN}", request.getMsisdn()),
                                String.class
                        );
                        log.info("Fetched VN's: " + vn_list.size());
                        int count = 0;
                        String option_list = "";

                        for (String vne : vn_list) {
                            count++;
                            // create option list
                            option_list += count + "- " + vne + "\n";
                        }
                        sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_Already_SENT_SUB_GOLDEN_OPTION_LIST_" + request.getLanguage())
                                .replace("{OPTIONS}", "1-" + count)
                                .replace("{DNLIST}", "\n" + option_list);

                        smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                                sms_string_after_replacing_params,
                                request.getTransactionId(), request.getSendSms());
                        return;
                    }

                    if (vn != null && vn.getVirtualMSISDN() > 0) {
                        // RETURNING subscriber, activate existing DN
                        log.info("Existing Subscriber: " + vn.getVirtualMSISDN() + " | " + vn.getTelcoMSISDN() + " ... invoking ParentSubAllNewImpl");

                        SubAdditionalParameters subparams = null;
                        if (vn.getTypeOfNum() == 9) {
                            subparams = new SubAdditionalParameters(request.getSendSms(), "SUB", false, true, false);
                        } else {
                            subparams = new SubAdditionalParameters(request.getSendSms(), "SUB", false, false, false);
                        }
//                        commented out by nouman
//                        SubAdditionalParameters subparams = new SubAdditionalParameters(sendSms, sendAkkaReply, "SUB", false, false, false);
                        subparams.setNewSubscriber(false); // NOT required ???? as default value is false
//                        ParentSubAllNewImpl subNew = new ParentSubAllNewImpl(request, senderRef, receiverRef, subparams);
                        request.setSubAdditionalParameters(subparams);
                        parentSubAllNewImpl.run(request);
                        return;

                    } else {
                        // NEW subscriber, give NORMAL/GOLDEN selection
                        log.error("[" + request.getTransactionId() + "] Not A Subscriber");
                        commonOperations.writeActivityLog(request, "SUB", "NonSub");
                        // Save the user SUB request to be used in later replies
                        jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_ADD_UPDATE_SUB_REQUEST")
                               // .getQRY_ADD_UPDATE_SUB_REQUEST()
                                .replace("{MSISDN}", request.getMsisdn())
                                .replace("{STATUS}", "0"));

                        sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_SUB_RAW_" + request.getLanguage());
                    }

                } else {
                    // NO, directly subscribe to Normal DN
                    SubAdditionalParameters subparams = new SubAdditionalParameters(request.getSendSms(), "SUB", false, false, false);
                    subparams.setNewSubscriber(false); // NOT required ???? as default value is false
//                    ParentSubAllNewImpl subNew = new ParentSubAllNewImpl(request, senderRef, receiverRef, subparams);
                    request.setSubAdditionalParameters(subparams);
                    parentSubAllNewImpl.run(request);
                    return;
                }
            } else {
                // SUB NOT ALLOWED
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_FAILURE_GENERAL_" + request.getLanguage());
                request.setDescription(user_type);
                commonOperations.writeActivityLog(request, "SUB", "FAILURE");
            }
            smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                    sms_string_after_replacing_params,
                    request.getTransactionId(), request.getSendSms());
        } catch (Exception ex) {
            log.error("Exception: " + ex, ex);
        }
    }
}
