package com.erwin.backend.audit.service;

import com.erwin.backend.audit.entity.AuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AuditSseService {

    // Lista thread-safe de clientes conectados — static para que sea compartida
    // entre el hilo HTTP (registrar) y el hilo @Async (notificar)
    private static final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public AuditSseService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Registra un nuevo cliente SSE.
     * Timeout de 10 minutos; el frontend reconecta automáticamente.
     */
    public SseEmitter registrar() {
        SseEmitter emitter = new SseEmitter(600_000L);

        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(()    -> emitters.remove(emitter));
        emitter.onError(e       -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Conexión en vivo establecida"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * Notifica a TODOS los clientes conectados con el nuevo log.
     * Usa un Map para evitar LazyInitializationException al serializar AuditLog
     * (cuyo campo config tiene FetchType.LAZY). El severidad se pasa explícitamente
     * desde AuditService donde el AuditConfig ya está cargado en memoria.
     */
    public void notificar(AuditLog log, String severidad) {
        System.out.println("[SSE] Notificando " + emitters.size()
                + " clientes — " + log.getEntidad() + "/" + log.getAccion());
        if (emitters.isEmpty()) return;

        String json;
        try {
            // Construir DTO plano que coincide con la interfaz AuditLog del frontend
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id",              log.getId());
            dto.put("entidad",         log.getEntidad());
            dto.put("entidadId",       log.getEntidadId());
            dto.put("accion",          log.getAccion());
            dto.put("username",        log.getUsername());
            dto.put("correoUsuario",   log.getCorreoUsuario());
            dto.put("ipAddress",       log.getIpAddress());
            dto.put("timestampEvento", log.getTimestampEvento());
            dto.put("estadoAnterior",  log.getEstadoAnterior());
            dto.put("estadoNuevo",     log.getEstadoNuevo());
            dto.put("metadata",        log.getMetadata());
            if (severidad != null) {
                // Imitar la estructura config.severidad que usa el template Angular
                dto.put("config", Map.of("severidad", severidad));
            }
            json = objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            return;
        }

        List<SseEmitter> muertos = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("nuevo-log")
                        .data(json));
            } catch (IOException ex) {
                muertos.add(emitter);
            }
        }

        emitters.removeAll(muertos);
    }

    /** Retorna cuántos clientes están conectados en este momento */
    public int clientesConectados() {
        return emitters.size();
    }
}
