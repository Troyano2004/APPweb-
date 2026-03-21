package com.erwin.backend.service;

import com.erwin.backend.dtos.AsignarCarreraDocenteRequest;
import com.erwin.backend.dtos.DocenteCarreraResponse;
import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.Docente;
import com.erwin.backend.entities.DocenteCarrera;
import com.erwin.backend.repository.CarreraRepository;
import com.erwin.backend.repository.DocenteCarreraRepository;
import com.erwin.backend.repository.DocenteRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DocenteCarreraService {
    private final DocenteRepository docenteRepo;
    private final DocenteCarreraRepository docenteCarreraRepo;
    private final CarreraRepository carreraRepo;

    public DocenteCarreraService(DocenteRepository docenteRepo,
                                 DocenteCarreraRepository docenteCarreraRepo,
                                 CarreraRepository carreraRepo) {
        this.docenteRepo = docenteRepo;
        this.docenteCarreraRepo = docenteCarreraRepo;
        this.carreraRepo = carreraRepo;
    }

    public List<DocenteCarreraResponse> listarDocentes() {
        return docenteRepo.findAll().stream().map(d -> {
            DocenteCarreraResponse dto = new DocenteCarreraResponse();
            dto.setIdDocente(d.getIdDocente());
            dto.setNombres(d.getUsuario().getNombres());
            dto.setApellidos(d.getUsuario().getApellidos());
            dto.setUsername(d.getUsuario().getUsername());

            List<DocenteCarrera> asignaciones = docenteCarreraRepo.findByDocente_IdDocente(d.getIdDocente());
            if (!asignaciones.isEmpty()) {
                DocenteCarrera dc = asignaciones.get(0);
                dto.setIdCarrera(dc.getCarrera().getIdCarrera());
                dto.setCarrera(dc.getCarrera().getNombre());
                dto.setActivo(dc.getActivo());
                dto.setTieneCarrera(true);
                dto.setIdDocenteCarrera(dc.getIdDocenteCarrera());

            } else {
                dto.setTieneCarrera(false);
                dto.setActivo(false);
            }
            return dto;
        }).sorted((a, b) -> {
            if (!a.getTieneCarrera() && b.getTieneCarrera()) return -1;
            if (a.getTieneCarrera() && !b.getTieneCarrera()) return 1;
            return 0;
        })
                .collect(java.util.stream.Collectors.toList());
    }
    @Transactional
    public DocenteCarreraResponse asignarCarrera(AsignarCarreraDocenteRequest req) {

        Docente docente = docenteRepo.findById(req.getIdDocente()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCENTE NO ENCONTRADO"));
        Carrera carrera = carreraRepo.findById(req.getIdCarrera()).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "CARRERA NO ENCONTRADO"));

        DocenteCarrera dc  = docenteCarreraRepo.findByDocente_IdDocenteAndCarrera_IdCarrera(req.getIdDocente(), req.getIdCarrera()).orElse(new DocenteCarrera());

        dc.setDocente(docente);
        dc.setCarrera(carrera);
        dc.setActivo(true);
        docenteCarreraRepo.save(dc);

        DocenteCarreraResponse dto = new DocenteCarreraResponse();
        dto.setIdDocente(docente.getIdDocente());
        dto.setNombres(docente.getUsuario().getNombres());
        dto.setApellidos(docente.getUsuario().getApellidos());
        dto.setUsername(docente.getUsuario().getUsername());
        dto.setIdCarrera(carrera.getIdCarrera());
        dto.setCarrera(carrera.getNombre());
        dto.setActivo(true);
        dto.setTieneCarrera(true);
        return dto;

    }
    @Transactional
    public DocenteCarreraResponse cambiarEstado(Integer idDocenteCarrera, boolean activo) {
        DocenteCarrera dc = docenteCarreraRepo.findById(idDocenteCarrera)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ASIGNACION NO ENCONTRADA"));
        dc.setActivo(activo);
        docenteCarreraRepo.save(dc);

        DocenteCarreraResponse dto = new DocenteCarreraResponse();
        dto.setIdDocente(dc.getDocente().getIdDocente());
        dto.setNombres(dc.getDocente().getUsuario().getNombres());
        dto.setApellidos(dc.getDocente().getUsuario().getApellidos());
        dto.setUsername(dc.getDocente().getUsuario().getUsername());
        dto.setIdCarrera(dc.getCarrera().getIdCarrera());
        dto.setCarrera(dc.getCarrera().getNombre());
        dto.setActivo(dc.getActivo());
        dto.setTieneCarrera(true);
        dto.setIdDocenteCarrera(dc.getIdDocenteCarrera());
        return dto;
    }
    public List<DocenteCarreraResponse> filtrarPorCarrera(Integer idCarrera) {
        return docenteCarreraRepo.findByCarrera_IdCarrera(idCarrera)
                .stream()
                .map(d -> {
                    DocenteCarreraResponse dto = new DocenteCarreraResponse();
                    dto.setIdDocenteCarrera(d.getIdDocenteCarrera());
                    dto.setIdDocente(d.getDocente().getIdDocente());
                    dto.setNombres(d.getDocente().getUsuario().getNombres());
                    dto.setApellidos(d.getDocente().getUsuario().getApellidos());
                    dto.setUsername(d.getDocente().getUsuario().getUsername());
                    dto.setIdCarrera(d.getCarrera().getIdCarrera());
                    dto.setCarrera(d.getCarrera().getNombre());
                    dto.setActivo(d.getActivo());
                    dto.setTieneCarrera(true);
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}



