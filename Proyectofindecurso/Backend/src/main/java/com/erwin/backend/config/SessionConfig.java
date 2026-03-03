package com.erwin.backend.config;

import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionConfig {

    @Bean
    public ServletListenerRegistrationBean<SessionLogListener> sessionListener() {
        return new ServletListenerRegistrationBean<>(new SessionLogListener());
    }
}