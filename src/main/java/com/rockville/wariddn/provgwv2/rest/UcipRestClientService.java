package com.rockville.wariddn.provgwv2.rest;

import com.rockville.wariddn.provgwv2.dto.UcipRequest;
import com.rockville.wariddn.provgwv2.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class UcipRestClientService {

    private final RestTemplate restTemplate;
    private final Constants constants;

    public UcipRequest ucipChargeUser(UcipRequest ucipRequest) {
        UcipRequest response = null;
        try {
            HttpEntity<UcipRequest> entity = new HttpEntity<>(ucipRequest);
            ResponseEntity<UcipRequest> responseEntity = restTemplate.postForEntity(constants.getUCIP_CHARGING_API(), entity, UcipRequest.class);
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                response = responseEntity.getBody();
            }
        } catch (Exception e) {
            log.error("Exception in ucipChargeUser for request : {}", ucipRequest, e);
        }
        return response;
    }
}
