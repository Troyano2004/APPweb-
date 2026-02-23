package com.erwin.backend.repository;

import com.erwin.backend.entities.CierreTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CierreTitulacionRepository extends JpaRepository<CierreTitulacion, Integer> {
    Optional<CierreTitulacion> findByDocumento_Id(Integer idDocumento);
}