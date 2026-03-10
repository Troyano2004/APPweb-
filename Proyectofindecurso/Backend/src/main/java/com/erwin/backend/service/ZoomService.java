package com.erwin.backend.service;

import com.erwin.backend.dtos.ZoomMeetingResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class ZoomService {

    @Value("${zoom.account-id}")
    private String accountId;

    @Value("${zoom.client-id}")
    private String clientId;

    @Value("${zoom.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    // ── Obtener token OAuth ───────────────────────────────────
    private String obtenerToken() {
        try {
            String credentials = clientId + ":" + clientSecret;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encoded);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "grant_type=account_credentials&account_id=" + accountId;

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://zoom.us/oauth/token",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return mapper.readTree(response.getBody()).get("access_token").asText();

        } catch (Exception e) {
            throw new RuntimeException("Error obteniendo token Zoom: " + e.getMessage());
        }
    }
    // ── Crear reunión Zoom ────────────────────────────────────
    public ZoomMeetingResult crearReunion(String tema, LocalDate fecha, LocalTime hora) {
        try {
            String token = obtenerToken();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String startTime = fecha.toString() + "T" +
                    (hora != null ? hora.format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "08:00:00");

            Map<String, Object> body = new HashMap<>();
            body.put("topic", tema);
            body.put("type", 2);
            body.put("start_time", startTime);
            body.put("duration", 60);
            body.put("timezone", "America/Guayaquil");

            Map<String, Object> settings = new HashMap<>();
            settings.put("join_before_host", true);
            settings.put("waiting_room", false);
            body.put("settings", settings);

            String bodyJson = mapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.zoom.us/v2/users/me/meetings",
                    HttpMethod.POST,
                    entity,
                    String.class  // ← cambiar a String
            );

            JsonNode result = mapper.readTree(response.getBody());

            ZoomMeetingResult zoom = new ZoomMeetingResult();
            zoom.joinUrl = result.get("join_url").asText();
            zoom.meetingId = result.get("id").asText();
            return zoom;

        } catch (Exception e) {
            throw new RuntimeException("Error al crear reunión Zoom: " + e.getMessage());
        }
    }
}