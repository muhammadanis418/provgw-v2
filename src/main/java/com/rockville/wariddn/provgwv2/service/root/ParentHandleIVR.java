package com.rockville.wariddn.provgwv2.service.root;

import com.rockville.wariddn.provgwv2.dto.Request;
import com.rockville.wariddn.provgwv2.enums.ResponseEnum;
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
public class ParentHandleIVR extends Operation {

    private final Constants constants;
    private final CommonOperations commonOperations;
    private final SmsSendingService smsSendingService;
    private final JdbcTemplate jdbcTemplate;
    private final ParentHandleDigits parentHandleDigits;

    @Override
    public void run(Request request) {
        try {
            if (request.getMsisdn() == null) {
                log.error("Request MUST be originated by a valid MSISDN ");
                return;
            }
            request.setMsisdn(Operation.normalizeMsisdn(request.getMsisdn()));

            request = validateLanguage(request);

            log.info("Request : {}", request);

            String sms_string_after_replacing_params = "";
            List<String> res = jdbcTemplate.queryForList(constants.getQRY_TEMPLATE().get("QRY_FETCH_SUB_REQUEST")
                    //.getQRY_FETCH_SUB_REQUEST()
                    .replace("{MSISDN}", request.getMsisdn()), String.class);

            if (!res.isEmpty() && res.size() > 0 && res.get(0).equalsIgnoreCase("1")) {
                //already sent numbers for subscription
                commonOperations.writeActivityLog(request, "HandleRawSub:", ResponseEnum.ALREADY_SENT_GOLDEN_NUMBERS.name());

                List<String> vn_list = jdbcTemplate.query(constants.getQRY_TEMPLATE().get("QRY_GET_RESERVE_DOUBLE_NUMBERS")
                                //.getQRY_GET_RESERVE_DOUBLE_NUMBERS()
                        .replace("{STATUS}", "" + VnStatus.RESERVED_FOR_OPT_IN.ordinal())
                        .replace("{MSISDN}", request.getMsisdn()),
                        new BeanPropertyRowMapper<>(String.class)
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

            // insert to tmp_table
            jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_ADD_UPDATE_SUB_REQUEST")
                    //.getQRY_ADD_UPDATE_SUB_REQUEST()
                    .replace("{MSISDN}", request.getMsisdn())
                    .replace("{STATUS}", "0"));

            //invoke ParentHandleDigit
            parentHandleDigits.run(request);

        } catch (Exception ex) {
            log.error("Exception: " + ex, ex);
        }
    }
}
