package com.erwin.backend.service;

import com.erwin.backend.dtos.TipoTrabajoTitulacionDto;
import com.erwin.backend.entities.Modalidadtitulacion;
import com.erwin.backend.entities.Tipotrabajotitulacion;
import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.repository.ModalidadTitulacionRepository;
import com.erwin.backend.repository.TipoTrabajoTitulacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TipoTrabajoTitulacionService {

    private final TipoTrabajoTitulacionRepository tipoTrabajoRepo;
    private final ModalidadTitulacionRepository modalidadRepo;

    public TipoTrabajoTitulacionService(
            TipoTrabajoTitulacionRepository tipoTrabajoRepo,
            ModalidadTitulacionRepository modalidadRepo) {
        this.tipoTrabajoRepo = tipoTrabajoRepo;
        this.modalidadRepo = modalidadRepo;
    }

    public List<TipoTrabajoTitulacionDto> listarTodos() {
        return tipoTrabajoRepo.findAll().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public List<TipoTrabajoTitulacionDto> listarPorModalidad(Integer idModalidad) {
        return tipoTrabajoRepo.findByModalidadTitulacionIdModalidad(idModalidad).stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public TipoTrabajoTitulacionDto obtenerPorId(Integer id) {
        Tipotrabajotitulacion tipo = tipoTrabajoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de trabajo no encontrado"));
        return convertirADto(tipo);
    }

    @Auditable(entidad = "TipoTrabajoTitulacion", accion = "CREATE", capturarArgs = true)
    public TipoTrabajoTitulacionDto crear(TipoTrabajoTitulacionDto dto) {
        Modalidadtitulacion modalidad = modalidadRepo.findById(dto.getIdModalidad())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));
        
        Tipotrabajotitulacion tipo = new Tipotrabajotitulacion();
        tipo.setNombre(dto.getNombre());
        tipo.setModalidadTitulacion(modalidad);
        
        Tipotrabajotitulacion guardado = tipoTrabajoRepo.save(tipo);
        return convertirADto(guardado);
    }

    @Auditable(entidad = "TipoTrabajoTitulacion", accion = "UPDATE", capturarArgs = true)
    public TipoTrabajoTitulacionDto actualizar(Integer id, TipoTrabajoTitulacionDto dto) {
        Tipotrabajotitulacion tipo = tipoTrabajoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de trabajo no encontrado"));
        
        Modalidadtitulacion modalidad = modalidadRepo.findById(dto.getIdModalidad())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));
        
        tipo.setNombre(dto.getNombre());
        tipo.setModalidadTitulacion(modalidad);
        
        Tipotrabajotitulacion actualizado = tipoTrabajoRepo.save(tipo);
        return convertirADto(actualizado);
    }

    @Auditable(entidad = "TipoTrabajoTitulacion", accion = "DELETE", capturarArgs = false)
    public void eliminar(Integer id) {
        if (!tipoTrabajoRepo.existsById(id)) {
            throw new RuntimeException("Tipo de trabajo no encontrado");
        }
        tipoTrabajoRepo.deleteById(id);
    }

    private TipoTrabajoTitulacionDto convertirADto(Tipotrabajotitulacion t) {
        return new TipoTrabajoTitulacionDto(
            t.getIdTipoTrabajo(),
            t.getNombre(),
            t.getModalidadTitulacion() != null ? t.getModalidadTitulacion().getIdModalidad() : null,
            t.getModalidadTitulacion() != null ? t.getModalidadTitulacion().getNombre() : null
        );
    }
}
