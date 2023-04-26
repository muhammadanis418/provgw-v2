/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rockville.wariddn.provgwv2.controller;

import com.rockville.wariddn.provgwv2.service.UserLanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Nouman Ahmed
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubscriberController {

    private final UserLanguageService languageService;

    @GetMapping("/setLanguage")
    public String changeUserLanguage(@RequestParam(value = "msisdn", required = true) String msisdn, @RequestParam(value = "lang", required = true) String lang) {
        return languageService.changeUserLanguage(msisdn, lang);
    }

}
