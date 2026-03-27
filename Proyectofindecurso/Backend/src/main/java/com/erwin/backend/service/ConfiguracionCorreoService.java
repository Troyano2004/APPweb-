package com.erwin.backend.service;

import com.erwin.backend.dtos.ConfiguracionCorreoDto;
import com.erwin.backend.entities.ConfiguracionCorreo;
import com.erwin.backend.repository.ConfiguracionCorreoRepository;
import com.erwin.backend.security.CryptoUtil;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConfiguracionCorreoService {

    private final JdbcTemplate jdbcTemplate;
    private final ConfiguracionCorreoRepository repo;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final Map<String, String> HOSTS = Map.of(
            "GMAIL",   "smtp.gmail.com",
            "YAHOO",   "smtp.mail.yahoo.com",
            "OUTLOOK", "smtp-mail.outlook.com"
    );

    private static final String OUTLOOK_AUTH_URL =
            "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
    private static final String OUTLOOK_TOKEN_URL =
            "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    private static final String REDIRECT_URI =
            "http://localhost:8080/api/correo/outlook/callback";
    private static final String SCOPES =
            "https://outlook.office.com/SMTP.Send offline_access openid email";

    public ConfiguracionCorreoService(ConfiguracionCorreoRepository repo, JdbcTemplate jdbcTemplate) {
        this.repo = repo;
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================================================
    // toDto — incluye si está autorizado para Outlook
    // =========================================================
    private ConfiguracionCorreoDto toDto(ConfiguracionCorreo e) {
        ConfiguracionCorreoDto dto = new ConfiguracionCorreoDto();
        dto.setId(e.getId());
        dto.setActivo(e.getActivo());
        dto.setUsuario(e.getUsuario());
        dto.setProveedor(e.getProveedor());
        dto.setAutorizado(e.getRefreshToken() != null && !e.getRefreshToken().isBlank());
        return dto;
    }

    public List<ConfiguracionCorreoDto> listarTodas() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public ConfiguracionCorreoDto obtener() {
        ConfiguracionCorreo config = repo.findFirstByActivoTrue().orElse(null);
        return config == null ? null : toDto(config);
    }

    // =========================================================
    // Crear — Gmail/Yahoo usan password, Outlook usa OAuth2
    // =========================================================
    public ConfiguracionCorreoDto crear(ConfiguracionCorreoDto dto) {
        validarBase(dto);
        boolean esOutlook = dto.getProveedor().toUpperCase().equals("OUTLOOK");

        if (!esOutlook && (dto.getPassword() == null || dto.getPassword().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CONTRASEÑA_OBLIGATORIA");
        }
        if (esOutlook && (dto.getClientId() == null || dto.getClientId().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLIENT_ID_OBLIGATORIO_PARA_OUTLOOK");
        }
        if (esOutlook && (dto.getClientSecret() == null || dto.getClientSecret().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLIENT_SECRET_OBLIGATORIO_PARA_OUTLOOK");
        }

        boolean hayActivo = repo.findFirstByActivoTrue().isPresent();

        ConfiguracionCorreo config = new ConfiguracionCorreo();
        config.setProveedor(dto.getProveedor().toUpperCase());
        config.setUsuario(dto.getUsuario().trim());
        config.setActivo(!hayActivo);

        if (esOutlook) {
            config.setClientId(dto.getClientId().trim());
            config.setClientSecret(CryptoUtil.encrypt(dto.getClientSecret().trim()));
            config.setPassword(null);
        } else {
            config.setPassword(CryptoUtil.encrypt(dto.getPassword().trim()));
        }

        return toDto(repo.save(config));
    }

    // =========================================================
    // Editar
    // =========================================================
    public ConfiguracionCorreoDto editar(Integer id, ConfiguracionCorreoDto dto) {
        validarBase(dto);
        ConfiguracionCorreo config = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CONFIGURACION_NO_ENCONTRADA"));

        config.setProveedor(dto.getProveedor().toUpperCase());
        config.setUsuario(dto.getUsuario().trim());

        boolean esOutlook = dto.getProveedor().toUpperCase().equals("OUTLOOK");

        if (esOutlook) {
            if (dto.getClientId() != null && !dto.getClientId().isBlank()) {
                config.setClientId(dto.getClientId().trim());
            }
            if (dto.getClientSecret() != null && !dto.getClientSecret().isBlank()) {
                config.setClientSecret(CryptoUtil.encrypt(dto.getClientSecret().trim()));
            }
        } else {
            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                config.setPassword(CryptoUtil.encrypt(dto.getPassword().trim()));
            }
        }

        return toDto(repo.save(config));
    }

    // =========================================================
    // Activar
    // =========================================================
    public ConfiguracionCorreoDto activar(Integer id) {
        ConfiguracionCorreo target = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CONFIGURACION_NO_ENCONTRADA"));

        boolean esOutlook = target.getProveedor().toUpperCase().equals("OUTLOOK");
        if (esOutlook && (target.getRefreshToken() == null || target.getRefreshToken().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "OUTLOOK_NO_AUTORIZADO: debes autorizar la cuenta antes de activarla");
        }

        repo.findAll().forEach(c -> { c.setActivo(false); repo.save(c); });
        target.setActivo(true);
        return toDto(repo.save(target));
    }

    // =========================================================
    // Eliminar
    // =========================================================
    public void eliminar(Integer id) {
        ConfiguracionCorreo config = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CONFIGURACION_NO_ENCONTRADA"));
        if (Boolean.TRUE.equals(config.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_SE_PUEDE_ELIMINAR_CONFIG_ACTIVA");
        }
        repo.deleteById(id);
    }

    // =========================================================
    // OAuth2 — Generar URL de autorización para Outlook
    // =========================================================
    public String generarUrlAutorizacion(Integer id) {
        ConfiguracionCorreo config = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CONFIGURACION_NO_ENCONTRADA"));

        if (config.getClientId() == null || config.getClientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLIENT_ID_NO_CONFIGURADO");
        }

        return OUTLOOK_AUTH_URL +
                "?client_id=" + config.getClientId() +
                "&response_type=code" +
                "&redirect_uri=" + REDIRECT_URI +
                "&scope=" + SCOPES.replace(" ", "%20") +
                "&state=" + id +
                "&login_hint=" + config.getUsuario();
    }

    // =========================================================
    // OAuth2 — Procesar callback de Microsoft y guardar tokens
    // =========================================================
    public void procesarCallback(String code, Integer configId) {
        ConfiguracionCorreo config = repo.findById(configId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CONFIGURACION_NO_ENCONTRADA"));

        String clientSecret = CryptoUtil.decrypt(config.getClientSecret());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id",     config.getClientId());
        body.add("client_secret", clientSecret);
        body.add("code",          code);
        body.add("redirect_uri",  REDIRECT_URI);
        body.add("grant_type",    "authorization_code");
        body.add("scope",         SCOPES);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(OUTLOOK_TOKEN_URL, request, Map.class);
            Map<?, ?> responseBody = response.getBody();

            if (responseBody == null || !responseBody.containsKey("refresh_token")) {
                throw new RuntimeException("No se recibió refresh_token de Microsoft");
            }

            config.setRefreshToken((String) responseBody.get("refresh_token"));
            repo.save(config);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ERROR_PROCESANDO_CALLBACK: " + e.getMessage());
        }
    }

    // =========================================================
    // Validación base
    // =========================================================
    public void validarBase(ConfiguracionCorreoDto dto) {
        if (dto.getProveedor() == null || dto.getProveedor().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PROVEEDOR_REQUERIDO");
        }
        if (!HOSTS.containsKey(dto.getProveedor().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PROVEEDOR_NO_VALIDO");
        }
        if (dto.getUsuario() == null || dto.getUsuario().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USUARIO_REQUERIDO");
        }
    }
}