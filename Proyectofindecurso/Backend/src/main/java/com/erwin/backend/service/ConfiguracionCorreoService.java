package com.erwin.backend.service;

import com.erwin.backend.dtos.ConfiguracionCorreoDto;
import com.erwin.backend.entities.ConfiguracionCorreo;
import com.erwin.backend.repository.ConfiguracionCorreoRepository;
import com.erwin.backend.security.CryptoUtil;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConfiguracionCorreoService {
    private final JdbcTemplate jdbcTemplate;
    private final ConfiguracionCorreoRepository repo;

    private static Map<String, String> HOSTS = Map.of( "GMAIL",   "smtp.gmail.com",
            "YAHOO",   "smtp.mail.yahoo.com",
            "OUTLOOK", "smtp-mail.outlook.com");

    public ConfiguracionCorreoService(ConfiguracionCorreoRepository repo, JdbcTemplate jdbcTemplate) {
        this.repo = repo;
        this.jdbcTemplate = jdbcTemplate;
    }
    private ConfiguracionCorreoDto toDto(ConfiguracionCorreo e) {
        ConfiguracionCorreoDto dto = new ConfiguracionCorreoDto();
        dto.setId(e.getId());
        dto.setActivo(e.getActivo());
        dto.setUsuario(e.getUsuario());
        dto.setProveedor(e.getProveedor());
        return dto;
    }
    public List<ConfiguracionCorreoDto> listarTodas() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    // =========================================================
    // Obtener configuración activa (usada por EmailService)
    // =========================================================
    public ConfiguracionCorreoDto obtener()
    {
        ConfiguracionCorreo config = repo.findFirstByActivoTrue().orElse(null);
        if (config == null) {
            return null;
        }
        return toDto(config);

    }
    public ConfiguracionCorreoDto crear(ConfiguracionCorreoDto dto) {
        validarBase(dto);
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CONTRASEÑA_OBLIGATORIA");
        }

        boolean hayActivo = repo.findFirstByActivoTrue().isPresent();
        String passwordEncriptado = CryptoUtil.encrypt(dto.getPassword().trim());

        jdbcTemplate.update(
                "CALL sp_crear_configuracion_correo(?, ?, ?, ?)",
                dto.getProveedor().toUpperCase(),
                dto.getUsuario().trim(),
                passwordEncriptado,
                !hayActivo
        );

        return repo.findByUsuario(dto.getUsuario().trim())
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_AL_CREAR"));
    }
    public ConfiguracionCorreoDto editar(Integer id,ConfiguracionCorreoDto dto) {
        validarBase(dto);
        ConfiguracionCorreo config = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CONFIGURACION_NO_ENCONTRADA"));
        config.setProveedor(dto.getProveedor().toUpperCase());
        config.setUsuario(dto.getUsuario().trim());
        if(dto.getPassword() != null &&  !dto.getPassword().isBlank()) {
            config.setPassword(CryptoUtil.encrypt(dto.getPassword().trim()));
        }
        return toDto(repo.save(config));

    }
    // =========================================================
    // Activar una configuración (desactiva todas las demás)
    // =========================================================
    public ConfiguracionCorreoDto activar(Integer id) {
        ConfiguracionCorreo target = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "CONFIGURACION_NO_ENCONTRADA"));
        repo.findAll().forEach(config -> {
            config.setActivo(false);
            repo.save(config);
        });
        target.setActivo(true);
        return toDto(repo.save(target));
    }
    // =========================================================
    // Eliminar (no se puede eliminar la activa)
    // =========================================================
    public void eliminar(Integer id) {
        ConfiguracionCorreo config = repo.findById(id).orElseThrow(()->new
                ResponseStatusException(HttpStatus.NOT_FOUND, "CONFIGURACION_NO_ENCONTRADA"));
        if(Boolean.TRUE.equals(config.getActivo())) { throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "NO_SE_PUEDE_ELIMINAR_CONFIG_ACTIVA");
        }
        repo.deleteById(id);

    }




    public void validarBase(ConfiguracionCorreoDto dto) {
       if(dto.getProveedor() == null || dto.getProveedor().isBlank()) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PROVEEDOR_REQUERIDO");
       }
        if(!HOSTS.containsKey(dto.getProveedor())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PROVEEDOR_NO_VALIDO");
        }
        if(dto.getUsuario() == null || dto.getUsuario().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USUARIO_REQUERIDO");
        }
    }

}