package com.erwin.backend.controller;

import com.erwin.backend.dtos.CoordinadorDtos.*;
import com.erwin.backend.dtos.DocumentoTitulacionDto;
import com.erwin.backend.entities.ComisionProyecto;
import com.erwin.backend.service.CoordinadorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coordinador")
@CrossOrigin(origins = "http://localhost:4200")
public class CoordinadorController {
    private final CoordinadorService service;

    public CoordinadorController(CoordinadorService service) {
        this.service = service;
    }

    @GetMapping("/seguimiento")
    public List<SeguimientoProyectoDto> seguimiento() {
        return service.seguimiento();
    }

    @GetMapping("/directores/sin-asignar")
    public List<EstudianteSinDirectorDto> sinDirector() {
        return service.estudiantesSinDirector();
    }

    @GetMapping("/directores/carga")
    public List<DirectorCargaDto> cargaDirectores() {
        return service.cargaDirectores();
    }

    @PostMapping("/directores/asignar")
    public void asignarDirector(@RequestBody AsignarDirectorRequest request) {
        service.asignarDirector(request);
    }

    @PostMapping("/validacion/{idProyecto}")
    public void validarProyecto(@PathVariable Integer idProyecto) {
        service.validarProyecto(idProyecto);
    }

    @GetMapping("/observaciones")
    public List<ObservacionAdministrativaDto> observaciones(@RequestParam(required = false) Integer idProyecto) {
        return service.observaciones(idProyecto);
    }

    @GetMapping("/proyecto/{idProyecto}/documento")
    public DocumentoTitulacionDto documentoProyecto(@PathVariable Integer idProyecto) {
        return service.documentoPorProyecto(idProyecto);
    }

    @PostMapping("/observaciones")
    public ObservacionAdministrativaDto crearObservacion(@RequestBody CrearObservacionAdministrativaRequest request) {
        return service.crearObservacion(request);
    }

    @GetMapping("/comisiones")
    public List<ComisionFormativaDto> comisiones() {
        return service.listarComisiones();
    }

    @GetMapping("/catalogos/carreras")
    public List<CatalogoCarreraDto> carreras() {
        return service.carreras();
    }

    @DeleteMapping("/comisiones/{idComision}")
    public void eliminarComision(@PathVariable Integer idComision) {
        service.eliminarComision(idComision);
    }

    @PostMapping("/comisiones")
    public ComisionFormativaDto crearComision(@RequestBody CrearComisionRequest request) {
        return service.crearComision(request);
    }

    @PostMapping("/comisiones/{idComision}/miembros")
    public void asignarMiembros(@PathVariable Integer idComision,
                                @RequestBody AsignarMiembrosRequest request) {
        service.asignarMiembros(idComision, request);
    }

    @PostMapping("/comisiones/asignar-proyecto")
    public void asignarComisionProyecto(@RequestBody AsignarComisionProyectoRequest request) {
        service.asignarComisionAProyecto(request);
    }

    @GetMapping("/comisiones/proyecto/{idProyecto}")
    public ComisionProyecto comisionPorProyecto(@PathVariable Integer idProyecto) {
        return service.comisionPorProyecto(idProyecto)
                .orElseThrow(() -> new RuntimeException("Comisión no encontrada"));
    }
}
