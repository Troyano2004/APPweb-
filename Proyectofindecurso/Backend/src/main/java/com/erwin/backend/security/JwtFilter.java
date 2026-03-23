package com.erwin.backend.security;

import com.erwin.backend.audit.service.SesionActivaRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SesionActivaRegistry sesionRegistry;

    public JwtFilter(JwtService jwtService, SesionActivaRegistry sesionRegistry) {
        this.jwtService     = jwtService;
        this.sesionRegistry = sesionRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ✅ EXCEPCIÓN PARA LOGIN / LOGOUT (no proteger)
        String path = request.getServletPath();
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtService.extractUsername(token);

        // 🔒 Verificar si el admin cerró la sesión de este usuario
        if (sesionRegistry.estaUsuarioMarcado(username)) {
            String cerradaPor = sesionRegistry.quienCerroSesionUsuario(username);
            sesionRegistry.limpiarMarcaUsuario(username);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"error\":\"sesion_cerrada_por_admin\",\"cerradaPor\":\"" + cerradaPor + "\"}"
            );
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username, null, Collections.emptyList());
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // Mantener el timestamp de última actividad actualizado en el registry
        sesionRegistry.actualizarActividadPorUsername(username);

        filterChain.doFilter(request, response);
    }
}
