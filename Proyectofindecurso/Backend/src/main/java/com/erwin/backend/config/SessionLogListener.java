

package com.erwin.backend.config;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SessionLogListener implements HttpSessionListener {

    private static final Logger log = LoggerFactory.getLogger(SessionLogListener.class);

    // ✅ CORREGIDO: debe ser auth_reader (usuario de arranque real)
    private static final String DEFAULT_USER = "auth_reader";

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        log.info("🟡 SESSION CREATED -> DB DEFAULT {} | sessionId={}",
                DEFAULT_USER, se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        Object u = se.getSession().getAttribute(DbSessionFilter.SES_DB_USER);
        String lastUser = (u != null) ? u.toString() : DEFAULT_USER;

        log.info("🔴 SESSION DESTROYED -> vuelve a DEFAULT {} | ultimoUsuarioBD={} | sessionId={}",
                DEFAULT_USER, lastUser, se.getSession().getId());
    }
}