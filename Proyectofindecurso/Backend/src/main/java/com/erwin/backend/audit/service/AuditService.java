package com.erwin.backend.audit.service;
import com.erwin.backend.audit.dto.AuditEventDto;
import com.erwin.backend.audit.entity.*;
import com.erwin.backend.audit.repository.*;
import com.erwin.backend.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class AuditService {
    private final AuditConfigRepository configRepo;
    private final AuditLogRepository    logRepo;
    private final EmailService          emailService;
    private final ObjectMapper          objectMapper;

    public AuditService(AuditConfigRepository configRepo, AuditLogRepository logRepo,
                        EmailService emailService, ObjectMapper objectMapper) {
        this.configRepo = configRepo; this.logRepo = logRepo;
        this.emailService = emailService; this.objectMapper = objectMapper;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(AuditEventDto evento) {
        try {
            Optional<AuditConfig> configOpt = configRepo.findByEntidadAndAccion(evento.getEntidad(), evento.getAccion());
            if (configOpt.isEmpty() || !configOpt.get().getActivo()) return;
            AuditConfig config = configOpt.get();
            AuditLog log = new AuditLog();
            log.setConfig(config);
            log.setEntidad(evento.getEntidad());
            log.setEntidadId(evento.getEntidadId());
            log.setAccion(evento.getAccion());
            log.setIdUsuario(evento.getIdUsuario());
            log.setUsername(evento.getUsername());
            log.setCorreoUsuario(evento.getCorreoUsuario());
            log.setIpAddress(evento.getIpAddress());
            log.setEstadoAnterior(toJson(evento.getEstadoAnterior()));
            log.setEstadoNuevo(toJson(evento.getEstadoNuevo()));
            log.setMetadata(toJson(evento.getMetadata()));
            logRepo.save(log);
            if (Boolean.TRUE.equals(config.getNotificarEmail()) && config.getDestinatarios() != null && !config.getDestinatarios().isEmpty()) {
                emailService.enviarAlertaAuditoria(config.getDestinatarios(), evento.getEntidad(), evento.getAccion(),
                    config.getSeveridad(), evento.getUsername() != null ? evento.getUsername() : "sistema",
                    evento.getIpAddress() != null ? evento.getIpAddress() : "desconocida",
                    java.time.LocalDateTime.now().toString());
            }
        } catch (Exception e) {
            // fallo silencioso — la auditoría nunca debe interrumpir el flujo principal
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return null; }
    }

    public static String extractIp(HttpServletRequest req) {
        if (req == null) return null;
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) return ip.split(",")[0].trim();
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip;
        return req.getRemoteAddr();
    }
}
