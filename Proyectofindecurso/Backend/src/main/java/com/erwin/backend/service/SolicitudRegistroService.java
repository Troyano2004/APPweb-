package com.erwin.backend.service;

import com.erwin.backend.dtos.SolicitudPendienteResponse;
import com.erwin.backend.dtos.SolicitudRegistroRequest;
import com.erwin.backend.dtos.SolicitudRegistroResponse;
import com.erwin.backend.dtos.VerificarCodigoRequest;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import com.erwin.backend.security.CryptoUtil;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.sql.Array;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
public class SolicitudRegistroService {

    private final SolicitudRegistroRepository solicitudRepo;
    private final CarreraRepository carreraRepo;
    private final UsuarioRepository usuarioRepo;
    private final EstudianteRepository estudianteRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JdbcTemplate jdbc;

    public SolicitudRegistroService(
            SolicitudRegistroRepository solicitudRepo,
            CarreraRepository carreraRepo,
            UsuarioRepository usuarioRepo,
            EstudianteRepository estudianteRepo,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            JdbcTemplate jdbc
    ) {
        this.solicitudRepo = solicitudRepo;
        this.carreraRepo = carreraRepo;
        this.usuarioRepo = usuarioRepo;
        this.estudianteRepo = estudianteRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jdbc = jdbc;
    }

    // =========================================================
    // 1) PASO 1: Solo correo -> genera código y lo manda
    // =========================================================
    @Transactional
    public SolicitudRegistroResponse EnviarCorreo(String correo) {
        if (correo == null || correo.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CORREO_REQUERIDO");
        }

        String correoLimpio = correo.trim().toLowerCase();

        if (solicitudRepo.existsByCorreoAndEstado(correoLimpio, "APROBADO")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SOLICITUD_YA_APROBADA");
        }

        SolicitudRegistro solicitud = solicitudRepo.findByCorreo(correoLimpio).orElse(null);

        if (solicitud == null) {
            solicitud = new SolicitudRegistro();
            solicitud.setCorreo(correoLimpio);
            solicitud.setEstado("PENDIENTE");
        } else if ("RECHAZADO".equalsIgnoreCase(texto(solicitud.getEstado()))) {
            solicitud.setEstado("PENDIENTE");
        }

        String codigo = generarCodigo6();
        solicitud.setCodigoVerificacion(codigo);
        solicitudRepo.save(solicitud);

        emailService.enviarCodigo(correoLimpio, codigo);

        SolicitudRegistroResponse resp = new SolicitudRegistroResponse();
        resp.setCorreo(correoLimpio);
        resp.setEstado(solicitud.getEstado());
        resp.setMensaje("CODIGO_ENVIADO");
        return resp;
    }

    // =========================================================
    // 2) PASO 2: Verificar código -> cambia a VERIFICADO
    // =========================================================
    @Transactional
    public SolicitudRegistroResponse verificarCodigo(VerificarCodigoRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DATOS_REQUERIDOS");
        }

        String correo = texto(req.getCorreo()).toLowerCase();
        String codigo = texto(req.getCodigo());

        if (correo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CORREO_REQUERIDO");
        }
        if (codigo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODIGO_REQUERIDO");
        }

        SolicitudRegistro s = solicitudRepo.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SOLICITUD_NO_EXISTE"));

        if ("APROBADO".equalsIgnoreCase(texto(s.getEstado()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SOLICITUD_YA_APROBADA");
        }

        if (s.getCodigoVerificacion() == null || !s.getCodigoVerificacion().equals(codigo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODIGO_INCORRECTO");
        }

        s.setEstado("VERIFICADO");
        solicitudRepo.save(s);

        SolicitudRegistroResponse resp = new SolicitudRegistroResponse();
        resp.setCorreo(correo);
        resp.setEstado(s.getEstado());
        resp.setMensaje("CORREO_VERIFICADO");
        return resp;
    }

    // =========================================================
    // 3) PASO 3: YA VERIFICADO -> datos + carrera -> PENDIENTE_APROBACION
    // =========================================================
    @Transactional
    public SolicitudRegistroResponse enviarDatos(SolicitudRegistroRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DATOS_REQUERIDOS");
        }

        String correo = texto(req.getCorreo()).toLowerCase();
        String cedula = texto(req.getCedula());
        String nombres = texto(req.getNombres());
        String apellidos = texto(req.getApellidos());
        Integer idCarrera = req.getIdCarrera();

        if (correo.isEmpty() || cedula.isEmpty() || nombres.isEmpty() || apellidos.isEmpty() || idCarrera == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FALTAN_DATOS");
        }

        SolicitudRegistro s = solicitudRepo.findByCorreoAndEstado(correo, "VERIFICADO")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "PRIMERO_VERIFICA_CORREO"));

        if ("APROBADO".equalsIgnoreCase(texto(s.getEstado()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SOLICITUD_YA_APROBADA");
        }

        if (usuarioRepo.existsByCorreoInstitucionalIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "YA_EXISTE_USUARIO_CON_ESE_CORREO");
        }
        if (usuarioRepo.existsByCedula(cedula)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "YA_EXISTE_USUARIO_CON_ESTA_CEDULA");
        }

        Carrera carrera = carreraRepo.findById(idCarrera)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "CARRERA_NO_EXISTE"));

        s.setCedula(cedula);
        s.setNombres(nombres);
        s.setApellidos(apellidos);
        s.setCarrera(carrera);
        s.setEstado("PENDIENTE_APROBACION");
        solicitudRepo.save(s);

        SolicitudRegistroResponse resp = new SolicitudRegistroResponse();
        resp.setCorreo(correo);
        resp.setEstado(s.getEstado());
        resp.setMensaje("DATOS_RECIBIDOS_PENDIENTE_APROBACION");
        return resp;
    }

    // =========================================================
    // ADMIN: listar pendientes
    // =========================================================
    @Transactional
    public List<SolicitudPendienteResponse> listarPendientes() {
        return solicitudRepo.findByEstadoOrderByFechaSolicitudAsc("PENDIENTE_APROBACION")
                .stream()
                .map(this::toPendienteDto)
                .toList();
    }

    // =========================================================
    // ADMIN: aprobar -> usa sp_crear_usuario_v3 (SECURITY DEFINER)
    // =========================================================
    @Transactional
    public SolicitudRegistroResponse aprobar(Integer idSolicitud) {
        if (idSolicitud == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_SOLICITUD_REQUERIDO");
        }

        SolicitudRegistro s = solicitudRepo.findByIdSolicitudAndEstado(idSolicitud, "PENDIENTE_APROBACION")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_EXISTE_O_NO_ESTA_PENDIENTE"));

        if (usuarioRepo.existsByCedula(s.getCedula())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "YA_EXISTE_USUARIO_CON_ESTA_CEDULA");
        }
        if (usuarioRepo.existsByCorreoInstitucionalIgnoreCase(s.getCorreo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "YA_EXISTE_USUARIO_CON_ESE_CORREO");
        }

        String username    = construirUsername(s.getCorreo(), s.getCedula());
        String rawPass     = generarPassword10();
        String passwordHash = passwordEncoder.encode(rawPass);
        String passwordDbPlain = generarPassword10();
        String passwordDbEncrypted = CryptoUtil.encrypt(passwordDbPlain);

        // id_rol_app = 24 = ROLE_ESTUDIANTE
        Integer[] idsRolApp = new Integer[]{24};

        // Llamar al stored procedure sp_crear_usuario_v3 (SECURITY DEFINER)
        // que tiene GRANT a auth_reader y crea el usuario + rol de BD
        Integer idUsuario = jdbc.execute((java.sql.Connection con) -> {
            Array sqlArray = con.createArrayOf("integer", idsRolApp);
            return jdbc.queryForObject(
                    "SELECT public.sp_crear_usuario_v3(?,?,?,?,?,?,?,?,?,?,?)",
                    Integer.class,
                    s.getCedula(),
                    s.getCorreo(),
                    username,
                    passwordHash,
                    s.getNombres(),
                    s.getApellidos(),
                    true,
                    sqlArray,
                    username,           // username_db
                    passwordDbEncrypted,
                    passwordDbPlain
            );
        });

        if (idUsuario == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_CREAR_USUARIO");
        }

        // Insertar en tabla estudiante (necesita GRANT INSERT a auth_reader)
        Usuario u = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "USUARIO_NO_ENCONTRADO_TRAS_CREAR"));

        Estudiante e = new Estudiante();
        e.setUsuario(u);
        e.setCarrera(s.getCarrera());
        e.setDiscapacidad(false);
        estudianteRepo.save(e);

        s.setEstado("APROBADO");
        solicitudRepo.save(s);

        emailService.enviarCredenciales(s.getCorreo(), username, rawPass);

        SolicitudRegistroResponse resp = new SolicitudRegistroResponse();
        resp.setIdSolicitud(s.getIdSolicitud());
        resp.setCorreo(s.getCorreo());
        resp.setEstado(s.getEstado());
        resp.setMensaje("SOLICITUD APROBADA Y CREDENCIALES ENVIADAS");
        return resp;
    }

    // =========================================================
    // ADMIN: rechazar
    // =========================================================
    @Transactional
    public SolicitudRegistroResponse rechazar(Integer idSolicitud, String motivo) {
        if (idSolicitud == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_SOLICITUD_REQUERIDO");
        }

        SolicitudRegistro s = solicitudRepo.findByIdSolicitudAndEstado(idSolicitud, "PENDIENTE_APROBACION")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_EXISTE_O_NO_ESTA_PENDIENTE"));

        s.setEstado("RECHAZADO");
        solicitudRepo.save(s);

        emailService.enviarRechazo(s.getCorreo(), texto(motivo));

        SolicitudRegistroResponse resp = new SolicitudRegistroResponse();
        resp.setIdSolicitud(s.getIdSolicitud());
        resp.setCorreo(s.getCorreo());
        resp.setEstado(s.getEstado());
        resp.setMensaje("SOLICITUD_RECHAZADA");
        return resp;
    }

    // ===========================
    // Helpers
    // ===========================
    private String generarCodigo6() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private String generarPassword10() {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    private SolicitudPendienteResponse toPendienteDto(SolicitudRegistro s) {
        String carrera = (s.getCarrera() == null) ? "" : safe(s.getCarrera().getNombre());
        String fecha = (s.getFechaSolicitud() == null) ? "" :
                s.getFechaSolicitud().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return new SolicitudPendienteResponse(
                s.getIdSolicitud(),
                safe(s.getCedula()),
                safe(s.getNombres()),
                safe(s.getApellidos()),
                safe(s.getCorreo()),
                carrera,
                safe(s.getEstado()),
                fecha
        );
    }

    private String construirUsername(String correo, String cedula) {
        String parteCorreo = texto(correo).toLowerCase();
        int at = parteCorreo.indexOf("@");
        if (at > 0) parteCorreo = parteCorreo.substring(0, at);
        parteCorreo = parteCorreo.replaceAll("[^a-z0-9._-]", "");
        String c = texto(cedula).replaceAll("\\s+", "");
        return parteCorreo + "_" + c;
    }

    private String texto(String s) {
        return s == null ? "" : s.trim();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}