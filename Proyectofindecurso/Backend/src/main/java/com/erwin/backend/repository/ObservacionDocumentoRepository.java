package com.erwin.backend.repository;

import com.erwin.backend.entities.ObservacionDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ObservacionDocumentoRepository extends JpaRepository<ObservacionDocumento, Integer> {
    List<ObservacionDocumento> findByDocumento_IdOrderByCreadoEnDesc(Integer idDocumentoTitulacion);
    List<ObservacionDocumento> findAllByOrderByCreadoEnDesc(Pageable pageable);
}
