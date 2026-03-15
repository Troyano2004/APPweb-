package com.erwin.backend.config;

import jakarta.servlet.http.HttpSession;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {

    // sessionId → HttpSession real
    private static final ConcurrentHashMap<String, HttpSession> sessions =
            new ConcurrentHashMap<>();

    public static void register(String sessionId, HttpSession session) {
        sessions.put(sessionId, session);
    }

    public static void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    public static void invalidar(String sessionId) {
        HttpSession session = sessions.get(sessionId);
        if (session != null) {
            try {
                session.invalidate(); // ← invalida la sesión HTTP real
            } catch (Exception ignored) {}
            sessions.remove(sessionId);
        }
    }
}