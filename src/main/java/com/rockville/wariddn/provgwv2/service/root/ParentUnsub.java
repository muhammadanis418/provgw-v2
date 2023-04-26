package com.rockville.wariddn.provgwv2.service.root;

import com.rockville.wariddn.provgwv2.config.DnSmsHandlingConfig;
import com.rockville.wariddn.provgwv2.dto.DbssResponse;
import com.rockville.wariddn.provgwv2.dto.RabbitModel;
import com.rockville.wariddn.provgwv2.dto.Request;
import com.rockville.wariddn.provgwv2.dto.VnDetails;
import com.rockville.wariddn.provgwv2.rest.RestClientService;
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
public class ParentUnsub extends Operation {
    
    private final Constants constants;
    private final CommonOperations commonOperations;
    private final SmsSendingService smsSendingService;
    private final RestClientService restClientService;
    private final JdbcTemplate jdbcTemplate;
    private final DnSmsHandlingConfig dnSmsHandlingConfig;
    
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
            
            if (vnd != null && vnd.getVirtualMSISDN() > 0 && vnd.getStatus() > 0 && vnd.getStatus() < 4) {
                log.info("UNSUB request for msisdn :" + request.getMsisdn() + " channel : " + request.getService());
                if (!constants.getDBSS_IGNORE_CHANNELS().contains(request.getService())) {
                    RabbitModel rabbitModel = new RabbitModel();
                    if (dnSmsHandlingConfig.getDbssSwapChannels().containsKey(request.getService().toUpperCase())) {
                        rabbitModel.setChannel(dnSmsHandlingConfig.getDbssSwapChannels().get(request.getService().toUpperCase()));
                    } else {
                        rabbitModel.setChannel(request.getService());
                    }
                    rabbitModel.setMsisdn(request.getMsisdn());
                    if (vnd.getTypeOfNum() == 9) {
                        rabbitModel.setProductID(constants.getDBSS_PRODUCTID_CONFIGURATIONS().get("DAILY_PREPAID_GOLDEN"));
                    } else if (vnd.getTelcoType() == 2 || vnd.getTelcoType() == 4 || vnd.getTelcoType() == 5 || vnd.getTelcoType() == 6) {
                        rabbitModel.setProductID(constants.getDBSS_PRODUCTID_CONFIGURATIONS().get("DAILY_PREPAID_NORMAL"));
                    } else if (vnd.getTelcoType() == 3 || vnd.getTelcoType() == 1 || vnd.getTelcoType() == 7) {
                        //rabbitModel.setProductID(Constants.DBSS_PRODUCTID_CONFIGURATIONS.get("DBSS.PRODUCTID.DAILY.POSTPAID.NORMAL"));
                        // sub was being done monthly package so we changed unsub from dail to monthly to handle EDA error
                        rabbitModel.setProductID(constants.getDBSS_PRODUCTID_CONFIGURATIONS().get("MONTHLY_POSTPAID_NORMAL"));
                    }
                    log.info(rabbitModel.getProductID());
                    rabbitModel.setType("products");
                    rabbitModel.setProvisioningStatus("Pending");
                    rabbitModel.setEvent(constants.getDBSS_EVENT_CONFIGURATIONS().get("DEACTIVATE"));
                    DbssResponse dbssResponse = restClientService.deactivateReq(rabbitModel);
//                    DbssResponse dbssResponse = new DbssResponse();
//                    dbssResponse.setSuccess(true);
                    if (!dbssResponse.proceedWithDeProvisioning(rabbitModel.getProductID())) {
                        commonOperations.writeActivityLog(request, "UNSUB", "DBSS UNSUB Failed: " + dbssResponse.getRespCode());
                        log.error("[" + request.getTransactionId() + "] DBSS UNSUB Failed: " + dbssResponse.getMsg());
                        sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_USUB_FAILED_DBSS_" + request.getLanguage());
                        smsSendingService.sendSms(constants.getSHORT_CODE(),
                                request.getMsisdn(),
                                sms_string_after_replacing_params,
                                request.getTransactionId(), request.getSendSms());
                        return;
                    }
                    
                }
                // MARK AS UNSUB IN LOCAL DB
                if (jdbcTemplate.update(constants.getQRY_UNSUBSCRIBE().replace("{TNUM}", request.getMsisdn()).replace("{MODE}", "0")) < 1) {
                    log.error("[" + request.getTransactionId() + "] Unsub DB Failure");
                    commonOperations.writeActivityLog(request, "UNSUB", "LocalDBFailure");
                } else {
                    log.debug("[" + request.getTransactionId() + "] Unsub updated in DB");
                    commonOperations.writeActivityLog(request, "UNSUB", "SUCCESS");
                }
                
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_UNSUB_SUCCESS_" + request.getLanguage());
            } else {
                
                log.error("[" + request.getTransactionId() + "] Not A Subscriber");
                commonOperations.writeActivityLog(request, "UNSUB", "NonSub");
                
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_NOT_SUB_" + request.getLanguage());
            }
            
            smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                    sms_string_after_replacing_params,
                    request.getTransactionId(), request.getSendSms());
        } catch (Exception e) {
            log.error("Exception in ParentUnsub for request : {} ", request, e);
        }
    }
}
