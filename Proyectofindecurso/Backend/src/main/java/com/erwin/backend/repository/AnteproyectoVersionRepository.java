package com.erwin.backend.repository;

import com.erwin.backend.entities.Anteproyectotitulacionversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnteproyectoVersionRepository extends JpaRepository<Anteproyectotitulacionversion, Integer> {
    List<Anteproyectotitulacionversion> findByAnteproyecto_IdAnteproyectoOrderByNumeroVersionAsc(Integer IdAnteproyecto);
    @Query("select coalesce(max(v.numeroVersion),0) from Anteproyectotitulacionversion v where v.anteproyecto.idAnteproyecto = :idAnteproyecto")
    Integer maxNumeroVersion(Integer idAnteproyecto);
    // ✅ ÚLTIMA versión
    Optional<Anteproyectotitulacionversion> findTopByAnteproyecto_IdAnteproyectoOrderByNumeroVersionDesc(Integer idAnteproyecto);
    Optional<Anteproyectotitulacionversion>
    findFirstByAnteproyecto_IdAnteproyectoOrderByNumeroVersionDesc(Integer idAnteproyecto);


}
