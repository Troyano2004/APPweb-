package com.erwin.backend.service;

import com.erwin.backend.config.SessionStore;
import com.erwin.backend.dtos.SesionActivaDto;
import com.erwin.backend.entities.SesionActiva;
import com.erwin.backend.repository.SesionActivaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
@Service
public class SesionActivaService {
    private final SesionActivaRepository sesionActivaRepository;
    private final SessionRegistry sessionRegistry;
    public SesionActivaService(SesionActivaRepository repository, SessionRegistry sessionRegistry) {
        this.sesionActivaRepository = repository;
        this.sessionRegistry = sessionRegistry;
    }
    public List<SesionActivaDto> listarActivas() {
        return sesionActivaRepository.findByActivoTrue()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    public SesionActivaDto cerrarSesion(Integer id) {
        SesionActiva  sesionActiva = sesionActivaRepository.findById(id).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND,"SESION_NO_ENCONTRADA"));
        sesionActiva.setActivo(false);
        sesionActivaRepository.save(sesionActiva);

        return toDto(sesionActiva);
    }
    public void marcarInactiva(String sessionId) {
        sesionActivaRepository.findBySessionId(sessionId).ifPresent(s -> {
            s.setActivo(false);
            sesionActivaRepository.save(s);

        });
    }
    public boolean estaActiva(String sessionId) {
        return sesionActivaRepository.findBySessionId(sessionId)
                .map(SesionActiva::getActivo)
                .orElse(false);
    }
    @Scheduled(fixedRate = 60000) // corre cada 1 minuto
    public void limpiarSesionesExpiradas() {
        LocalDateTime hace30Minutos = LocalDateTime.now().minusMinutes(30);

        sesionActivaRepository.findByActivoTrue().forEach(s -> {
            if (s.getUltimaActividad().isBefore(hace30Minutos)) {
                s.setActivo(false);
                sesionActivaRepository.save(s);
            }
        });

        LocalDateTime hace24Horas = LocalDateTime.now().minusHours(24);
        sesionActivaRepository.deleteByActivoFalseAndUltimaActividadBefore(hace24Horas);

    }
    public Optional<SesionActiva> buscarPorSessionId(String sessionId) {
        return sesionActivaRepository.findBySessionId(sessionId);
    }



        private SesionActivaDto toDto(SesionActiva s)
    {
        SesionActivaDto dto = new SesionActivaDto();
        dto.setId(s.getId());
        dto.setIdUsuario(s.getIdUsuario());
        dto.setNombres(s.getNombres());
        dto.setApellidos(s.getApellidos());
        dto.setRol(s.getRol());
        dto.setIp(s.getIp());
        dto.setFechaInicio(s.getFechaInicio());
        dto.setUltimaActividad(s.getUltimaActividad());
        return dto;

    }
}
