
package com.erwin.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class DbSessionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DbSessionFilter.class);

    // Llaves de sesión (usa estas mismas en AuthService)
    public static final String SES_DB_USER = "SES_DB_USER";
    public static final String SES_DB_PASS = "SES_DB_PASS";

    // Para no spamear logs
    private static final String LAST_MODE = "LAST_DB_MODE";

    // Tu default real (según application.properties)
    private static final String DEFAULT_USER = "auth_writer";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        String user = null;
        String pass = null;

        if (session != null) {
            user = (String) session.getAttribute(SES_DB_USER);
            pass = (String) session.getAttribute(SES_DB_PASS);
        }

        boolean hasCreds = user != null && !user.isBlank() && pass != null && !pass.isBlank();
        String modeNow = hasCreds ? ("SWITCHED:" + user) : ("DEFAULT:" + DEFAULT_USER);

        // Log SOLO cuando cambia (por sesión)
        if (session != null) {
            String last = (String) session.getAttribute(LAST_MODE);
            if (last == null || !last.equals(modeNow)) {
                session.setAttribute(LAST_MODE, modeNow);

                if (hasCreds) {
                    log.info("🟢 DB SWITCH -> ahora usando usuario BD = {} | sessionId={} | path={}",
                            user, session.getId(), request.getRequestURI());
                } else {
                    log.info("🟡 DB DEFAULT -> ahora usando usuario BD = {} | sessionId={} | path={}",
                            DEFAULT_USER, session.getId(), request.getRequestURI());
                }
            }
        } else {
            // Si no hay sesión, estás en default (esto puede repetirse por requests públicos).
            // Si te molesta el spam, lo quitamos.
            log.info("🟡 DB DEFAULT (sin sesión) -> usando usuario BD = {} | path={}",
                    DEFAULT_USER, request.getRequestURI());
        }

        try {
            if (hasCreds) {
                DbContextHolder.set(user, pass);
            }
            filterChain.doFilter(request, response);
        } finally {
            DbContextHolder.clear();
        }
    }
}