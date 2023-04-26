package com.rockville.wariddn.provgwv2.util;

import com.rockville.wariddn.provgwv2.dto.Request;
import com.rockville.wariddn.provgwv2.dto.VnDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommonOperations {

    private final JdbcTemplate jdbcTemplate;
    private final Constants constants;

    public void writeActivityLog(Request request, String type, String opResult) {

        String activityquery = constants.getQRY_ACTIVITY_LOG()
                .replace("{MSISDN}", request.getMsisdn())
                .replace("{SHORTCODE}", request.getDestination())
                .replace("{TYPE}", type)
                .replace("{DESC}", request.getDescription() + ":" + opResult)
                .replace("{CHANNEL}", request.getService());
        try {
            jdbcTemplate.update(activityquery);
        } catch (Exception e) {
            log.error("WriteActivitylog QRY: " + activityquery + " | Exception: " + e.getMessage());
        }
    }

    /**
     * the user must check if the details returned are valid or not
     * <pre>
     *   if (VnDetails != null && VnDetails.getVirtualMSISDN() > 0) {
     *      // telco is a valid Subscriber
     *   }
     * </pre>
     *
     * @param telco
     * @return
     */
    public VnDetails fetchSubscriptionDetailsByTelcoNum(String telco) {
        VnDetails vn = null;

        try {
            String query = constants.getQRY_TEMPLATE().get("QRY_GET_SUBSCRIBER_DETAILS_BY_TN")
                   // getQRY_GET_SUBSCRIBER_DETAILS_BY_TN()
                    .replace("{TMSISDN}", telco);
            log.info(query);
            vn = jdbcTemplate.queryForObject(query, new BeanPropertyRowMapper<>(VnDetails.class));
        } catch (Exception e) {
//            log.error("exception in fetchSubscriptionDetailsByTelcoNum for msisdn : {}", telco, e);
        }
        return vn;
    }

    /**
     * the user must check if the details returned are valid or not
     * <pre>
     *   if (VnDetails != null && VnDetails.getTelcoMSISDN() > 0) {
     *      // VNUM is assigned to TelcoMSISDN
     *   }
     * </pre>
     *
     * @param VNUM
     * @return
     */
//-------------------This method is not used which is taking Query getSubscriberDetailsByVn
   @Deprecated
    public VnDetails fetchSubscriptionDetailsByVirtualNum(String VNUM) {
        VnDetails vn = null;
        try {
            String query = constants.getQRY_TEMPLATE().get("QRY_GET_SUBSCRIBER_DETAILS_BY_VN")
                    //.getQRY_GET_SUBSCRIBER_DETAILS_BY_VN()
                    .replace("{VMSISDN}", VNUM);
            log.info(query);
            vn = jdbcTemplate.queryForObject(query, new BeanPropertyRowMapper<>(VnDetails.class));
        } catch (Exception e) {
//            log.error("Exception in fetchSubscriptionDetailsByVirtualNum for VNUM : {}", VNUM, e);
        }
        return vn;
    }

}
