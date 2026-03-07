package com.erwin.backend.repository;
import com.erwin.backend.entities.AsesoriaDirector;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AsesoriaDirectorRepository extends JpaRepository<AsesoriaDirector, Integer> {
    List<AsesoriaDirector> findByProyecto_IdProyectoOrderByFechaDesc(Integer idProyecto);
    List<AsesoriaDirector> findByProyecto_IdProyectoAndNumeroCorteOrderByFechaDesc(Integer idProyecto, Integer numeroCorte);
    long countByProyecto_IdProyectoAndNumeroCorte(Integer idProyecto, Integer numeroCorte);
    /** Proyectos donde el docente es director y tienen asesorías registradas. */
    List<AsesoriaDirector> findDistinctByDirector_IdDocenteOrderByFechaDesc(Integer idDocente);
}