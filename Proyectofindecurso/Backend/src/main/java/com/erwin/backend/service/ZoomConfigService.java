package com.erwin.backend.service;

import com.erwin.backend.dtos.ZoomConfigDto;
import com.erwin.backend.entities.Docente;
import com.erwin.backend.entities.ZoomConfigDocente;
import com.erwin.backend.repository.DocenteRepository;
import com.erwin.backend.repository.ZoomConfigDocenteRepository;
import com.erwin.backend.security.CryptoUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class ZoomConfigService {
    private final ZoomConfigDocenteRepository repo;
    private final DocenteRepository docenteRepo;
    public ZoomConfigService(ZoomConfigDocenteRepository repo, DocenteRepository docenteRepo) {
        this.repo = repo;
        this.docenteRepo = docenteRepo;
    }
    public ZoomConfigDto obtener (Integer idDocente)
    {
        return repo.findByDocente_IdDocente(idDocente).map(c -> {
            ZoomConfigDto dto = new ZoomConfigDto();
            dto.setId(c.getId());
            dto.setAccountId(c.getAccountId());
            dto.setClientId(c.getClientId());
            dto.setClientSecret(c.getClientSecret());
            dto.setConfigurado(true);
            return dto;

        }).orElseGet(()->
        {
            ZoomConfigDto dto = new ZoomConfigDto();
            dto.setConfigurado(false);
            return dto;
        });
    }
    public ZoomConfigDto guardar(Integer idDocente, ZoomConfigDto req) {
        if (req.getAccountId() == null || req.getAccountId().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ACCOUNT_ID_REQUERIDO");
        if (req.getClientId() == null || req.getClientId().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLIENT_ID_REQUERIDO");
        if (req.getClientSecret() == null || req.getClientSecret().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLIENT_SECRET_REQUERIDO");

        Docente docente = docenteRepo.findById(idDocente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCENTE_NO_ENCONTRADO"));

        ZoomConfigDocente config = repo.findByDocente_IdDocente(docente.getIdDocente())
                .orElseGet(ZoomConfigDocente::new);

        config.setDocente(docente);
        config.setAccountId(req.getAccountId().trim());
        config.setClientId(req.getClientId().trim());
        config.setClientSecret(CryptoUtil.encrypt(req.getClientSecret().trim()));
        repo.save(config);

        ZoomConfigDto dto = new ZoomConfigDto();
        dto.setId(config.getId());
        dto.setAccountId(config.getAccountId());
        dto.setClientId(config.getClientId());
        dto.setClientSecret("");
        dto.setConfigurado(true);
        return dto;
    }
    public void eliminar(Integer idDocente) {
        ZoomConfigDocente config = repo.findByDocente_IdDocente(idDocente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CONFIGURACION_NO_ENCONTRADA"));
        repo.delete(config);
    }

}
