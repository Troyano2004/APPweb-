package com.erwin.backend.repository;

import com.erwin.backend.entities.BancoTemas;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BancoTemasRepository extends JpaRepository<BancoTemas, Integer> {
    List<BancoTemas> findByEstadoOrderByIdTemaDesc(String estado);
    List<BancoTemas> findByDocenteProponente_IdDocenteOrderByIdTemaDesc(Integer idDocente);
}
