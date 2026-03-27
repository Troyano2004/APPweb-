package com.erwin.backend.audit.service;

import com.erwin.backend.audit.entity.AuditLog;
import com.erwin.backend.audit.repository.AuditLogRepository;
import com.erwin.backend.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SesionActivaRegistry {

    /** sessionId → datos de sesión activa */
    private final Map<String, SesionActivaInfo> sesiones = new ConcurrentHashMap<>();

    /**
     * username → quién cerró la sesión (nombre del admin).
     * El JwtFilter consulta este mapa en cada request autenticado.
     * Una vez detectado, se elimina (one-shot) y se devuelve 401.
     */
    private final Map<String, String> usuariosAExpulsar = new ConcurrentHashMap<>();

    private final AuditLogRepository auditLogRepo;
    private final UsuarioRepository  usuarioRepo;

    public SesionActivaRegistry(AuditLogRepository auditLogRepo,
                                 UsuarioRepository usuarioRepo) {
        this.auditLogRepo = auditLogRepo;
        this.usuarioRepo  = usuarioRepo;
    }

    // ── Inicialización al arrancar ────────────────────────────────────────────

    /**
     * Pre-carga sesiones reconstituidas desde audit_log al arrancar Spring Boot.
     * Toma los LOGINs de las últimas 8 horas que no tienen LOGOUT posterior.
     */
    @PostConstruct
    public void cargarSesionesDesdeLog() {
        try {
            LocalDateTime desde = LocalDateTime.now().minusHours(8);

            List<AuditLog> logins = auditLogRepo.findByAccionSince("LOGIN", desde);

            for (AuditLog login : logins) {
                String username = login.getUsername();

                // Saltar si ya hay una sesión activa para este usuario
                boolean yaRegistrado = sesiones.values().stream()
                        .anyMatch(s -> username.equals(s.getUsername()));
                if (yaRegistrado) continue;

                // Saltar si existe un LOGOUT posterior a este login
                boolean tieneLogout = auditLogRepo.existsByAccionAndUsernameAfter(
                        "LOGOUT", username, login.getTimestampEvento());
                if (tieneLogout) continue;

                SesionActivaInfo info = new SesionActivaInfo();
                info.setSessionId("recovered-" + username + "-" + System.currentTimeMillis());
                info.setUsername(username);
                info.setIp(login.getIpAddress());
                info.setInicio(login.getTimestampEvento());
                info.setUltimaActividad(LocalDateTime.now());

                // Enriquecer con rol y correo desde la BD
                usuarioRepo.findByUsername(username).ifPresent(u -> {
                    info.setRol(u.getRolAsignado());
                    info.setCorreo(u.getCorreoInstitucional());
                });

                sesiones.put(info.getSessionId(), info);
            }

            System.out.println("[SesionRegistry] Sesiones reconstituidas al arrancar: "
                    + sesiones.size());
        } catch (Exception e) {
            System.err.println("[SesionRegistry] Error al cargar sesiones: " + e.getMessage());
        }
    }

    // ── Registro / baja de sesiones ──────────────────────────────────────────

    public void registrarSesion(String sessionId, String username,
                                String rol, String correo, String ip) {
        System.out.println("[REGISTRY] registrarSesion llamado: "
                + username + " | sessionId=" + sessionId
                + " | ip=" + ip
                + " | total antes=" + sesiones.size());

        // Eliminar sesiones anteriores del mismo usuario antes de registrar la nueva
        sesiones.entrySet().removeIf(e -> username.equals(e.getValue().getUsername()));

        SesionActivaInfo info = new SesionActivaInfo();
        info.setSessionId(sessionId);
        info.setUsername(username);
        info.setRol(rol);
        info.setCorreo(correo);
        info.setIp(ip);
        info.setInicio(LocalDateTime.now());
        info.setUltimaActividad(LocalDateTime.now());
        sesiones.put(sessionId, info);

        System.out.println("[REGISTRY] sesiones después de guardar: "
                + sesiones.size());
    }

    /** Logout normal: elimina la sesión sin marcar para expulsión. */
    public void cerrarSesion(String sessionId) {
        sesiones.remove(sessionId);
    }

    /**
     * Cierre remoto por el admin:
     * elimina del mapa Y guarda el username del usuario con quién lo cerró.
     */
    public void marcarParaCerrar(String sessionId, String cerradaPor) {
        SesionActivaInfo info = sesiones.remove(sessionId);
        if (info != null) {
            usuariosAExpulsar.put(info.getUsername(),
                    cerradaPor != null ? cerradaPor : "el administrador");
        }
    }

    /** Logout manual — solo elimina del registry, NO marca para expulsión. */
    public void cerrarSesionesPorUsername(String username) {
        sesiones.entrySet().removeIf(e -> username.equals(e.getValue().getUsername()));
    }

    /** Cierre remoto por admin — elimina Y marca para expulsión. */
    public void cerrarSesionesPorUsername(String username, String cerradaPor) {
        sesiones.entrySet().removeIf(e -> username.equals(e.getValue().getUsername()));
        usuariosAExpulsar.put(username,
                cerradaPor != null ? cerradaPor : "el administrador");
    }

    // ── Control de expulsión por username (para JwtFilter) ───────────────────

    public boolean estaUsuarioMarcado(String username) {
        return username != null && usuariosAExpulsar.containsKey(username);
    }

    /** Devuelve quién cerró la sesión del usuario. */
    public String quienCerroSesionUsuario(String username) {
        return usuariosAExpulsar.getOrDefault(username, "el administrador");
    }

    /** Limpia la marca después de haber rechazado el request (one-shot). */
    public void limpiarMarcaUsuario(String username) {
        usuariosAExpulsar.remove(username);
    }

    // ── Métodos de sesión HTTP (compatibilidad secundaria) ────────────────────

    public boolean estaMarcadaParaCerrar(String sessionId) {
        return !sesiones.containsKey(sessionId);
    }

    public void limpiarMarca(String sessionId) {
        // No-op
    }

    // ── Lista de sesiones activas ─────────────────────────────────────────────

    public List<SesionActivaInfo> getSesionesActivas() {
        System.out.println("[REGISTRY] getSesionesActivas - total=" + sesiones.size());

        LocalDateTime limite = LocalDateTime.now().minusMinutes(30);
        sesiones.entrySet().removeIf(e -> {
            boolean expirada = e.getValue().getUltimaActividad().isBefore(limite);
            if (expirada) System.out.println("[REGISTRY] Eliminando expirada: "
                    + e.getValue().getUsername());
            return expirada;
        });

        System.out.println("[REGISTRY] después de limpiar: " + sesiones.size());
        return new ArrayList<>(sesiones.values());
    }

    public void actualizarActividad(String sessionId) {
        SesionActivaInfo info = sesiones.get(sessionId);
        if (info != null) info.setUltimaActividad(LocalDateTime.now());
    }

    /** Actualiza la última actividad buscando por username (usado desde JwtFilter). */
    public void actualizarActividadPorUsername(String username) {
        sesiones.values().stream()
                .filter(s -> username.equals(s.getUsername()))
                .forEach(s -> s.setUltimaActividad(LocalDateTime.now()));
    }

    public boolean existeSesion(String sessionId) {
        return sesiones.containsKey(sessionId);
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    @Getter @Setter
    public static class SesionActivaInfo {
        private String sessionId;
        private String username;
        private String rol;
        private String correo;
        private String ip;
        private LocalDateTime inicio;
        private LocalDateTime ultimaActividad;

        public long getMinutosActivo() {
            return Duration.between(inicio, LocalDateTime.now()).toMinutes();
        }
    }
}
