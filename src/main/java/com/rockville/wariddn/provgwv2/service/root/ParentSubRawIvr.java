package com.rockville.wariddn.provgwv2.service.root;

import com.rockville.wariddn.provgwv2.dto.Request;
import com.rockville.wariddn.provgwv2.dto.SubAdditionalParameters;
import com.rockville.wariddn.provgwv2.dto.VnDetails;
import com.rockville.wariddn.provgwv2.service.Operation;
import com.rockville.wariddn.provgwv2.util.CommonOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParentSubRawIvr extends Operation {

    private final CommonOperations commonOperations;
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

            VnDetails vn = commonOperations.fetchSubscriptionDetailsByTelcoNum(request.getMsisdn());
            if (vn != null) {
                log.info(vn.toString());
            }

            // Handle subscriber that is already in VN Database
            if (vn != null && vn.getVirtualMSISDN() > 0) {

                log.info("Existing Subscriber: " + vn.getVirtualMSISDN() + " | " + vn.getTelcoMSISDN() + " ... invoking ParentSubAllNewImpl");

                SubAdditionalParameters subparams = new SubAdditionalParameters(request.getSendSms(), "SUB", false, false, false);
                subparams.setNewSubscriber(false); // NOT required ???? as default value is false
                request.setSubAdditionalParameters(subparams);
                parentSubAllNewImpl.run(request);
            } else {
                request.setDescription("NO A Subscriber");
                commonOperations.writeActivityLog(request, "SUB", "FAILURE");
            }

        } catch (Exception ex) {
            log.error("Exception: " + ex, ex);
        }
    }
}
