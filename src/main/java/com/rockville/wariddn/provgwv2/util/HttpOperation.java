package com.rockville.wariddn.provgwv2.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class HttpOperation {

    @Autowired
    private RestTemplate restTemplate;
    private final static Logger log = LoggerFactory.getLogger(HttpOperation.class);

    public String sendGet(String url, MultiValueMap<String, String> params) {

        log.info("Send get");
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParams(params);

        log.info("Get Request for: " + builder.toUriString());

        ResponseEntity<String> response = restTemplate.getForEntity(builder.toUriString(), String.class);
        log.info("Response body: " + response.getBody());
        return response.getBody();
    }
//----------This method is not used
    @Deprecated()
    public String getUserType(String url) {

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        return response.getBody();
    }


}
