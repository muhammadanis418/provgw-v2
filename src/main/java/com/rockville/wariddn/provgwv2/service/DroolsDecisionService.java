package com.rockville.wariddn.provgwv2.service;

import com.rockville.wariddn.provgwv2.dto.Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DroolsDecisionService {
    private final KieSession kieSession;
    private final ApplicationContext applicationContext;

    public void processRequest(Request request) {
        try {
            log.info("Request before drool : {}", request);
            kieSession.insert(request);
            kieSession.fireAllRules();
            log.info("Request after drool : {}", request);
            Operation operation = (Operation) applicationContext.getBean(request.getMethodName());
            log.info("Bean received : {}", operation.getClass().getName());
            operation.run(request);
        } catch (Exception e) {
            log.error("Exception in processRequest for request : {}", request, e);
        }
    }
}
