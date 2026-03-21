package com.erwin.backend.service;

import com.erwin.backend.dtos.SemanaPredefensaDtos;
import com.erwin.backend.dtos.Dt2Dtos;
import com.erwin.backend.dtos.ExtenderSemanaRequest;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanaPredefensaService {

    private final SemanaPredefensaRepository semanaRepo;
    private final SustentacionRepository     sustentacionRepo;
    private final PeriodoTitulacionRepository periodoRepo;
    private final ProyectoTitulacionRepository proyectoRepo;
    private final Dt2Service                 dt2Service;

    // ── Guardar o actualizar la semana de predefensas ──────────────────────────

    // ── Extender / modificar semana SIN borrar slots existentes ───────────────
    @Transactional
    public SemanaPredefensaDtos.SemanaPredefensaDto extenderSemana(ExtenderSemanaRequest req) {

        SemanaPredefensa semana = semanaRepo.findTopByActivoTrueOrderByIdSemanaDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay semana de predefensas activa para modificar"));

        LocalDate nuevaFechaFin   = req.getFechaFin()        != null ? req.getFechaFin()        : semana.getFechaFin();
        LocalTime nuevaHoraInicio = req.getHoraInicio()      != null ? req.getHoraInicio()      : semana.getHoraInicio();
        LocalTime nuevaHoraFin    = req.getHoraFin()         != null ? req.getHoraFin()         : semana.getHoraFin();
        Integer   nuevaDuracion   = req.getDuracionMinutos() != null ? req.getDuracionMinutos() : semana.getDuracionMinutos();

        if (nuevaFechaFin.isBefore(semana.getFechaInicio()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha fin no puede ser anterior a la fecha de inicio (" + semana.getFechaInicio() + ")");

        if (nuevaHoraInicio.isAfter(nuevaHoraFin))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La hora de inicio debe ser anterior a la hora de fin");

        if (nuevaDuracion < 15)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La duración mínima es 15 minutos");

        // Los slots ya asignados NO se tocan — solo se amplía/modifica la configuración
        List<Sustentacion> ocupados = sustentacionRepo
                .findByTipoAndFechaBetween("PREDEFENSA", semana.getFechaInicio(), semana.getFechaFin());
        if (!ocupados.isEmpty() && !nuevaDuracion.equals(semana.getDuracionMinutos())) {
            log.warn("Cambiando duración de {} a {} min con {} slots asignados — slots existentes mantienen su hora",
                    semana.getDuracionMinutos(), nuevaDuracion, ocupados.size());
        }

        semana.setFechaFin(nuevaFechaFin);
        semana.setHoraInicio(nuevaHoraInicio);
        semana.setHoraFin(nuevaHoraFin);
        semana.setDuracionMinutos(nuevaDuracion);
        if (req.getLugarDefecto()  != null) semana.setLugarDefecto(req.getLugarDefecto());
        if (req.getObservaciones() != null) semana.setObservaciones(req.getObservaciones());

        semana = semanaRepo.save(semana);
        log.info("Semana modificada: {} al {}, {}–{}, {} min",
                semana.getFechaInicio(), semana.getFechaFin(),
                semana.getHoraInicio(), semana.getHoraFin(), semana.getDuracionMinutos());

        return toDto(semana);
    }

    @Transactional
    public SemanaPredefensaDtos.SemanaPredefensaDto guardarSemana(
            SemanaPredefensaDtos.GuardarSemanaRequest req) {

        if (req.getFechaInicio().isAfter(req.getFechaFin())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de inicio debe ser anterior a la fecha de fin");
        }
        if (req.getHoraInicio().isAfter(req.getHoraFin())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La hora de inicio debe ser anterior a la hora de fin");
        }
        if (req.getDuracionMinutos() == null || req.getDuracionMinutos() < 15) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La duración mínima por predefensa es 15 minutos");
        }

        // Desactivar semana anterior si existe
        semanaRepo.findTopByActivoTrueOrderByIdSemanaDesc().ifPresent(s -> {
            s.setActivo(false);
            semanaRepo.save(s);
        });

        SemanaPredefensa semana = new SemanaPredefensa();
        semana.setFechaInicio(req.getFechaInicio());
        semana.setFechaFin(req.getFechaFin());
        semana.setHoraInicio(req.getHoraInicio());
        semana.setHoraFin(req.getHoraFin());
        semana.setDuracionMinutos(req.getDuracionMinutos());
        semana.setLugarDefecto(req.getLugarDefecto());
        semana.setObservaciones(req.getObservaciones());
        semana.setActivo(true);

        if (req.getIdPeriodo() != null) {
            periodoRepo.findById(req.getIdPeriodo()).ifPresent(semana::setPeriodo);
        }

        semana = semanaRepo.save(semana);
        log.info("Semana de predefensas configurada: {} al {}", req.getFechaInicio(), req.getFechaFin());

        return toDto(semana);
    }

    // ── Obtener la semana activa ───────────────────────────────────────────────

    public SemanaPredefensaDtos.SemanaPredefensaDto obtenerSemanaActiva() {
        return semanaRepo.findTopByActivoTrueOrderByIdSemanaDesc()
                .map(this::toDto)
                .orElse(null);
    }

    // ── Generar el calendario completo con slots ───────────────────────────────

    @Transactional
    public SemanaPredefensaDtos.CalendarioSemanaDto obtenerCalendario() {
        SemanaPredefensa semana = semanaRepo.findTopByActivoTrueOrderByIdSemanaDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay semana de predefensas configurada"));

        // Obtener todas las sustentaciones de tipo PREDEFENSA en el rango de fechas
        List<Sustentacion> predefensas = sustentacionRepo
                .findByTipoAndFechaBetween("PREDEFENSA", semana.getFechaInicio(), semana.getFechaFin());

        List<SemanaPredefensaDtos.DiaCalendarioDto> dias = new ArrayList<>();

        LocalDate cursor = semana.getFechaInicio();
        while (!cursor.isAfter(semana.getFechaFin())) {
            final LocalDate diaActual = cursor;

            // Solo días hábiles (lunes a viernes)
            int dow = cursor.getDayOfWeek().getValue(); // 1=lun, 7=dom
            if (dow <= 5) {
                List<SemanaPredefensaDtos.SlotDto> slots =
                        generarSlots(diaActual, semana, predefensas);

                long ocupados = slots.stream().filter(SemanaPredefensaDtos.SlotDto::isOcupado).count();

                SemanaPredefensaDtos.DiaCalendarioDto dia = new SemanaPredefensaDtos.DiaCalendarioDto(
                        diaActual,
                        cursor.getDayOfWeek().getDisplayName(TextStyle.FULL,
                                new Locale("es", "EC")),
                        slots,
                        slots.size(),
                        (int) ocupados,
                        (int) (slots.size() - ocupados)
                );
                dias.add(dia);
            }
            cursor = cursor.plusDays(1);
        }

        return new SemanaPredefensaDtos.CalendarioSemanaDto(toDto(semana), dias);
    }

    // ── Asignar slot a un proyecto (llama al programarPredefensa del Dt2Service) ─

    @Transactional
    public Dt2Dtos.MensajeDto asignarSlot(SemanaPredefensaDtos.AsignarSlotRequest req) {
        SemanaPredefensa semana = semanaRepo.findTopByActivoTrueOrderByIdSemanaDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No hay semana de predefensas configurada"));

        // Verificar que la fecha está dentro del rango
        if (req.getFecha().isBefore(semana.getFechaInicio())
                || req.getFecha().isAfter(semana.getFechaFin())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha seleccionada está fuera del rango de la semana de predefensas");
        }

        // Verificar que el slot no está ya ocupado
        boolean ocupado = sustentacionRepo
                .findByTipoAndFechaBetween("PREDEFENSA", req.getFecha(), req.getFecha())
                .stream()
                .anyMatch(s -> s.getHora().equals(req.getHora()));

        if (ocupado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El slot seleccionado ya está ocupado. Por favor elige otro horario.");
        }

        // Usar el lugar del slot o el lugar por defecto de la semana
        String lugar = (req.getLugar() != null && !req.getLugar().isBlank())
                ? req.getLugar()
                : (semana.getLugarDefecto() != null ? semana.getLugarDefecto() : "Por definir");

        Dt2Dtos.ProgramarPredefensaRequest predefensaReq = new Dt2Dtos.ProgramarPredefensaRequest();
        predefensaReq.setIdProyecto(req.getIdProyecto());
        predefensaReq.setFecha(req.getFecha());
        predefensaReq.setHora(req.getHora());
        predefensaReq.setLugar(lugar);
        predefensaReq.setObservaciones(req.getObservaciones());
        predefensaReq.setIdRealizadoPor(req.getIdRealizadoPor());

        return dt2Service.programarPredefensa(predefensaReq);
    }

    // ── Generar slots para un día ──────────────────────────────────────────────

    private List<SemanaPredefensaDtos.SlotDto> generarSlots(
            LocalDate fecha,
            SemanaPredefensa semana,
            List<Sustentacion> predefensas) {

        List<SemanaPredefensaDtos.SlotDto> slots = new ArrayList<>();
        LocalTime cursor = semana.getHoraInicio();
        int duracion     = semana.getDuracionMinutos();

        while (cursor.plusMinutes(duracion).compareTo(semana.getHoraFin()) <= 0) {
            final LocalTime horaSlot = cursor;

            // Buscar si hay una predefensa en este slot
            Optional<Sustentacion> ocupante = predefensas.stream()
                    .filter(s -> s.getFecha().equals(fecha) && s.getHora().equals(horaSlot))
                    .findFirst();

            SemanaPredefensaDtos.SlotDto slot = new SemanaPredefensaDtos.SlotDto(
                    fecha,
                    horaSlot,
                    horaSlot.plusMinutes(duracion),
                    ocupante.isPresent(),
                    null, null, null, null, null
            );

            ocupante.ifPresent(sus -> {
                slot.setIdSustentacion(sus.getIdSustentacion());
                slot.setLugar(sus.getLugar());
                if (sus.getProyecto() != null) {
                    slot.setIdProyecto(sus.getProyecto().getIdProyecto());
                    slot.setTituloProyecto(sus.getProyecto().getTitulo());
                    if (sus.getProyecto().getPropuesta() != null
                            && sus.getProyecto().getPropuesta().getEstudiante() != null
                            && sus.getProyecto().getPropuesta().getEstudiante().getUsuario() != null) {
                        Usuario u = sus.getProyecto().getPropuesta().getEstudiante().getUsuario();
                        slot.setEstudiante(
                                ((u.getNombres() != null ? u.getNombres() : "") + " "
                                        + (u.getApellidos() != null ? u.getApellidos() : "")).trim()
                        );
                    }
                }
            });

            slots.add(slot);
            cursor = cursor.plusMinutes(duracion);
        }

        return slots;
    }

    // ── Helper mapper ──────────────────────────────────────────────────────────

    private SemanaPredefensaDtos.SemanaPredefensaDto toDto(SemanaPredefensa s) {
        // Contar slots y ocupados
        List<Sustentacion> predefensas = sustentacionRepo
                .findByTipoAndFechaBetween("PREDEFENSA", s.getFechaInicio(), s.getFechaFin());

        int totalSlots = 0;
        LocalDate cursor = s.getFechaInicio();
        while (!cursor.isAfter(s.getFechaFin())) {
            if (cursor.getDayOfWeek().getValue() <= 5) {
                long minutos = java.time.Duration.between(s.getHoraInicio(), s.getHoraFin()).toMinutes();
                totalSlots += (int) (minutos / s.getDuracionMinutos());
            }
            cursor = cursor.plusDays(1);
        }

        return new SemanaPredefensaDtos.SemanaPredefensaDto(
                s.getIdSemana(),
                s.getFechaInicio(),
                s.getFechaFin(),
                s.getHoraInicio(),
                s.getHoraFin(),
                s.getDuracionMinutos(),
                s.getLugarDefecto(),
                s.getObservaciones(),
                s.getPeriodo() != null ? s.getPeriodo().getDescripcion() : null,
                s.getActivo(),
                totalSlots,
                predefensas.size(),
                totalSlots - predefensas.size()
        );
    }
}