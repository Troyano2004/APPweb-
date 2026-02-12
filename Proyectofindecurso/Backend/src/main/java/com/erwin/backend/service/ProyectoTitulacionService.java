// Proyectofindecurso/Backend/src/main/java/com/erwin/backend/service/ProyectoTitulacionService.java
package com.erwin.backend.service;

import com.erwin.backend.dtos.ProyectoTitulacionCreateRequest;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProyectoTitulacionService {

    private final ProyectoTitulacionRepository proyectoRepo;
    private final PropuestaTitulacionRepository propuestaRepo;
    private final PeriodoTitulacionRepository periodoRepo;
    private final DocenteRepository docenteRepo;
    private final TipoTrabajoTitulacionRepository tipoTrabajoRepo;
    private final EleccionTitulacionRepository eleccionRepo;

    public ProyectoTitulacionService(ProyectoTitulacionRepository proyectoRepo,
                                     PropuestaTitulacionRepository propuestaRepo,
                                     PeriodoTitulacionRepository periodoRepo,
                                     DocenteRepository docenteRepo,
                                     TipoTrabajoTitulacionRepository tipoTrabajoRepo,
                                     EleccionTitulacionRepository eleccionRepo) {
        this.proyectoRepo = proyectoRepo;
        this.propuestaRepo = propuestaRepo;
        this.periodoRepo = periodoRepo;
        this.docenteRepo = docenteRepo;
        this.tipoTrabajoRepo = tipoTrabajoRepo;
        this.eleccionRepo = eleccionRepo;
    }

    @Transactional
    public ProyectoTitulacion crearProyecto(ProyectoTitulacionCreateRequest req) {
        if (req.getIdPropuesta() == null || req.getIdPeriodo() == null || req.getIdTipoTrabajo() == null
                || req.getIdEleccion() == null || req.getTitulo() == null || req.getTitulo().isBlank()) {
            throw new RuntimeException("Debes enviar idPropuesta, idPeriodo, idTipoTrabajo, idEleccion y titulo");
        }

        PropuestaTitulacion propuesta = propuestaRepo.findById(req.getIdPropuesta())
                .orElseThrow(() -> new RuntimeException("Propuesta no encontrada"));

        if (proyectoRepo.findByPropuesta_IdPropuesta(req.getIdPropuesta()).isPresent()) {
            throw new RuntimeException("Ya existe un proyecto para esta propuesta");
        }

        PeriodoTitulacion periodo = periodoRepo.findById(req.getIdPeriodo())
                .orElseThrow(() -> new RuntimeException("Periodo no encontrado"));

        Tipotrabajotitulacion tipoTrabajo = tipoTrabajoRepo.findById(req.getIdTipoTrabajo())
                .orElseThrow(() -> new RuntimeException("Tipo de trabajo no encontrado"));

        EleccionTitulacion eleccion = eleccionRepo.findById(req.getIdEleccion())
                .orElseThrow(() -> new RuntimeException("Elección no encontrada"));

        Docente director = null;
        if (req.getIdDirector() != null) {
            director = docenteRepo.findById(req.getIdDirector())
                    .orElseThrow(() -> new RuntimeException("Director no encontrado"));
        }

        ProyectoTitulacion proyecto = new ProyectoTitulacion();
        proyecto.setPropuesta(propuesta);
        proyecto.setPeriodo(periodo);
        proyecto.setDirector(director);
        proyecto.setTipoTrabajo(tipoTrabajo);
        proyecto.setEleccion(eleccion);
        proyecto.setTitulo(req.getTitulo().trim());
        proyecto.setEstado(req.getEstado() == null || req.getEstado().isBlank() ? "ANTEPROYECTO" : req.getEstado());
        proyecto.setPorcentajeAntiplagio(req.getPorcentajeAntiplagio());
        proyecto.setFechaVerificacionAntiplagio(req.getFechaVerificacionAntiplagio());
        proyecto.setUrlInformeAntiplagio(req.getUrlInformeAntiplagio());

        return proyectoRepo.save(proyecto);
    }
}
