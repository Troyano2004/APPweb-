package com.erwin.backend.service;

import com.erwin.backend.dtos.ConfiguracionCorreoDto;
import com.erwin.backend.entities.ConfiguracionCorreo;
import com.erwin.backend.repository.ConfiguracionCorreoRepository;
import com.erwin.backend.security.CryptoUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConfiguracionCorreoService {

    private final ConfiguracionCorreoRepository repo;

    private static final Map<String, String> HOSTS = Map.of(
            "GMAIL",   "smtp.gmail.com",
            "YAHOO",   "smtp.mail.yahoo.com",
            "OUTLOOK", "smtp.office365.com"
    );

    public ConfiguracionCorreoService(ConfiguracionCorreoRepository repo) {
        this.repo = repo;
    }

    private ConfiguracionCorreoDto toDto(ConfiguracionCorreo e) {
        ConfiguracionCorreoDto dto = new ConfiguracionCorreoDto();
        dto.setId(e.getId());
        dto.setProveedor(e.getProveedor());
        dto.setUsuario(e.getUsuario());
        dto.setActivo(e.getActivo());
        // password NO se mapea al DTO por seguridad
        return dto;
    }


    // =========================================================
    // Obtener configuración activa (usada por EmailService)
    // =========================================================
    public ConfiguracionCorreoDto obtener() {
        ConfiguracionCorreo config = repo.findFirstByActivoTrue().orElse(null);
        if (config == null) return null;
        return toDtoSinPassword(config);
    }

    // =========================================================
    // Listar TODAS las configuraciones
    // =========================================================
    public List<ConfiguracionCorreoDto> listarTodas() {
        return repo.findAll().stream()
                .map(this::toDtoSinPassword)
                .collect(Collectors.toList());
    }

    // =========================================================
    // Crear nueva configuración
    // Queda activa solo si no hay ninguna activa aún
    // =========================================================
    public ConfiguracionCorreoDto crear(ConfiguracionCorreoDto dto) {
        validarBase(dto);

        if (dto.getPassword() == null || dto.getPassword().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PASSWORD_REQUERIDO");

        boolean hayActiva = repo.findFirstByActivoTrue().isPresent();

        ConfiguracionCorreo config = new ConfiguracionCorreo();
        config.setProveedor(dto.getProveedor().toUpperCase());
        config.setUsuario(dto.getUsuario().trim());
        config.setPassword(CryptoUtil.encrypt(dto.getPassword().trim()));
        config.setActivo(!hayActiva);

        return toDtoSinPassword(repo.save(config));
    }

    // =========================================================
    // Editar configuración existente (password opcional)
    // =========================================================
    public ConfiguracionCorreoDto editar(Integer id, ConfiguracionCorreoDto dto) {
        validarBase(dto);

        ConfiguracionCorreo config = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "CONFIGURACION_NO_ENCONTRADA"));

        config.setProveedor(dto.getProveedor().toUpperCase());
        config.setUsuario(dto.getUsuario().trim());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            config.setPassword(CryptoUtil.encrypt(dto.getPassword().trim()));
        }

        return toDtoSinPassword(repo.save(config));
    }

    // =========================================================
    // Activar una configuración (desactiva todas las demás)
    // =========================================================
    public ConfiguracionCorreoDto activar(Integer id) {
        ConfiguracionCorreo target = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "CONFIGURACION_NO_ENCONTRADA"));

        repo.findAll().forEach(c -> {
            c.setActivo(false);
            repo.save(c);
        });

        target.setActivo(true);
        return toDtoSinPassword(repo.save(target));
    }

    // =========================================================
    // Eliminar (no se puede eliminar la activa)
    // =========================================================
    public void eliminar(Integer id) {
        ConfiguracionCorreo config = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "CONFIGURACION_NO_ENCONTRADA"));

        if (Boolean.TRUE.equals(config.getActivo()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "NO_SE_PUEDE_ELIMINAR_CONFIG_ACTIVA");

        repo.deleteById(id);
    }

    // =========================================================
    // Validaciones base
    // =========================================================
    private void validarBase(ConfiguracionCorreoDto dto) {
        if (dto.getProveedor() == null || dto.getProveedor().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PROVEEDOR_REQUERIDO");
        if (!HOSTS.containsKey(dto.getProveedor().toUpperCase()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PROVEEDOR_NO_VALIDO");
        if (dto.getUsuario() == null || dto.getUsuario().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USUARIO_REQUERIDO");
    }

    // =========================================================
    // Mapper — nunca expone la password
    // =========================================================
    private ConfiguracionCorreoDto toDtoSinPassword(ConfiguracionCorreo c) {
        ConfiguracionCorreoDto dto = new ConfiguracionCorreoDto();
        dto.setId(c.getId());
        dto.setProveedor(c.getProveedor());
        dto.setUsuario(c.getUsuario());
        dto.setPassword(null);
        dto.setActivo(c.getActivo());
        return dto;
    }
}