package com.rockville.wariddn.provgwv2.service.root;

import com.rockville.wariddn.provgwv2.config.DnSmsHandlingConfig;
import com.rockville.wariddn.provgwv2.dto.*;
import com.rockville.wariddn.provgwv2.enums.ResponseEnum;
import com.rockville.wariddn.provgwv2.enums.TelcoType;
import com.rockville.wariddn.provgwv2.enums.VnStatus;
import com.rockville.wariddn.provgwv2.rest.RestClientService;
import com.rockville.wariddn.provgwv2.rest.UcipRestClientService;
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
public class ParentSubAllNewImpl extends Operation {

    private final Constants constants;
    private final CommonOperations commonOperations;
    private final SmsSendingService smsSendingService;
    private final RestClientService restClientService;
    private final UcipRestClientService ucipRestClientService;
    private final JdbcTemplate jdbcTemplate;
    private final DnSmsHandlingConfig dnSmsHandlingConfig;

    @Override
    public void run(Request request) {
        String sms_string_after_replacing_params = "";
        log.info("Request : {}", request);
        log.info(request.getSubAdditionalParameters().toString());
        boolean alreadySub = false;

        try {
            if (request.getMsisdn() == null) {
                log.error("[" + request.getTransactionId() + "] Invalid MSISDN:" + request.getMsisdn());
                return;
            }
            request.setMsisdn(normalizeMsisdn(request.getMsisdn()));

            request.getSubAdditionalParameters().setUserNetworkAndPackage(restClientService.getUserType(request.getMsisdn()).toUpperCase());
            request = validateLanguage(request);
            log.info("Msisdn : " + request.getMsisdn() + " User Network and package: " + request.getSubAdditionalParameters().getUserNetworkAndPackage());
            //Writing to DBSS Queue
            if (constants.getALLOWED_SUBSCRIPTION_BY_TYPE().get("ALLOWED_TYPE_" + request.getSubAdditionalParameters().getUserNetworkAndPackage()) != null && constants.getALLOWED_SUBSCRIPTION_BY_TYPE().get("ALLOWED_TYPE_" + request.getSubAdditionalParameters().getUserNetworkAndPackage())) {
                //Writing in DBSS queue
                log.info(request.getMsisdn() + " is in whitelist publishing in queue");
                RabbitModel rabbitModel = new RabbitModel();
                if (dnSmsHandlingConfig.getDbssSwapChannels().containsKey(request.getService().toUpperCase())) {
                    rabbitModel.setChannel(dnSmsHandlingConfig.getDbssSwapChannels().get(request.getService().toUpperCase()));
                } else {
                    rabbitModel.setChannel(request.getService());
                }
                rabbitModel.setMsisdn(request.getMsisdn());
                //Golden -> prepaid.golden
                //prepaid -> prep
                //postpaid - > po
                if (request.getSubAdditionalParameters().isGoldenSubscriptionRequest()) {
                    rabbitModel.setProductID(constants.getDBSS_PRODUCTID_CONFIGURATIONS().get("DAILY_PREPAID_GOLDEN"));
                } else if (request.getSubAdditionalParameters().getUserNetworkAndPackage().equalsIgnoreCase("prepaid")) {
                    rabbitModel.setProductID(constants.getDBSS_PRODUCTID_CONFIGURATIONS().get("DAILY_PREPAID_NORMAL"));
                } else if (request.getSubAdditionalParameters().getUserNetworkAndPackage().equalsIgnoreCase("postpaid")) {
                    rabbitModel.setProductID(constants.getDBSS_PRODUCTID_CONFIGURATIONS().get("MONTHLY_POSTPAID_NORMAL"));
                }
                log.info(rabbitModel.toString());
                rabbitModel.setType("products");
                rabbitModel.setProvisioningStatus("Pending");
                rabbitModel.setEvent(constants.getDBSS_EVENT_CONFIGURATIONS().get("ACTIVATE"));
                DbssResponse dbssResponse = restClientService.activateReq(rabbitModel);
//                DbssResponse dbssResponse = new DbssResponse();
//                dbssResponse.setSuccess(true);
                if (!dbssResponse.proceedWithProvisioning()) {
                    commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), "DBSS Failed: " + dbssResponse.getRespCode());
                    log.error("[" + request.getTransactionId() + "] DBSS Failed: " + dbssResponse.getMsg());

                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_SUB_FAILED_DBSS_" + request.getLanguage());
                    
                    smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                            sms_string_after_replacing_params, request.getTransactionId(), request.getSendSms()
                    );
                    return;
                }

                boolean do_subscription = true;
                boolean blocked_by_operator = false;
                VnDetails vnd = commonOperations.fetchSubscriptionDetailsByTelcoNum(request.getMsisdn());

                if (vnd != null) {
                    log.info(vnd.toString());
                } else {
                    log.info("VnDetails are NULL");
                }

                if (vnd != null && vnd.getVirtualMSISDN() > 0) {
                    // Existing subscriber details found in DB
                    switch (vnd.getStatus()) {
                        case 1:     // customer is already active
                        case 3:     // customer is already subscribed and has powered-OFF his service
                            do_subscription = false;
                            break;
                        case 2:     // in Low balance
                        case 4:     // unsubscribed
                            // we can send charging now and make the user active
                            break;
                        case 5:     // Blocked due to address/CNIC cannot re-activate their service
                            do_subscription = false;
                            blocked_by_operator = true;
                            break;
                        default:
                            break;
                    }
                } else {
                    request.getSubAdditionalParameters().setNewSubscriber(true);
                }

                if (do_subscription) {
//                    if (Constants.IS_MEMCACHE_ENABLED) {
//                        log.debug("[" + request.getTransactionId() + "] DelMemCache");
//                        MemcacheClient.getInstance().client.delete(request.getMsisdn());
//                    }

                    if (!request.getMethodName().equals("SubFreeWithReply")) {
                        //send 48 hours sms
                        smsSendingService.sendSms(
                                constants.getSHORT_CODE(),
                                request.getMsisdn(),
                                constants.getMESSAGE_TEMPLATE().get("MSG_48_HOURS_SUB_" + request.getLanguage()),
                                request.getTransactionId(),
                                false
                        );
                    } else {
                        log.info("[" + request.getTransactionId() + "] NOT sending SMS > " + constants.getMESSAGE_TEMPLATE().get("MSG_48_HOURS_SUB_" + request.getLanguage()));
                    }

                    boolean invoke_UCIP = true;

                    // if not Constants.is_postpaid_base_consolidated and subParams.subParams.getUserNetworkAndPackage() in ("waridpost"):
                    //      skip UCIP
                    // REMEMBER: this check will be eliminated as soon as Warid POSTPAID based is merged with JAZZ
                    UcipRequest ucipRequest = new UcipRequest();
                    ucipRequest.setMsisdn(request.getMsisdn());
                    ucipRequest.setTransactionId(request.getTransactionId());
                    ucipRequest.setOriginHostName(constants.getORIGIN_HOST_NAME());
                    ucipRequest.setOriginNodeType(constants.getORIGIN_NODE_TYPE());

                    if (constants.getDO_UCIP_CHARGING_BY_USER_NETWORK_PACKAGE_TYPE().get("UCIP_DO_CHARGING_" + request.getSubAdditionalParameters().getUserNetworkAndPackage()) != null
                            && !constants.getDO_UCIP_CHARGING_BY_USER_NETWORK_PACKAGE_TYPE().get("UCIP_DO_CHARGING_" + request.getSubAdditionalParameters().getUserNetworkAndPackage())) {
                        ucipRequest.setDeductionAmount(0);
                        ucipRequest.setResponseCode("102");
                        ucipRequest.setTransactionCode("NOT_CHARGED");
                        invoke_UCIP = false;
                    }

                    // Decide about
                    // - UCIP transaction details (amount and code)
                    // - TABS
                    // as we have different details for UCIP and TABS
                    String package_config_key = "NORMAL";   // NORMAL, GOLDEN, FREE
                    if (request.getSubAdditionalParameters().isGoldenSubscriptionRequest()) {
                        log.info("GOLDEN SUB request");
                        package_config_key = "GOLDEN";

                    } else {
                        if (request.getSubAdditionalParameters().isNewSubscriber() && !request.getSubAdditionalParameters().isFreeSubscription()) {
                            log.info("NORMAL: NEW BUT NOT FREE SUB request");
                        } else if (!request.getSubAdditionalParameters().isNewSubscriber() && request.getSubAdditionalParameters().isFreeSubscription()) {
                            log.info("NORMAL: NOT NEW BUT FREE SUB request");
                            package_config_key = "FREE";
                            ucipRequest.setDeductionAmount(0);
                            ucipRequest.setResponseCode("0");
                            ucipRequest.setTransactionCode("NOT_CHARGED");
                            invoke_UCIP = false;
                        } else if (request.getMethodName().equals("SubFreeWithReply")) {
                            log.info("SubFreeWithReply no UCIP charging for msisdn : " + request.getMsisdn());
                            package_config_key = "FREE";
                            ucipRequest.setDeductionAmount(0);
                            ucipRequest.setResponseCode("0");
                            ucipRequest.setTransactionCode("NOT_CHARGED");
                            invoke_UCIP = false;
                        } else {
                            log.info("NORMAL: ELSE SUB request");
                        }
                    }

                    if (invoke_UCIP) {
                        // UCIP.CONF.prepaid.NORMAL.ADJUSTMENT_AMOUNT_RELATIVE
                        ucipRequest.setDeductionAmount(Integer.parseInt(constants.getUCIP_CONFIGURATIONS().get(request.getSubAdditionalParameters().getUserNetworkAndPackage() + "_" + package_config_key + "_ADJUSTMENT_AMOUNT_RELATIVE")));
                        ucipRequest.setTransactionCode(constants.getUCIP_CONFIGURATIONS().get(request.getSubAdditionalParameters().getUserNetworkAndPackage() + "_" + package_config_key + "_TRANSACTION_CODE"));
                        ucipRequest.setTransactionCurrency(constants.getUCIP_CONFIGURATIONS().get(request.getSubAdditionalParameters().getUserNetworkAndPackage() + "_" + package_config_key + "_TRANSACTION_CURRENCY"));
                        ucipRequest.setExternalData1(constants.getUCIP_CONFIGURATIONS().get(request.getSubAdditionalParameters().getUserNetworkAndPackage() + "_" + package_config_key + "_EXTERNAL_DATA1"));
                        ucipRequest.setExternalData2(constants.getUCIP_CONFIGURATIONS().get(request.getSubAdditionalParameters().getUserNetworkAndPackage() + "_" + package_config_key + "_EXTERNAL_DATA2"));
                        ucipRequest.setTransactionType(constants.getUCIP_CONFIGURATIONS().get(request.getSubAdditionalParameters().getUserNetworkAndPackage() + "_" + package_config_key + "_TRANSACTION_TYPE"));
                        ucipRequest.setOriginHostName(constants.getUCIP_CONFIGURATIONS().get(request.getSubAdditionalParameters().getUserNetworkAndPackage() + "_" + package_config_key + "_ORIGIN_HOST_NAME"));

                        UcipRequest ucipResponse = ucipRestClientService.ucipChargeUser(ucipRequest);
//                        ucipRequest.setResponseCode("0");
//                        UcipRequest ucipResponse = ucipRequest;
                        if (ucipResponse != null) {
                            resumeAfterUCIP(request, ucipResponse);
                        } else {
                            log.error("[" + request.getTransactionId() + "][" + request.getMsisdn() + "]*** Ucip Failure ***");
                            commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), "UcipFailure");

                            smsSendingService.sendSms(constants.getSHORT_CODE(),
                                    request.getMsisdn(),
                                    constants.getMESSAGE_TEMPLATE().get("MSG_FAILURE_GENERAL_" + request.getLanguage()),
                                    request.getTransactionId(), request.getSendSms()
                            );
                        }
                    } else {
                        // Verify Warid Number
                        // manually update UCIP object to simulate success
                        log.debug("SKIPPING UCIP");
                        resumeAfterUCIP(request, ucipRequest);
                    }

                    log.debug("[" + request.getTransactionId() + "] after future success and failure");
                    return;

                } else if (blocked_by_operator) {

                    commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), "re-SUB not allowed by Operator/SP");
                    log.error("[" + request.getTransactionId() + "] Blocked by Operator/SP:" + request.getMsisdn() + " | re-SUB not Allowed");

                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_SUB_NOT_ALLOWED_BLOCKED_BY_OPERATOR_" + request.getLanguage());
                } else {

                    commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), "AlreadySub");
                    log.error("[" + request.getTransactionId() + "] Already Subscribed");
                    alreadySub = true;
                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_ALREADY_SUB_" + request.getLanguage());
                }

                //}
            } else {
                //User is not alloweed to subscribe
                log.error(request.getSubAdditionalParameters().getUserNetworkAndPackage() + " Not allowed to subscribe");
                commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), request.getSubAdditionalParameters().getUserNetworkAndPackage() + " Not Allowed to sub");
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_FAILURE_GENERAL_" + request.getLanguage());
                freeReservedGoldenNumber(request.getMsisdn());
            }

            if (!request.getMethodName().equals("SubFreeWithReply")) {
                smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                        sms_string_after_replacing_params, request.getTransactionId(), request.getSendSms()
                );
            } else if (request.getMethodName().equals("SubFreeWithReply") && !alreadySub) {
                smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                        sms_string_after_replacing_params, request.getTransactionId(), request.getSendSms()
                );
            }
        } catch (Exception e) {
            log.error("Exception in ParentSubAllNewImpl", e);
        }
    }

    private void resumeAfterUCIP(Request request, UcipRequest resp) {
        if (resp.getResponseCode() == null || resp.getResponseCode().isEmpty()) {
            log.debug("[" + request.getTransactionId() + "] UcipResponseCode Empty [Notify Customer: " + request.getSendSms() + "]");
            
            smsSendingService.sendSms(constants.getSHORT_CODE(),
                    request.getMsisdn(),
                    constants.getMESSAGE_TEMPLATE().get("MSG_FAILURE_GENERAL_" + request.getLanguage()),
                    request.getTransactionId(), request.getSendSms()
            );
            return;
        }

        String sms_string_after_replacing_params = "";
        String qry_after_param_replacement = "";

        String vn_type = ResponseEnum.NORMAL.name();
        if (request.getSubAdditionalParameters().isGoldenSubscriptionRequest()) {
            vn_type = ResponseEnum.GOLDEN.name();
        }

        String old_or_new = "old";
        if (request.getSubAdditionalParameters().isNewSubscriber()) {
            old_or_new = "new";
        }

        int respCode = Integer.parseInt(resp.getResponseCode());
        log.debug("[" + resp.getTransactionId() + "] responseCode:" + respCode);

        TelcoType telcoType = null;
        if (request.getSubAdditionalParameters().getUserNetworkAndPackage() != null
                && !"NA".equals(request.getSubAdditionalParameters().getUserNetworkAndPackage())
                && !"-1".equals(request.getSubAdditionalParameters().getUserNetworkAndPackage())) {
            telcoType = TelcoType.valueOf(request.getSubAdditionalParameters().getUserNetworkAndPackage().toLowerCase());
        } else {
            telcoType = TelcoType.prepaid;
        }

        if (respCode == 0) {
            if (request.getSubAdditionalParameters().getUserNetworkAndPackage().equals("POSTPAID")) {
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_SUB_SUCCESS_POSTPAID_" + vn_type + "_" + request.getLanguage());

                /*
                 QRY_SUB_SUCCESS_QUERY_POSTPAID_NORMAL="update NumberMapping
                 set TelcoMSISDN={TNUM}, status=1, WebPassword=NULL, CarryForward=0, BilledUpto=DATE_ADD(CURDATE(), INTERVAL 50 YEAR),
                 BillingTries=0, SubscriptionDate=NOW(), SubReqCount=1, TelcoType=2, Remarks=NULL, UnSubDate=NULL
                 where TelcoMSISDN IS NULL AND status=0 LIMIT 1"

                 QRY_SUB_SUCCESS_QUERY_POSTPAID_GOLDEN="update NumberMapping
                 set TelcoMSISDN={TNUM}, status=1, WebPassword=NULL, CarryForward=0, BilledUpto=DATE_ADD(CURDATE(), INTERVAL 50 YEAR),
                 BillingTries=0, SubscriptionDate=NOW(), SubReqCount=1, TelcoType=2, Remarks=NULL, UnSubDate=NULL
                 where VirtualMSISDN={VNUM}"
                 */
                if (request.getSubAdditionalParameters().isNewSubscriber()) {
                    if (request.getSubAdditionalParameters().isGoldenSubscriptionRequest()) {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_QUERY_POSTPAID_GOLDEN")
                                //getQRY_SUB_SUCCESS_QUERY_POSTPAID_GOLDEN()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{VNUM}", request.getSubAdditionalParameters().getGoldenDN())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());

                    } else {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_QUERY_POSTPAID_NORMAL")
                                //.getQRY_SUB_SUCCESS_QUERY_POSTPAID_NORMAL()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());
                    }
                } else {
                    if (request.getSubAdditionalParameters().isGoldenSubscriptionRequest()) {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_ACTIVATE_EXISTING_GOLDEN")
                                ////-----------getQRY_SUB_SUCCESS_ACTIVATE_EXISTING_GOLDEN()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());
                    } else {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_ACTIVATE_EXISTING_NORMAL_POSTPAID")
                                //------getQRY_SUB_SUCCESS_ACTIVATE_EXISTING_NORMAL_POSTPAID()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());
                    }
                }
            } else {
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_SUB_SUCCESS_PREPAID_" + vn_type + "_" + request.getLanguage());
                /*
             QRY_SUB_SUCCESS_QUERY_PREPAID_NORMAL="update NumberMapping
             set TelcoMSISDN={TNUM}, status=1, WebPassword=NULL, CarryForward=0, BilledUpto=CURDATE(), BillingTries=0,
             SubscriptionDate=NOW(), SubReqCount=1, TelcoType=1, Remarks=NULL, UnSubDate=NULL
             where TelcoMSISDN IS NULL AND status=0 LIMIT 1"

             QRY_SUB_SUCCESS_QUERY_PREPAID_GOLDEN="update NumberMapping
             set TelcoMSISDN={TNUM}, status=1, WebPassword=NULL, CarryForward=0, BilledUpto=CURDATE(), BillingTries=0,
             SubscriptionDate=NOW(), SubReqCount=1, TelcoType=1, Remarks=NULL, UnSubDate=NULL
             where VirtualMSISDN={VNUM}"
                 */
                //log.info("telcoType =" + telcoType);
                //log.info("telcoType ordinal =" + telcoType.getIntOrdinal());
                if (request.getSubAdditionalParameters().isNewSubscriber()) {
                    if (request.getSubAdditionalParameters().isGoldenSubscriptionRequest()) {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_QUERY_PREPAID_GOLDEN")
                                //--------getQRY_SUB_SUCCESS_QUERY_PREPAID_GOLDEN()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{VNUM}", request.getSubAdditionalParameters().getGoldenDN())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());
                    } else {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_QUERY_PREPAID_NORMAL")
                                //.getQRY_SUB_SUCCESS_QUERY_PREPAID_NORMAL()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());
                    }
                } else {
                    if (request.getSubAdditionalParameters().isGoldenSubscriptionRequest()) {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_ACTIVATE_EXISTING_GOLDEN")
                                //.getQRY_SUB_SUCCESS_ACTIVATE_EXISTING_GOLDEN()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());
                    } else {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_ACTIVATE_EXISTING_NORMAL_PREPAID")

                              //--------getQRY_SUB_SUCCESS_ACTIVATE_EXISTING_NORMAL_PREPAID()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());

                    }
                }
            }

            int res = jdbcTemplate.update(qry_after_param_replacement);
            log.info("Number mapping update rows : {} for msisdn : {}, query  : {}", res, request.getMsisdn(), qry_after_param_replacement);
            if (res > 0) {
                log.debug("[" + resp.getTransactionId() + "][" + resp.getMsisdn() + "] Charged: DB UPDATED: " + old_or_new);
                commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), "SUCCESS: " + old_or_new);

                VnDetails vnd = commonOperations.fetchSubscriptionDetailsByTelcoNum(request.getMsisdn());
                sms_string_after_replacing_params = sms_string_after_replacing_params.replaceAll("<VN>", vnd.getVirtualMSISDN() + "");
            } else {
                log.error("[" + resp.getTransactionId() + "][" + resp.getMsisdn() + "] Charged: DB FAILED: " + old_or_new);
                commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), "LocalDBFailure: " + old_or_new);
            }

            // install_service_on_tabs = true;
        } else if (respCode == 124) {
            if (constants.getALLOW_LOW_BALANCE_SUB() > 0 || request.getSubAdditionalParameters().isSubOnChargingFailure()) {
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_SUB_SUCCESS_PREPAID_" + vn_type + "_" + request.getLanguage());
                /*
                 QRY_LOW_BALANCE_SUB_QUERY_NORMAL="update NumberMapping
                 set TelcoMSISDN={TNUM}, status=1, WebPassword=NULL, CarryForward=0, BilledUpto=DATE_ADD(CURDATE(), INTERVAL -1 DAY),
                 BillingTries=0, SubscriptionDate=NOW(), SubReqCount=1, TelcoType=2, Remarks=NULL, UnSubDate=NULL
                 where TelcoMSISDN IS NULL AND status=0 LIMIT 1"

                 QRY_LOW_BALANCE_SUB_QUERY_GOLDEN="update NumberMapping
                 set TelcoMSISDN={TNUM}, status=1, WebPassword=NULL, CarryForward=0, BilledUpto=DATE_ADD(CURDATE(), INTERVAL -1 DAY),
                 BillingTries=0, SubscriptionDate=NOW(), SubReqCount=1, TelcoType=2, Remarks=NULL, UnSubDate=NULL
                 where VirtualMSISDN={VNUM}"
                 */
                //log.info("telcoType =" + telcoType);
                //log.info("telcoType ordinal =" + telcoType.getIntOrdinal());
                if (request.getSubAdditionalParameters().isNewSubscriber()) {
                    if (request.getSubAdditionalParameters().isGoldenSubscriptionRequest()) {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_LOW_BALANCE_SUB_QUERY_GOLDEN")
                                //.getQRY_LOW_BALANCE_SUB_QUERY_GOLDEN()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{VNUM}", request.getSubAdditionalParameters().getGoldenDN())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());
                    } else {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_LOW_BALANCE_SUB_QUERY_NORMAL")
                                //.getQRY_LOW_BALANCE_SUB_QUERY_NORMAL()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());
                    }
                } else {
                    if (request.getSubAdditionalParameters().isGoldenSubscriptionRequest()) {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_ACTIVATE_EXISTING_GOLDEN")
                                //----.getQRY_SUB_SUCCESS_ACTIVATE_EXISTING_GOLDEN()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());

                    } else {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_ACTIVATE_EXISTING_NORMAL_PREPAID")
                                //----------getQRY_SUB_SUCCESS_ACTIVATE_EXISTING_NORMAL_PREPAID()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());
                    }
                }

                int res = jdbcTemplate.update(qry_after_param_replacement);
                if (res > 0) {
                    log.debug("[" + resp.getTransactionId() + "][" + resp.getMsisdn() + "] LowBalSub: DB UPDATED: " + old_or_new);
                    commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), "SUCCESS: " + old_or_new);

                    VnDetails vnd = commonOperations.fetchSubscriptionDetailsByTelcoNum(request.getMsisdn());
                    sms_string_after_replacing_params = sms_string_after_replacing_params.replaceAll("<VN>", vnd.getVirtualMSISDN() + "");
                } else {
                    log.error("[" + resp.getTransactionId() + "][" + resp.getMsisdn() + "] LowBalSub: DB FAILED: " + old_or_new);
                    commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), "LocalDBFailure: " + old_or_new);
                }

                //  install_service_on_tabs = true;
            } else {
                // low balance (no sub)
                log.debug("[" + resp.getTransactionId() + "][" + resp.getMsisdn() + "] LowBalSub: Not Allowed: " + old_or_new);
                commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), "LowBal: " + old_or_new);

                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_FAILURE_LOW_BAL_" + request.getLanguage());
            }

        } else if (respCode == 102) {

            // Check if subscription against this user (operator_productType) is allowed or NOT
            if (constants.getALLOWED_SUBSCRIPTION_BY_TYPE().get("ALLOWED_TYPE_" + request.getSubAdditionalParameters().getUserNetworkAndPackage())) {
                //log.info("telcoType =" + telcoType);
                //log.info("telcoType ordinal =" + telcoType.getIntOrdinal());
                // sms_string_after_replacing_params.replaceAll("<VN>", vnd.getVirtualMSISDN() + "");
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_SUB_SUCCESS_POSTPAID_" + vn_type + "_" + request.getLanguage());

                /*
                 QRY_SUB_SUCCESS_QUERY_POSTPAID_NORMAL="update NumberMapping
                 set TelcoMSISDN={TNUM}, status=1, WebPassword=NULL, CarryForward=0, BilledUpto=DATE_ADD(CURDATE(), INTERVAL 50 YEAR),
                 BillingTries=0, SubscriptionDate=NOW(), SubReqCount=1, TelcoType=2, Remarks=NULL, UnSubDate=NULL
                 where TelcoMSISDN IS NULL AND status=0 LIMIT 1"

                 QRY_SUB_SUCCESS_QUERY_POSTPAID_GOLDEN="update NumberMapping
                 set TelcoMSISDN={TNUM}, status=1, WebPassword=NULL, CarryForward=0, BilledUpto=DATE_ADD(CURDATE(), INTERVAL 50 YEAR),
                 BillingTries=0, SubscriptionDate=NOW(), SubReqCount=1, TelcoType=2, Remarks=NULL, UnSubDate=NULL
                 where VirtualMSISDN={VNUM}"
                 */
                if (request.getSubAdditionalParameters().isNewSubscriber()) {
                    if (request.getSubAdditionalParameters().isGoldenSubscriptionRequest()) {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_QUERY_POSTPAID_GOLDEN")
                                //.getQRY_SUB_SUCCESS_QUERY_POSTPAID_GOLDEN()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{VNUM}", request.getSubAdditionalParameters().getGoldenDN())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());

                    } else {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_QUERY_POSTPAID_NORMAL")
                                //.getQRY_SUB_SUCCESS_QUERY_POSTPAID_NORMAL()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());
                    }
                } else {
                    if (request.getSubAdditionalParameters().isGoldenSubscriptionRequest()) {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_ACTIVATE_EXISTING_GOLDEN")
                                //---------.getQRY_SUB_SUCCESS_ACTIVATE_EXISTING_GOLDEN()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());
                    } else {
                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_SUB_SUCCESS_ACTIVATE_EXISTING_NORMAL_POSTPAID")
                                //---------getQRY_SUB_SUCCESS_ACTIVATE_EXISTING_NORMAL_POSTPAID()
                                .replace("{TNUM}", request.getMsisdn())
                                .replace("{telcoType}", "" + telcoType.getIntOrdinal());
                    }
                }

                int res = jdbcTemplate.update(qry_after_param_replacement);

                if (res > 0) {
                    VnDetails vnd = commonOperations.fetchSubscriptionDetailsByTelcoNum(request.getMsisdn());
                    sms_string_after_replacing_params = sms_string_after_replacing_params.replaceAll("<VN>", vnd.getVirtualMSISDN() + "");
                    log.debug("[" + resp.getTransactionId() + "][" + resp.getMsisdn() + "] Postpaid: DB UPDATED: " + old_or_new);
                    commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), "SUCCESS: " + old_or_new);
                } else {
                    log.error("[" + resp.getTransactionId() + "][" + resp.getMsisdn() + "] Postpaid: DB FAILED: " + old_or_new);
                    commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), "LocalDBFailure: " + old_or_new);
                }

                //    install_service_on_tabs = true;
            } else {

                log.error("[" + resp.getTransactionId() + "][" + resp.getMsisdn() + "] Service NOT allowed on POSTPAID: " + old_or_new);
                commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), "PostpaidNotAllowed: " + old_or_new);
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_POSTPAID_SUB_NOT_ALLOWED_" + request.getLanguage());
            }

        } else {

            log.error("[" + resp.getTransactionId() + "][" + resp.getMsisdn() + "] Not Charged[" + old_or_new + "] Ucip Response:" + resp.getResponseCode());
            commonOperations.writeActivityLog(request, request.getSubAdditionalParameters().getLogStr(), "UcipFailure [" + old_or_new + "] Ucip Response:" + resp.getResponseCode());
            sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_FAILURE_GENERAL_" + request.getLanguage());
            // TODO: call only
            freeReservedGoldenNumber(request.getMsisdn());

        }

        smsSendingService.sendSms(constants.getSHORT_CODE(),
                request.getMsisdn(),
                sms_string_after_replacing_params,
                request.getTransactionId(), request.getSendSms()
        );
    }

    public boolean freeReservedGoldenNumber(String msisdn) {
        List<ReservedVn> assigned_vns = jdbcTemplate.query(constants.getQRY_TEMPLATE().get("QRY_RETRIEVE_RESERVED_TMP_DN")
               // .getQRY_RETRIEVE_RESERVED_TMP_DN()
                .replace("{TNUM}", msisdn), new BeanPropertyRowMapper<>(ReservedVn.class));
        // If assigned_vns is null, delete request from DB.sub_request_status and return
        if (assigned_vns.isEmpty()) {
            log.info("NO record in DB.sub_tmp_assign_dn, delete from sub_request: "
                    + jdbcTemplate.update(constants.getQRY_TEMPLATE().get("QRY_DELETE_SUB_REQUEST")
                   // .getQRY_DELETE_SUB_REQUEST()
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
                    //.getQRY_RELEASE_RESERVED_DOUBLE_NUMBERS()
                            .replace("{VN_LIST}", vn_list.substring(1))
                            .replace("{MSISDN}", msisdn)
                            .replace("{RESERVED_FOR_SELECTION}", "" + VnStatus.RESERVED_FOR_OPT_IN.ordinal())) + "]"
            );
        }
        return true;
    }
}
