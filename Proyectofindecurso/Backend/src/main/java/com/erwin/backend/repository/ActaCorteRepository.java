package com.erwin.backend.repository;
import com.erwin.backend.entities.ActaCorte;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ActaCorteRepository extends JpaRepository<ActaCorte, Integer> {
    List<ActaCorte> findByProyecto_IdProyectoOrderByNumeroCorteAsc(Integer idProyecto);
    Optional<ActaCorte> findByProyecto_IdProyectoAndNumeroCorte(Integer idProyecto, Integer numeroCorte);
    boolean existsByProyecto_IdProyectoAndNumeroCorte(Integer idProyecto, Integer numeroCorte);
}