package com.rockville.wariddn.provgwv2.rest;

import com.rockville.wariddn.provgwv2.dto.DbssResponse;
import com.rockville.wariddn.provgwv2.dto.RabbitModel;
import com.rockville.wariddn.provgwv2.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestClientService {

    private final RestTemplate restTemplate;
    private final Constants constants;

    public DbssResponse activateReq(RabbitModel rabbitModel) {
        ResponseEntity<DbssResponse> response = restTemplate.postForEntity(constants.getDBSS_REQUEST().get("Activate"), rabbitModel, DbssResponse.class);
        log.info(rabbitModel.getMsisdn() + ": " + response.getBody().toString());
        return response.getBody();
    }

    public DbssResponse deactivateReq(RabbitModel rabbitModel) {
        ResponseEntity<DbssResponse> response = restTemplate.postForEntity(constants.getDBSS_REQUEST().get("DEACTIVATE"), rabbitModel, DbssResponse.class);
        log.info(rabbitModel.getMsisdn() + ": " + response.getBody().toString());
        return response.getBody();
    }

    public Boolean isSuspended(String msisdn) {
        ResponseEntity<Boolean> response = restTemplate.getForEntity(constants.getGET_USER_TYPE_API_ISSUSPENDED().replace("{MSISDN}", msisdn), Boolean.class);
        return response.getBody();
    }

    public String getUserType(String msisdn) {
        try {
            ResponseEntity<DbssResponse> responseEntity = restTemplate.getForEntity(constants.getDBSS_REQUEST().get("TYPE_CHECK") + msisdn, DbssResponse.class);
            if (responseEntity != null && responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody().getSuccess()) {
                return responseEntity.getBody().getRespData().toString();
            }
        } catch (Exception e) {
            log.error("Exception in getUserType for msisdn : {}  ", msisdn, e);
        }
        return "NA";
    }

}
