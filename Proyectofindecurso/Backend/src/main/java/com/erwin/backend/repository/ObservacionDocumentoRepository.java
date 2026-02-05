package com.erwin.backend.repository;

import com.erwin.backend.entities.ObservacionDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObservacionDocumentoRepository extends JpaRepository<ObservacionDocumento, Integer> {
    List<ObservacionDocumento> findByDocumento_IdOrderByCreadoEnDesc(Integer idDocumentoTitulacion);
}
