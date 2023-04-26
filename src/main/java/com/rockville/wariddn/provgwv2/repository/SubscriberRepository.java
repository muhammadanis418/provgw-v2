package com.rockville.wariddn.provgwv2.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class SubscriberRepository {

    private final JdbcTemplate jdbcTemplate;

    public int update(String query) {
        int update = 0;
        try {
            update = jdbcTemplate.update(query);
        } catch (Exception e) {
            log.error("Exception in update for query : {}", query, e);
        }
        return update;
    }
}

