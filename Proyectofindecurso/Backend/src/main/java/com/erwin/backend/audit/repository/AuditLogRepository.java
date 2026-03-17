package com.erwin.backend.audit.repository;
import com.erwin.backend.audit.entity.AuditLog;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    @Query("SELECT COUNT(l) FROM AuditLog l WHERE l.timestampEvento >= :desde")
    long countDesde(@Param("desde") LocalDateTime desde);

    @Query("SELECT l.entidad, COUNT(l) FROM AuditLog l WHERE l.timestampEvento >= :desde GROUP BY l.entidad ORDER BY COUNT(l) DESC")
    List<Object[]> topEntidades(@Param("desde") LocalDateTime desde);

    @Query("SELECT l.accion, COUNT(l) FROM AuditLog l WHERE l.timestampEvento >= :desde GROUP BY l.accion ORDER BY COUNT(l) DESC")
    List<Object[]> topAcciones(@Param("desde") LocalDateTime desde);

    @Query("SELECT l.username, COUNT(l) FROM AuditLog l WHERE l.timestampEvento >= :desde AND l.username IS NOT NULL GROUP BY l.username ORDER BY COUNT(l) DESC")
    List<Object[]> topUsuarios(@Param("desde") LocalDateTime desde);
}
