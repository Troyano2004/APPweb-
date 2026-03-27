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
        // Capturar IP y args ANTES de proceed() — el RequestContextHolder
        // solo está disponible en el hilo del request original
        String ipAddress      = extraerIp();
        Object estadoAnterior = (auditable.capturarArgs() && pjp.getArgs().length > 0) ? pjp.getArgs()[0] : null;
        long inicio = System.currentTimeMillis();
        Object resultado = pjp.proceed();
        long duracion = System.currentTimeMillis() - inicio;
        try {
            String  username   = extraerUsername();
            String  entidadId  = extraerEntidadId(resultado);
            Integer idUsuario  = null;
            String  correo     = null;
            if (username != null) {
                Optional<Usuario> u = usuarioRepo.findByUsername(username);
                if (u.isPresent()) { idUsuario = u.get().getIdUsuario(); correo = u.get().getCorreoInstitucional(); }
            }
            auditService.registrar(AuditEventDto.builder()
                .entidad(auditable.entidad()).accion(auditable.accion())
                .entidadId(entidadId)
                .idUsuario(idUsuario).username(username).correoUsuario(correo).ipAddress(ipAddress)
                .estadoAnterior(auditable.capturarArgs() ? estadoAnterior : null)
                .estadoNuevo(auditable.capturarArgs() ? resultado : null)
                .duracionMs((int) duracion)
                .build());
        } catch (Exception e) { System.err.println("[AuditAspect] Error (no propagado): " + e.getMessage()); }
        return resultado;
    }

    private String extraerEntidadId(Object obj) {
        if (obj == null) return null;
        for (String metodo : new String[]{"getId","getIdUsuario","getIdEstudiante","getIdDocente","getIdProyecto"}) {
            try {
                Object id = obj.getClass().getMethod(metodo).invoke(obj);
                if (id != null) return id.toString();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String extraerUsername() {
        try {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            if (a != null && a.isAuthenticated() && !"anonymousUser".equals(a.getName())) {
                return a.getName();
            }
        } catch (Exception ignored) {}
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                jakarta.servlet.http.HttpSession session = attrs.getRequest().getSession(false);
                if (session != null) {
                    Object user = session.getAttribute("usuario");
                    if (user != null) return user.toString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
    private String extraerIp() {
        try { ServletRequestAttributes a = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes(); if (a != null) return AuditService.extractIp(a.getRequest()); } catch (Exception ignored) {}
        return null;
    }
}