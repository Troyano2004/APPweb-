

package com.erwin.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private static final String SECRET_KEY = "bXlfc2VjcmV0X2tleV9wYXJhX2p3dF9zaWduYXR1cmVfMzJfYnl0ZXM=";
    private static final long EXPIRATION_MS = 1000L * 60 * 60; // 1 hora

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(SECRET_KEY));
    }

    // ✅ NUEVO: genera token con claims de BD
    public String generateToken(String username, String dbUser, String dbPass) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "db_user", dbUser != null ? dbUser : "",
                        "db_pass", dbPass  != null ? dbPass  : ""
                ))
                .issuedAt(now)
                .expiration(exp)
                .signWith(getSignKey())
                .compact();
    }

    // Mantener por compatibilidad (sin BD)
    public String generateToken(String username) {
        return generateToken(username, "", "");
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // ✅ NUEVO
    public String extractDbUser(String token) {
        Object val = extractAllClaims(token).get("db_user");
        return val != null ? val.toString() : "";
    }

    // ✅ NUEVO
    public String extractDbPass(String token) {
        Object val = extractAllClaims(token).get("db_pass");
        return val != null ? val.toString() : "";
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}