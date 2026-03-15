
package com.erwin.backend.security;

import com.erwin.backend.service.SesionActivaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {
    private final SesionActivaService sesionActivaService;
    private final SessionRegistry sessionRegistry;
    public SecurityConfig(SesionActivaService sesionActivaService,  SessionRegistry sessionRegistry) {
        this.sesionActivaService = sesionActivaService;
        this.sessionRegistry = sessionRegistry;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()
                        .requestMatchers("/debug/**").permitAll()
                        .anyRequest().permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)   // ✅ destruye sesión
                        .deleteCookies("JSESSIONID")   // ✅ borra cookie de sesión
                        .logoutSuccessHandler((req, res, auth) -> {
                            // ← MARCAR SESION INACTIVA EN BD
                            jakarta.servlet.http.HttpSession session = req.getSession(false);
                            if (session != null) {
                                sesionActivaService.marcarInactiva(session.getId());
                            }
                            System.out.println("🔴 LOGOUT realizado -> sesión invalidada");
                            res.setStatus(200);
                        })
                ).sessionManagement(session -> session
                        .maximumSessions(-1)          // -1 = sesiones ilimitadas por usuario
                        .sessionRegistry(sessionRegistry)  // usa el registry que definiste
                )

                .httpBasic(Customizer.withDefaults()); // opcional


        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","Origin","X-Requested-With"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}