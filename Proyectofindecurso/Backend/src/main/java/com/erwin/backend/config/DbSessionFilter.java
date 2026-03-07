

package com.erwin.backend.config;

import com.erwin.backend.security.JwtService;
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

    public static final String SES_DB_USER = "SES_DB_USER";
    public static final String SES_DB_PASS = "SES_DB_PASS";

    // ✅ El usuario de arranque/default — debe coincidir con spring.datasource.username
    private static final String DEFAULT_USER = "auth_reader";

    private final JwtService jwtService;

    public DbSessionFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         filterChain
    ) throws ServletException, IOException {

        String user = null;
        String pass = null;

        // ✅ PRIORIDAD 1: leer credenciales BD desde el JWT (Authorization: Bearer ...)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isTokenValid(token)) {
                String dbUser = jwtService.extractDbUser(token);
                String dbPass = jwtService.extractDbPass(token);
                if (dbUser != null && !dbUser.isBlank()
                        && dbPass != null && !dbPass.isBlank()) {
                    user = dbUser;
                    pass = dbPass;
                }
            }
        }

        // ✅ PRIORIDAD 2 (fallback): leer de sesión HTTP (para clientes con cookie)
        if (user == null || user.isBlank()) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String sesUser = (String) session.getAttribute(SES_DB_USER);
                String sesPass = (String) session.getAttribute(SES_DB_PASS);
                if (sesUser != null && !sesUser.isBlank()
                        && sesPass != null && !sesPass.isBlank()) {
                    user = sesUser;
                    pass = sesPass;
                }
            }
        }

        boolean switched = user != null && !user.isBlank();

        if (switched) {
            log.info("🟢 DB SWITCH -> usuario BD = {} | path={}",
                    user, request.getRequestURI());
        } else {
            log.debug("🟡 DB DEFAULT -> usuario BD = {} | path={}",
                    DEFAULT_USER, request.getRequestURI());
        }

        try {
            if (switched) {
                DbContextHolder.set(user, pass);
            }
            filterChain.doFilter(request, response);
        } finally {
            DbContextHolder.clear();
        }
    }
}