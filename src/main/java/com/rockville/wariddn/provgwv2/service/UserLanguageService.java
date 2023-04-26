/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rockville.wariddn.provgwv2.service;

import com.rockville.wariddn.provgwv2.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * @author Nouman Ahmed
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserLanguageService {
    
    private final JdbcTemplate jdbcTemplate;
    private final Constants constants;
    
    public String changeUserLanguage(String msisdn, String language) {
        log.info("Change User language for msisdn : {} , language : {}", msisdn, language);
        String response = "Failed";
        try {
            msisdn = Operation.normalizeMsisdn(msisdn);
            String query = constants.getQRY_TEMPLATE().get("QRY_UPDATE_USER_LANGUAGE")
                   // getQRY_UPDATE_USER_LANGUAGE()
                    .replace("{lang}", language).replace("{msisdn}", msisdn);
            log.info("Query is-----:{}",query);
            if (jdbcTemplate.update(query) > 0) {
                response = "Success";
            }
        } catch (Exception e) {
            log.error("Exception in changeUserLanguage for msisnd : {} , language : {}", msisdn, language, e);
        }
        return response;
    }
}
