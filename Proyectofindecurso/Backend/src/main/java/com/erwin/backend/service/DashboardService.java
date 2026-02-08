package com.erwin.backend.service;

import com.erwin.backend.dtos.DashboardDetalleDto;
import com.erwin.backend.dtos.DashboardItemDto;
import com.erwin.backend.dtos.DashboardResumenDto;
import com.erwin.backend.entities.ObservacionDocumento;
import com.erwin.backend.entities.PropuestaTitulacion;
import com.erwin.backend.enums.EstadoDocumento;
import com.erwin.backend.enums.EstadoObservacion;
import com.erwin.backend.repository.ComplexivoTutoriaRepository;
import com.erwin.backend.repository.DocumentoTitulacionRepository;
import com.erwin.backend.repository.ObservacionDocumentoRepository;
import com.erwin.backend.repository.PropuestaTitulacionRepository;
import com.erwin.backend.repository.ProyectoTitulacionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {
    private final PropuestaTitulacionRepository propuestaRepo;
    private final DocumentoTitulacionRepository documentoRepo;
    private final ProyectoTitulacionRepository proyectoRepo;
    private final ComplexivoTutoriaRepository tutoriaRepo;
    private final ObservacionDocumentoRepository observacionRepo;

    public DashboardService(PropuestaTitulacionRepository propuestaRepo,
                            DocumentoTitulacionRepository documentoRepo,
                            ProyectoTitulacionRepository proyectoRepo,
                            ComplexivoTutoriaRepository tutoriaRepo,
                            ObservacionDocumentoRepository observacionRepo) {
        this.propuestaRepo = propuestaRepo;
        this.documentoRepo = documentoRepo;
        this.proyectoRepo = proyectoRepo;
        this.tutoriaRepo = tutoriaRepo;
        this.observacionRepo = observacionRepo;
    }

    public DashboardResumenDto obtenerResumen() {
        long propuestasPendientes = propuestaRepo.countByEstado("EN_REVISION");
        long tutoriasActivas = tutoriaRepo.count();
        long proyectosAprobados = proyectoRepo.countByEstado("FINALIZADO");
        long documentosPendientes = documentoRepo.countByEstado(EstadoDocumento.CORRECCION_REQUERIDA);

        return new DashboardResumenDto(
                propuestasPendientes,
                tutoriasActivas,
                proyectosAprobados,
                documentosPendientes
        );
    }

    public DashboardDetalleDto obtenerDetalle() {
        List<DashboardItemDto> alertas = new ArrayList<>();
        List<DashboardItemDto> actividades = new ArrayList<>();

        long propuestasRevision = propuestaRepo.countByEstado("EN_REVISION");
        long documentosCorreccion = documentoRepo.countByEstado(EstadoDocumento.CORRECCION_REQUERIDA);
        long observacionesPendientes = observacionRepo
                .findAllByOrderByCreadoEnDesc(PageRequest.of(0, 10))
                .stream()
                .filter(obs -> obs.getEstado() == EstadoObservacion.PENDIENTE)
                .count();

        alertas.add(new DashboardItemDto(
                "Propuestas en revisión: " + propuestasRevision,
                LocalDateTime.now()
        ));
        alertas.add(new DashboardItemDto(
                "Documentos con correcciones: " + documentosCorreccion,
                LocalDateTime.now()
        ));
        alertas.add(new DashboardItemDto(
                "Observaciones pendientes: " + observacionesPendientes,
                LocalDateTime.now()
        ));

        List<ObservacionDocumento> ultimasObservaciones = observacionRepo
                .findAllByOrderByCreadoEnDesc(PageRequest.of(0, 3));
        for (ObservacionDocumento obs : ultimasObservaciones) {
            String mensaje = "Observación en " + obs.getSeccion() + " del documento #" + obs.getDocumento().getId();
            actividades.add(new DashboardItemDto(mensaje, obs.getCreadoEn()));
        }

        List<PropuestaTitulacion> propuestasAprobadas =
                propuestaRepo.findTop5ByEstadoOrderByFechaRevisionDesc("APROBADA");
        for (PropuestaTitulacion propuesta : propuestasAprobadas) {
            if (actividades.size() >= 6) {
                break;
            }
            String titulo = propuesta.getTitulo() != null ? propuesta.getTitulo() : "propuesta";
            String mensaje = "Propuesta aprobada: " + titulo;
            actividades.add(new DashboardItemDto(
                    mensaje,
                    propuesta.getFechaRevision() != null
                            ? propuesta.getFechaRevision().atStartOfDay()
                            : LocalDateTime.now()
            ));
        }

        return new DashboardDetalleDto(alertas, actividades);
    }
}