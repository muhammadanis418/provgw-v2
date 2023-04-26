package com.rockville.wariddn.provgwv2.service.root;

import com.rockville.wariddn.provgwv2.dto.Request;
import com.rockville.wariddn.provgwv2.dto.ReservedVn;
import com.rockville.wariddn.provgwv2.dto.SubAdditionalParameters;
import com.rockville.wariddn.provgwv2.dto.VnDetails;
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
public class ParentHandleDigits extends Operation {

    private final Constants constants;
    private final CommonOperations commonOperations;
    private final SmsSendingService smsSendingService;
    private final JdbcTemplate jdbcTemplate;
    private final ParentSubAllNewImpl parentSubAllNewImpl;

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

            // DONE: Check in DB.sub_request_status
            String statusQuery = constants.getQRY_TEMPLATE().get("QRY_FETCH_SUB_REQUEST")
                   // .getQRY_FETCH_SUB_REQUEST()
                    .replace("{MSISDN}", request.getMsisdn());
            log.info("Status Query : {}", statusQuery);
            List<String> res = jdbcTemplate.queryForList(statusQuery, String.class);

            if (res.isEmpty()) {
                // SUB not initiated
                // Implementation: COMPLETED
                commonOperations.writeActivityLog(request, "HandleDigit:" + request.getDigit(), ResponseEnum.SUB_NOT_INITIATED.name());
                // Send invalid command + SUB procedure
                smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                        constants.getMESSAGE_TEMPLATE().get("MSG_INVALID_CMD_SUB_HELP_" + request.getLanguage()),
                        request.getTransactionId(), request.getSendSms());
                return;
            }

            // TODO: Check Status in NumberMapping
            VnDetails vnd = commonOperations.fetchSubscriptionDetailsByTelcoNum(request.getMsisdn());
            if (vnd != null) {
                log.info(vnd.toString());
            }

            // Old
            if (vnd != null && vnd.getVirtualMSISDN() > 0) {
                // telco is a valid Subscriber
                if (vnd.getStatus() == 1 || vnd.getStatus() == 3) {
                    freeReservedGoldenNumber(request.getMsisdn());
                    // Active VN
                    return;
                } else {
                    // Inactive VN
                }
            }

            // DB.sub_request_status.status
            log.info("DB.sub_request_status.status: {}", res);
            switch (res.get(0)) {
                case "0":
                    // DB.sub_request_status.status = 0
                    /*
                    SMS = 1 … subscribe to Golden number
                        Select 5 Available Golden Numbers
                        Update Their Status to 6/7 in DB.NumberMapping
                        Add these reserved VN's to DB.sub_tmp_assign_dn
                        Send these reserved VN's list to customer
                        Update status to 1 in DB.sub_request_status
                    SMS = 2 … subscribe to Normal number --------> TO THIS NOW
                        Delete from DB.sub_request_status
                        Call the Parent Subscription Class to handle it
                            Identify Customer TelcoType
                                If Prepaid: Charge Customer
                                If Postpaid: Install on TABS
                            Subscribe to Normal DN
                            Notify Customer
                    ELSE
                        reSend "1 for Golden and 2 for Normal" Seletion message
                     */
                    log.info("Handling: " + request.getDigit());
                    if (request.getDigit().equalsIgnoreCase("1")) {
                        try {
                            log.info("Inside 1: " + constants.getQRY_TEMPLATE().get("QRY_RESERVE_DOUBLE_NUMBERS")
                                    //.getQRY_RESERVE_DOUBLE_NUMBERS()
                                    .replace("{NEWSTATUS}", "" + VnStatus.RESERVED_FOR_OPT_IN.ordinal())
                                    .replace("{TNUM}", request.getMsisdn())
                            );
                        } catch (Exception e) {
                            log.error(e.getMessage());
                        }
                        int vns = jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_RESERVE_DOUBLE_NUMBERS")
                                //.getQRY_RESERVE_DOUBLE_NUMBERS()
                                .replace("{NEWSTATUS}", "" + VnStatus.RESERVED_FOR_OPT_IN.ordinal()).replace("{TNUM}", request.getMsisdn()));
                        log.info("Effected rows: {}", vns);

                        // process according to the # of DN's reserved
                        switch (vns) {
                            case 0:
                                log.info("NO VN Available");
                                // NO VN Available
                                // log to DB and notify customer
                                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_SUB_GOLDEN_NO_DN_AVAILABLE_" + request.getLanguage());
                                break;
                            case 1:
                                log.info("ONLY 1 VN is Available");
                                // Only 1 DN is available
                                // What to do, ask Business Team
                                break;
                            default:
                                log.info("Default: " + constants.getQRY_TEMPLATE().get("QRY_GET_RESERVE_DOUBLE_NUMBERS")
                                        //.getQRY_GET_RESERVE_DOUBLE_NUMBERS()
                                        .replace("{STATUS}", "" + VnStatus.RESERVED_FOR_OPT_IN.ordinal())
                                        .replace("{MSISDN}", request.getMsisdn())
                                );
                                // Fetch effected DN's l    ist
                                List<String> vn_list = jdbcTemplate.queryForList(constants.getQRY_TEMPLATE().get("QRY_GET_RESERVE_DOUBLE_NUMBERS")
                                        //.getQRY_GET_RESERVE_DOUBLE_NUMBERS()
                                        .replace("{STATUS}", "" + VnStatus.RESERVED_FOR_OPT_IN.ordinal()).replace("{MSISDN}", request.getMsisdn()), String.class);
                                log.info("Fetched VN's: {}", vn_list);

                                int count = 0;
                                String option_list = "";

                                for (String vn : vn_list) {
                                    count++;
                                    // create option list
                                    option_list += count + "- " + vn + "\n";
                                    // Add reserved DN's to DB.sub_tmp_assign_dn
                                    jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_SAVE_RESERVE_DOUBLE_NUMBERS")
                                            //getQRY_SAVE_RESERVE_DOUBLE_NUMBERS()
                                            .replace("{TNUM}", request.getMsisdn())
                                            .replace("{VNUM}", vn)
                                            .replace("{SEQ_NO}", "" + count)
                                            .replace("{LANG}", request.getLanguage())
                                            .replace("{CTYPE}", "0")
                                            .replace("{DTYPE}", "0"));
                                }
                                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_SUB_GOLDEN_OPTION_LIST_" + request.getLanguage())
                                        .replace("{OPTIONS}", "1-" + count)
                                        .replace("{DNLIST}", "\n" + option_list);
                                // Update Status to 1
                                log.info("1 is Almost Done: " + constants.getQRY_TEMPLATE().get("QRY_ADD_UPDATE_SUB_REQUEST")
                                        //.getQRY_ADD_UPDATE_SUB_REQUEST()
                                        .replace("{MSISDN}", request.getMsisdn())
                                        .replace("{STATUS}", "1")
                                );
                                jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_ADD_UPDATE_SUB_REQUEST")
                                        //.getQRY_ADD_UPDATE_SUB_REQUEST()
                                        .replace("{MSISDN}", request.getMsisdn())
                                        .replace("{STATUS}", "1"));

                                commonOperations.writeActivityLog(request, request.getDigit(), ResponseEnum.SUCCESS.name());
                                break;
                        }
                    } else if (request.getDigit().equalsIgnoreCase("2")) {

                        // DONE: Call Sub class
                        log.info("Invoke Subscribe Normal");
                        SubAdditionalParameters subParams = new SubAdditionalParameters(request.getSendSms(), "SubNormal", false, false, false);
                        if (vnd != null && vnd.getVirtualMSISDN() > 0) {
                            // RETURNING subscriber, activate existing DN
                            log.info("Existing Subscriber: " + vnd.getVirtualMSISDN() + " | " + vnd.getTelcoMSISDN() + " ... invoking ParentSubAllNewImpl");
                        } else {
                            subParams.setNewSubscriber(true);
                        }
                        request.setSubAdditionalParameters(subParams);
                        parentSubAllNewImpl.run(request);
                        log.info("after ParentSubAllNewImpl, Delete from sub_request table before returning: "
                                + jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_DELETE_SUB_REQUEST")
                                //.getQRY_DELETE_SUB_REQUEST()
                                        .replace("{MSISDN}", request.getMsisdn()))
                        );
                        return;

                    } else {
                        // DONE: invalid command, resend option list
                        log.info("invalid command, resend option list");

                        commonOperations.writeActivityLog(request, "HandleDigit:" + request.getDigit(), ResponseEnum.SUB_NOT_INITIATED.name());

                        // reSend "1 for Golden and 2 for Normal" Seletion message
                        sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_SUB_RAW_" + request.getLanguage());
                    }
                    break;
                case "1":
                    // DB.sub_request_status.status = 1
                    /*
                    Fetch reserved VN list from DB.sub_tmp_assign_dn
                        Check if customer reply matches with the options in reserved List
                            NO
                                update timestamp in DB.sub_request_status :: DONE
                                update timestamp in DB.sub_tmp_assign_dn :: DONE
                                update timestamp in DB.NumberMapping :: DONE
                                notify customer to reply correctly : DONE
                            YES
                                delete from DB.sub_request_status
                                extract VN against the selected option
                                Identify Customer TelcoType
                                    If Prepaid: Charge Customer premium
                                    If Postpaid: Install on TABS golden
                                update VN Status in DB.NumberMapping
                                release remaining 4 VNs from DB.NumberMapping
                                delete from DB.sub_tmp_assign_dn

                     */
                    // Fetch Assigned DN's list
                    List<ReservedVn> assigned_vns = jdbcTemplate.query(constants.getQRY_TEMPLATE().get("QRY_RETRIEVE_RESERVED_TMP_DN")
                           // .getQRY_RETRIEVE_RESERVED_TMP_DN()
                            .replace("{TNUM}", request.getMsisdn()), new BeanPropertyRowMapper<>(ReservedVn.class));
                    // If assigned_vns is null, delete request from DB.sub_request_status and return
                    if (assigned_vns.isEmpty()) {
                        log.info("NO record from DB.sub_tmp_assign_dn: "
                                + jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_DELETE_SUB_REQUEST")
                                //.getQRY_DELETE_SUB_REQUEST()
                                .replace("{MSISDN}", request.getMsisdn()))
                        );
                    } else {
                        log.info("Found " + assigned_vns.size() + " records in DB.sub_tmp_assign_dn");
                        /*
mysql> select * from sub_tmp_assign_dn where telcoMSISDN=3244355272;
+----+-------------+---------------+----------+----------+---------------------+-------+-------+
| id | telcoMSISDN | virtualMSISDN | sequence | language | TStamp              | ctype | dtype |
+----+-------------+---------------+----------+----------+---------------------+-------+-------+
|  2 |  3244355272 |    3200710002 |        1 | UR       | 2017-10-04 23:29:53 |     0 |     0 |
|  4 |  3244355272 |    3200710004 |        2 | UR       | 2017-10-04 23:29:53 |     0 |     0 |
|  6 |  3244355272 |    3200710005 |        3 | UR       | 2017-10-04 23:29:53 |     0 |     0 |
|  8 |  3244355272 |    3200710006 |        4 | UR       | 2017-10-04 23:29:53 |     0 |     0 |
| 10 |  3244355272 |    3200710007 |        5 | UR       | 2017-10-04 23:29:53 |     0 |     0 |
+----+-------------+---------------+----------+----------+---------------------+-------+-------+
5 rows in set (0.00 sec)
                         */
                        boolean is_valid_reply = false;
                        ReservedVn match = null;
                        String id_list = "";
                        String vn_list = "";

                        for (ReservedVn assigned_vn : assigned_vns) {
                            log.info("option: {}", assigned_vn);
                            id_list += "," + assigned_vn.getId();
                            vn_list += "," + assigned_vn.getVirtualMsisdn();
                            if (assigned_vn.getSequence().equalsIgnoreCase("" + request.getDigit())) {
                                log.info(" === Matched === ");
                                is_valid_reply = true;
                                match = assigned_vn;
                                // DO NOT break the loop, as we need id_list and vn_list for our operations
                                // break;
                            }
                        }

                        if (is_valid_reply) {
                            log.info("Matched : {}", match);
//                            log.info("Matched: {" + match[0] + ", " + match[1] + ", " + match[2] + ", " + match[3] + ", " + match[4] + ", " + match[5] + ", " + match[6] + "}");

                            // Call Subscription Handler for Golden DN
                            SubAdditionalParameters subParams = new SubAdditionalParameters(request.getSendSms(), "SubGolden" + request.getDigit(), false, true, false);
                            subParams.setNewSubscriber(true);
                            subParams.setGoldenDN(match.getVirtualMsisdn());
                            request.setSubAdditionalParameters(subParams);
                            parentSubAllNewImpl.run(request);
                            // TODO: decide if the below logic has to be handled in ParentSubAllNewImpl.Success and NOT here
                            log.info(constants.getQRY_TEMPLATE().get("QRY_DELETE_SUB_REQUEST")
                                   // .getQRY_DELETE_SUB_REQUEST()
                                    .replace("{MSISDN}", request.getMsisdn())
                                    + "\n"
                                    + constants.getQRY_TEMPLATE().get("QRY_DELETE_RESERVED_TMP_DN")
                                    //.getQRY_DELETE_RESERVED_TMP_DN()
                                    .replace("{ID_LIST}", id_list.substring(1))
                                    + "\n"
                                    + constants.getQRY_TEMPLATE().get("QRY_RELEASE_RESERVED_DOUBLE_NUMBERS")
                                    //.getQRY_RELEASE_RESERVED_DOUBLE_NUMBERS()
                                            .replace("{VN_LIST}", vn_list.substring(1))
                                            .replace("{MSISDN}", request.getMsisdn())
                                            .replace("{RESERVED_FOR_SELECTION}", "" + VnStatus.RESERVED_FOR_OPT_IN.ordinal())
                            );

                            log.info("Deleting from temp tables ["
                                    + "SUB_REQUEST: "
                                    + jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_DELETE_SUB_REQUEST")
                                    //.getQRY_DELETE_SUB_REQUEST()
                                    .replace("{MSISDN}", request.getMsisdn()))
                                    + ", TMP_ASSIGN_DN: "
                                    + jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_DELETE_RESERVED_TMP_DN")
                                   // .getQRY_DELETE_RESERVED_TMP_DN()
                                    .replace("{ID_LIST}", id_list.substring(1)))
                                    + ", NUMBER_MAPPING: "
                                    + jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_RELEASE_RESERVED_DOUBLE_NUMBERS")
                                    //.getQRY_RELEASE_RESERVED_DOUBLE_NUMBERS()
                                            .replace("{VN_LIST}", vn_list.substring(1))
                                            .replace("{MSISDN}", request.getMsisdn())
                                            .replace("{RESERVED_FOR_SELECTION}", "" + VnStatus.RESERVED_FOR_OPT_IN.ordinal())) + "]"
                            );

                            return;

                        } else {
                            log.error("NOT Matched, Resend DN Selection Notification");
                            /*
                                update timestamp in DB.sub_request_status :: DONE
                                update timestamp in DB.sub_tmp_assign_dn :: DONE
                                update timestamp in DB.NumberMapping :: DONE
                                notify customer to reply correctly : DONE
                             */
                            log.info("Updating datetime in relevant tables ["
                                    + "SUB_REQUEST: "
                                    + jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_HOLD_SUB_REQUEST")
                                   // getQRY_HOLD_SUB_REQUEST()
                                    .replace("{MSISDN}", request.getMsisdn()))
                                    + ", TMP_ASSIGN_DN: "
                                    + jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_HOLD_RESERVED_TMP_DN")
                                    //getQRY_HOLD_RESERVED_TMP_DN()
                                    .replace("{ID_LIST}", id_list.substring(1)))
                                    + ", NUMBER_MAPPING: "
                                    + jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_GET_RESERVE_DOUBLE_NUMBERS")
                                    //getQRY_HOLD_RESERVED_NUMERMAPPING()
                                    .replace("{VN_LIST}", vn_list.substring(1)))
                                    + "]"
                            );

                            // reSend "1 for Golden and 2 for Normal" Seletion message
                            sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_SUB_RAW_" + request.getLanguage());
                        }
                    }
                    break;
                default:
                    // DB.sub_request_status.status all non handled status
                    // Implementation: COMPLETED
                    commonOperations.writeActivityLog(request, "HandleDigit:" + request.getDigit(), ResponseEnum.SUB_NOT_INITIATED.name());

                    // reSend "1 for Golden and 2 for Normal" Seletion message
                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_SUB_RAW_" + request.getLanguage());
                    // TODO: do we want to update/retain relevant records to disable auto-removal ?
                    break;
            }

            smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                    sms_string_after_replacing_params,
                    request.getTransactionId(), request.getSendSms());
        } catch (Exception ex) {
            log.error("Exception: " + ex, ex);
        }
    }

    public boolean freeReservedGoldenNumber(String msisdn) {
        List<ReservedVn> assigned_vns = jdbcTemplate.query(constants.getQRY_TEMPLATE().get("QRY_RETRIEVE_RESERVED_TMP_DN")
                //.getQRY_RETRIEVE_RESERVED_TMP_DN()
                .replace("{TNUM}", msisdn), new BeanPropertyRowMapper<>(ReservedVn.class));
        // If assigned_vns is null, delete request from DB.sub_request_status and return
        if (assigned_vns.isEmpty()) {
            log.info("NO record in DB.sub_tmp_assign_dn, delete from sub_request: "
                    + jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_DELETE_SUB_REQUEST")
                    //.getQRY_DELETE_SUB_REQUEST()
                    .replace("{MSISDN}", msisdn))
            );
            return false;
        } else {
            log.info("Found " + assigned_vns.size() + " records in DB.sub_tmp_assign_dn");
            String id_list = "";
            String vn_list = "";
            for (ReservedVn assigned_vn : assigned_vns) {
                log.info("option: {}", assigned_vn);
                id_list += "," + assigned_vn.getId();
                vn_list += "," + assigned_vn.getVirtualMsisdn();
            }

            log.info("Deleting from temp tables ["
                    + "SUB_REQUEST: "
                    + jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_DELETE_SUB_REQUEST")
                    //.getQRY_DELETE_SUB_REQUEST()
                    .replace("{MSISDN}", msisdn))
                    + ", TMP_ASSIGN_DN: "
                    + jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_DELETE_RESERVED_TMP_DN")
                    //.getQRY_DELETE_RESERVED_TMP_DN()
                    .replace("{ID_LIST}", id_list.substring(1)))
                    + ", NUMBER_MAPPING: "
                    + jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_RELEASE_RESERVED_DOUBLE_NUMBERS")
                    //getQRY_RELEASE_RESERVED_DOUBLE_NUMBERS()
                            .replace("{VN_LIST}", vn_list.substring(1))
                            .replace("{MSISDN}", msisdn)
                            .replace("{RESERVED_FOR_SELECTION}", "" + VnStatus.RESERVED_FOR_OPT_IN.ordinal())) + "]"
            );
        }
        return true;
    }
}
