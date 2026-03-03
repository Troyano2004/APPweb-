package com.erwin.backend.controller;
import jakarta.servlet.http.HttpSession;
import com.erwin.backend.config.DbSessionFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DebugDbController {

    private final JdbcTemplate jdbc;

    public DebugDbController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/debug/session-db")
    public Map<String, Object> debug(HttpSession session) {
        Object sUser = session.getAttribute(DbSessionFilter.SES_DB_USER); // "DB_USER"
        Object sPass = session.getAttribute(DbSessionFilter.SES_DB_PASS); // "DB_PASS"

        String currentUser = jdbc.queryForObject("select current_user", String.class);

        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("sessionUser", sUser);
        out.put("sessionPassNull", (sPass == null));
        out.put("dbCurrentUser", currentUser);
        out.put("sessionId", session.getId());
        return out;
    }
}