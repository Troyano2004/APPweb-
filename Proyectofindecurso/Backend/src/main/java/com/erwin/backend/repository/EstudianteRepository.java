package com.erwin.backend.repository;

import com.erwin.backend.entities.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> {
    // ✅ Todos los estudiantes de una carrera
    List<Estudiante> findByCarrera_IdCarrera(Integer idCarrera);

    // ✅ Estudiantes de la carrera que NO tienen tutor DT1 en ese periodo
    @Query("""
        select e
        from Estudiante e
        where e.carrera.idCarrera = :idCarrera
          and not exists (
              select 1
              from Dt1TutorEstudiante t
              where t.estudiante.idEstudiante = e.idEstudiante
                and t.periodo.idPeriodo = :idPeriodo
                and t.activo = true
          )
    """)
    List<Estudiante> findEstudiantesSinTutorDt1EnPeriodo(
            @Param("idCarrera") Integer idCarrera,
            @Param("idPeriodo") Integer idPeriodo
    );
}