
package com.erwin.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DbStartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DbStartupLogger.class);

    @Value("${spring.datasource.username:auth_writer}")
    private String defaultUser;

    @Override
    public void run(ApplicationArguments args) {
        log.info("🟡 BACKEND STARTED -> DB DEFAULT (arranque) usando usuario BD = {}", defaultUser);
    }
}