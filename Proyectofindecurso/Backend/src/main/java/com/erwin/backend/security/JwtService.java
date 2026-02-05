package com.erwin.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    // ✅ Debe ser Base64 y suficientemente larga (mínimo 256 bits)
    private static final String SECRET_KEY = "bXlfc2VjcmV0X2tleV9wYXJhX2p3dF9zaWduYXR1cmVfMzJfYnl0ZXM=";
    private static final long EXPIRATION_MS = 1000L * 60 * 60; // 1 hora

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(SECRET_KEY));
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(getSignKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
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
