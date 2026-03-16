package com.erwin.backend.audit.aspect;
import com.erwin.backend.audit.dto.AuditEventDto;
import com.erwin.backend.audit.service.AuditService;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.UsuarioRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Optional;

@Aspect @Component
public class AuditAspect {
    private final AuditService      auditService;
    private final UsuarioRepository usuarioRepo;

    public AuditAspect(AuditService auditService, UsuarioRepository usuarioRepo) {
        this.auditService = auditService; this.usuarioRepo = usuarioRepo;
    }

    @Around("@annotation(auditable)")
    public Object interceptar(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        Object estadoAnterior = (auditable.capturarArgs() && pjp.getArgs().length > 0) ? pjp.getArgs()[0] : null;
        Object resultado = pjp.proceed();
        try {
            String  username  = extraerUsername();
            Integer idUsuario = null;
            String  correo    = null;
            if (username != null) {
                Optional<Usuario> u = usuarioRepo.findByUsername(username);
                if (u.isPresent()) { idUsuario = u.get().getIdUsuario(); correo = u.get().getCorreoInstitucional(); }
            }
            auditService.registrar(AuditEventDto.builder()
                .entidad(auditable.entidad()).accion(auditable.accion())
                .idUsuario(idUsuario).username(username).correoUsuario(correo).ipAddress(extraerIp())
                .estadoAnterior(auditable.capturarArgs() ? estadoAnterior : null)
                .estadoNuevo(auditable.capturarArgs() ? resultado : null)
                .build());
        } catch (Exception e) { System.err.println("[AuditAspect] Error (no propagado): " + e.getMessage()); }
        return resultado;
    }

    private String extraerUsername() {
        try { Authentication a = SecurityContextHolder.getContext().getAuthentication(); if (a != null && a.isAuthenticated()) return a.getName(); } catch (Exception ignored) {}
        return null;
    }
    private String extraerIp() {
        try { ServletRequestAttributes a = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes(); if (a != null) return AuditService.extractIp(a.getRequest()); } catch (Exception ignored) {}
        return null;
    }
}