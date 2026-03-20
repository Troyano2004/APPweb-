package com.erwin.backend.service;

import com.erwin.backend.entities.BackupExecution;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

@Service
@Slf4j
public class BackupSseNotificationService {

    // Lista de clientes SSE conectados
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Suscribir cliente ──────────────────────────────────────────────────────

    public SseEmitter suscribir() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(()    -> emitters.remove(emitter));
        emitter.onError(e       -> emitters.remove(emitter));

        emitters.add(emitter);
        log.debug("Cliente SSE conectado. Total: {}", emitters.size());

        // Enviar ping inicial para confirmar conexión
        try {
            emitter.send(SseEmitter.event().name("ping").data("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    // ── Emitir evento de backup completado ────────────────────────────────────

    public void notificarBackupCompletado(BackupExecution exec, BackupExecution.EstadoEjecucion estado) {
        if (emitters.isEmpty()) return;

        try {
            Map<String, Object> payload = Map.of(
                    "tipo",           "BACKUP_COMPLETADO",
                    "idExecution",    exec.getIdExecution(),
                    "jobNombre",      exec.getJob() != null ? exec.getJob().getNombre() : "—",
                    "databaseNombre", exec.getDatabaseNombre() != null ? exec.getDatabaseNombre() : "—",
                    "estado",         estado.name(),
                    "duracion",       exec.getDuracionSegundos() != null ? exec.getDuracionSegundos() : 0,
                    "tamano",         exec.getTamanoBytes() != null ? exec.getTamanoBytes() : 0,
                    "manual",         Boolean.TRUE.equals(exec.getManual())
            );

            String json = objectMapper.writeValueAsString(payload);

            List<SseEmitter> muertos = new CopyOnWriteArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("backup-completado").data(json));
                } catch (IOException e) {
                    muertos.add(emitter);
                }
            }
            emitters.removeAll(muertos);

            log.debug("Evento SSE enviado a {} clientes", emitters.size() - muertos.size());
        } catch (Exception e) {
            log.error("Error enviando notificación SSE", e);
        }
    }

    // ── Emitir evento genérico ─────────────────────────────────────────────────

    public void emitirEvento(String nombre, Object datos) {
        if (emitters.isEmpty()) return;
        List<SseEmitter> muertos = new CopyOnWriteArrayList<>();
        try {
            String json = objectMapper.writeValueAsString(datos);
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name(nombre).data(json));
                } catch (IOException e) {
                    muertos.add(emitter);
                }
            }
        } catch (Exception e) {
            log.error("Error emitiendo evento SSE: {}", nombre, e);
        }
        emitters.removeAll(muertos);
    }
}