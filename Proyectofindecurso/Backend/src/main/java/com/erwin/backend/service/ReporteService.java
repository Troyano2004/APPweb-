package com.erwin.backend.service;

import com.erwin.backend.dtos.reporte.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    private final EstudianteRepository estudianteRepo;
    private final ProyectoTitulacionRepository proyectoRepo;
    private final TutoriaAnteproyectoRepository tutoriaRepo;
    private final TribunalProyectoRepository tribunalRepo;
    private final SustentacionRepository sustentacionRepo;
    private final EvaluacionSustentacionRepository evaluacionRepo;
    private final PeriodoTitulacionRepository periodoRepo;
    private final ActaRevisionDirectorRepository actaRepo;
    private final ReporteConfigService configService;

    public ReporteService(EstudianteRepository estudianteRepo,
                          ProyectoTitulacionRepository proyectoRepo,
                          TutoriaAnteproyectoRepository tutoriaRepo,
                          TribunalProyectoRepository tribunalRepo,
                          SustentacionRepository sustentacionRepo,
                          EvaluacionSustentacionRepository evaluacionRepo,
                          PeriodoTitulacionRepository periodoRepo,
                          ActaRevisionDirectorRepository actaRepo,
                          ReporteConfigService configService) {
        this.estudianteRepo  = estudianteRepo;
        this.proyectoRepo    = proyectoRepo;
        this.tutoriaRepo     = tutoriaRepo;
        this.tribunalRepo    = tribunalRepo;
        this.sustentacionRepo = sustentacionRepo;
        this.evaluacionRepo  = evaluacionRepo;
        this.periodoRepo     = periodoRepo;
        this.actaRepo        = actaRepo;
        this.configService   = configService;
    }

    // ── EXPEDIENTE POR ESTUDIANTE ─────────────────────────────────────────

    public ExpedienteEstudianteDto getExpediente(Integer idEstudiante) {
        Estudiante est = estudianteRepo.findById(idEstudiante)
            .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        ExpedienteEstudianteDto dto = new ExpedienteEstudianteDto();
        dto.setCedula(est.getUsuario().getCedula());
        dto.setNombres(est.getUsuario().getNombres());
        dto.setApellidos(est.getUsuario().getApellidos());
        dto.setCorreo(est.getUsuario().getCorreoInstitucional());
        dto.setCarrera(est.getCarrera() != null ? est.getCarrera().getNombre() : "-");
        dto.setPromedioRecord(est.getPromedioRecord80());
        dto.setDiscapacidad(est.getDiscapacidad());

        // Proyecto activo del estudiante
        proyectoRepo.findAll().stream()
            .filter(p -> p.getEleccion() != null
                && p.getEleccion().getEstudiante() != null
                && p.getEleccion().getEstudiante().getIdEstudiante().equals(idEstudiante))
            .findFirst()
            .ifPresent(proyecto -> {
                dto.setTituloProyecto(proyecto.getTitulo());
                dto.setEstadoProyecto(proyecto.getEstado());
                dto.setPorcentajeAntiplagio(proyecto.getPorcentajeAntiplagio());
                dto.setFechaAntiplagio(proyecto.getFechaVerificacionAntiplagio());
                if (proyecto.getTipoTrabajo() != null)
                    dto.setTipoTrabajo(proyecto.getTipoTrabajo().getNombre());
                if (proyecto.getPeriodo() != null)
                    dto.setPeriodo(proyecto.getPeriodo().getDescripcion());
                if (proyecto.getDirector() != null)
                    dto.setDirector(proyecto.getDirector().getUsuario().getNombres()
                        + " " + proyecto.getDirector().getUsuario().getApellidos());

                // Tutorías del anteproyecto relacionado al proyecto
                List<TutoriaAnteproyecto> tutorias = tutoriaRepo.findAll().stream()
                    .filter(t -> t.getAnteproyecto() != null
                        && t.getAnteproyecto().getPropuesta() != null
                        && proyecto.getPropuesta() != null
                        && t.getAnteproyecto().getPropuesta().getIdPropuesta()
                            .equals(proyecto.getPropuesta().getIdPropuesta()))
                    .collect(Collectors.toList());

                if (configService.isEnabled("seccion_tutorias")) {
                    dto.setTotalTutorias(tutorias.size());
                    dto.setTutoriasRealizadas((int) tutorias.stream()
                        .filter(t -> "REALIZADA".equals(t.getEstado())).count());
                    dto.setTutoriasPendientes((int) tutorias.stream()
                        .filter(t -> "PROGRAMADA".equals(t.getEstado())).count());
                    dto.setTutorias(tutorias.stream().map(t ->
                        new ExpedienteEstudianteDto.TutoriaReporteDto(
                            t.getFecha(), t.getModalidad(), t.getEstado(),
                            t.getDocente().getUsuario().getNombres() + " " + t.getDocente().getUsuario().getApellidos()
                        )).collect(Collectors.toList()));
                }

                // Tribunal
                if (configService.isEnabled("seccion_tribunal")) {
                    List<TribunalProyecto> tribunal = tribunalRepo.findAll().stream()
                        .filter(tr -> tr.getProyecto().getIdProyecto().equals(proyecto.getIdProyecto()))
                        .collect(Collectors.toList());
                    dto.setTribunal(tribunal.stream().map(tr ->
                        new ExpedienteEstudianteDto.TribunalReporteDto(
                            tr.getDocente().getUsuario().getNombres() + " " + tr.getDocente().getUsuario().getApellidos(),
                            tr.getCargo(),
                            tr.getDocente().getTitulo4toNivel()
                        )).collect(Collectors.toList()));
                }

                // Sustentaciones
                if (configService.isEnabled("seccion_sustentaciones")) {
                    List<Sustentacion> sustentaciones = sustentacionRepo.findAll().stream()
                        .filter(s -> s.getProyecto().getIdProyecto().equals(proyecto.getIdProyecto()))
                        .collect(Collectors.toList());
                    dto.setSustentaciones(sustentaciones.stream().map(s -> {
                        BigDecimal nota = evaluacionRepo.findAll().stream()
                            .filter(e -> e.getSustentacion().getIdSustentacion().equals(s.getIdSustentacion()))
                            .map(e -> e.getNotaFinal() != null ? e.getNotaFinal() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        long count = evaluacionRepo.findAll().stream()
                            .filter(e -> e.getSustentacion().getIdSustentacion().equals(s.getIdSustentacion()))
                            .count();
                        BigDecimal promedio = count > 0 ? nota.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : null;
                        return new ExpedienteEstudianteDto.SustentacionReporteDto(
                            s.getTipo(), s.getFecha(), s.getLugar(), promedio, s.getObservaciones());
                    }).collect(Collectors.toList()));
                }
            });

        return dto;
    }

    // ── REPORTE POR PERIODO ───────────────────────────────────────────────

    public ReportePeriodoDto getReportePeriodo(Integer idPeriodo, Integer idCarrera, String estado) {
        PeriodoTitulacion periodo = periodoRepo.findById(idPeriodo)
            .orElseThrow(() -> new RuntimeException("Periodo no encontrado"));

        List<ProyectoTitulacion> proyectos = proyectoRepo.findAll().stream()
            .filter(p -> p.getPeriodo().getIdPeriodo().equals(idPeriodo))
            .filter(p -> idCarrera == null || (p.getEleccion() != null
                && p.getEleccion().getEstudiante() != null
                && p.getEleccion().getEstudiante().getCarrera() != null
                && p.getEleccion().getEstudiante().getCarrera().getIdCarrera().equals(idCarrera)))
            .filter(p -> estado == null || estado.equalsIgnoreCase(p.getEstado()))
            .collect(Collectors.toList());

        ReportePeriodoDto dto = ReportePeriodoDto.builder()
            .periodo(periodo.getDescripcion())
            .fechaInicio(periodo.getFechaInicio())
            .fechaFin(periodo.getFechaFin())
            .totalEstudiantes(proyectos.size())
            .proyectosFinalizados((int) proyectos.stream().filter(p -> "FINALIZADO".equals(p.getEstado())).count())
            .proyectosEnProceso((int) proyectos.stream().filter(p -> "DESARROLLO".equals(p.getEstado())).count())
            .proyectosAnteproyecto((int) proyectos.stream().filter(p -> "ANTEPROYECTO".equals(p.getEstado())).count())
            .proyectosPredefensa((int) proyectos.stream().filter(p -> "PREDEFENSA".equals(p.getEstado())).count())
            .proyectosDefensa((int) proyectos.stream().filter(p -> "DEFENSA".equals(p.getEstado())).count())
            .proyectos(proyectos.stream().map(p -> new ReportePeriodoDto.ProyectoPeriodoDto(
                p.getEleccion() != null && p.getEleccion().getEstudiante() != null
                    ? p.getEleccion().getEstudiante().getUsuario().getNombres() + " "
                      + p.getEleccion().getEstudiante().getUsuario().getApellidos() : "-",
                p.getEleccion() != null && p.getEleccion().getEstudiante() != null
                    ? p.getEleccion().getEstudiante().getUsuario().getCedula() : "-",
                p.getTitulo(),
                p.getTipoTrabajo() != null ? p.getTipoTrabajo().getNombre() : "-",
                p.getEstado(),
                p.getDirector() != null ? p.getDirector().getUsuario().getNombres()
                    + " " + p.getDirector().getUsuario().getApellidos() : "Sin asignar",
                p.getEleccion() != null && p.getEleccion().getEstudiante() != null
                    && p.getEleccion().getEstudiante().getCarrera() != null
                    ? p.getEleccion().getEstudiante().getCarrera().getNombre() : "-"
            )).collect(Collectors.toList()))
            .build();

        return dto;
    }

    // ── ACTAS Y CONSTANCIAS ───────────────────────────────────────────────

    public List<ActaConstanciaDto> getActasTutorias(Integer idProyecto) {
        return actaRepo.findAll().stream()
            .filter(a -> a.getTutoria() != null
                && a.getTutoria().getAnteproyecto() != null)
            .map(a -> ActaConstanciaDto.builder()
                .tipoActa("TUTORIA")
                .estudiante(a.getEstudianteNombre())
                .tituloProyecto(a.getTituloProyecto())
                .director(a.getDirectorNombre())
                .fecha(a.getTutoria().getFecha())
                .hora(a.getTutoria().getHora())
                .modalidad(a.getTutoria().getModalidad())
                .objetivo(a.getObjetivo())
                .detalleRevision(a.getDetalleRevision())
                .cumplimiento(a.getCumplimiento())
                .conclusion(a.getConclusion())
                .observaciones(a.getObservaciones())
                .build())
            .collect(Collectors.toList());
    }

    public List<ActaConstanciaDto> getActasSustentacion(Integer idProyecto) {
        return sustentacionRepo.findAll().stream()
            .filter(s -> s.getProyecto().getIdProyecto().equals(idProyecto))
            .map(s -> {
                List<EvaluacionSustentacion> evals = evaluacionRepo.findAll().stream()
                    .filter(e -> e.getSustentacion().getIdSustentacion().equals(s.getIdSustentacion()))
                    .collect(Collectors.toList());

                BigDecimal promedio = evals.stream()
                    .filter(e -> e.getNotaFinal() != null)
                    .map(EvaluacionSustentacion::getNotaFinal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                long count = evals.stream().filter(e -> e.getNotaFinal() != null).count();
                BigDecimal notaFinal = count > 0
                    ? promedio.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : null;

                return ActaConstanciaDto.builder()
                    .tipoActa(s.getTipo())
                    .tituloProyecto(s.getProyecto().getTitulo())
                    .periodo(s.getProyecto().getPeriodo().getDescripcion())
                    .fecha(s.getFecha())
                    .hora(s.getHora())
                    .lugar(s.getLugar())
                    .observaciones(s.getObservaciones())
                    .notaFinalPromedio(notaFinal)
                    .evaluaciones(evals.stream().map(e ->
                        new ActaConstanciaDto.EvaluacionActaDto(
                            e.getDocente().getUsuario().getNombres() + " " + e.getDocente().getUsuario().getApellidos(),
                            tribunalRepo.findAll().stream()
                                .filter(t -> t.getProyecto().getIdProyecto().equals(idProyecto)
                                    && t.getDocente().getIdDocente().equals(e.getDocente().getIdDocente()))
                                .map(TribunalProyecto::getCargo).findFirst().orElse("-"),
                            e.getCalidadTrabajo(), e.getOriginalidad(),
                            e.getDominioTema(), e.getPreguntas(), e.getNotaFinal(),
                            e.getObservaciones()
                        )).collect(Collectors.toList()))
                    .build();
            }).collect(Collectors.toList());
    }

    public List<PeriodoTitulacion> getPeriodos() {
        return periodoRepo.findAll();
    }

    public List<Estudiante> getEstudiantes() {
        return estudianteRepo.findAll();
    }
}
