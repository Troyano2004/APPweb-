package com.erwin.backend.config;

import com.erwin.backend.repository.SesionActivaRepository;
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
import java.time.LocalDateTime;

@Component
public class DbSessionFilter extends OncePerRequestFilter {

    private final SesionActivaRepository sesionActivaRepository;

    public DbSessionFilter(SesionActivaRepository sesionActivaRepository) {
        this.sesionActivaRepository = sesionActivaRepository;
    }

    private static final Logger log = LoggerFactory.getLogger(DbSessionFilter.class);

    public static final String SES_DB_USER = "SES_DB_USER";
    public static final String SES_DB_PASS = "SES_DB_PASS";
    private static final String LAST_MODE = "LAST_DB_MODE";
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

            // actualizar ultima actividad
            sesionActivaRepository.findBySessionId(session.getId()).ifPresent(s -> {
                s.setUltimaActividad(LocalDateTime.now());
                sesionActivaRepository.save(s);
            });
        }

        boolean hasCreds = user != null && !user.isBlank() && pass != null && !pass.isBlank();
        String modeNow = hasCreds ? ("SWITCHED:" + user) : ("DEFAULT:" + DEFAULT_USER);

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