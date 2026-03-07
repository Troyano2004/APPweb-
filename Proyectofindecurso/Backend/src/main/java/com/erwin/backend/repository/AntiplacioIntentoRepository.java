package com.erwin.backend.repository;
import com.erwin.backend.entities.AntiplacioIntento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface AntiplacioIntentoRepository extends JpaRepository<AntiplacioIntento, Integer> {
    List<AntiplacioIntento> findByProyecto_IdProyectoOrderByFechaIntentoDesc(Integer idProyecto);
    /** Último intento favorable (certificado vigente). */
    Optional<AntiplacioIntento> findFirstByProyecto_IdProyectoAndFavorableTrueOrderByFechaIntentoDesc(Integer idProyecto);
    boolean existsByProyecto_IdProyectoAndFavorableTrue(Integer idProyecto);
}