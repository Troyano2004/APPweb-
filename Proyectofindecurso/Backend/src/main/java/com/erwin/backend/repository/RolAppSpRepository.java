
package com.erwin.backend.repository;

import com.erwin.backend.dtos.RolAppDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Repository
public class RolAppSpRepository {

    @PersistenceContext
    private EntityManager em;

    // ✅ CORREGIDO: la query ahora también trae id_rol_base
    public List<RolAppDto> listarRolesApp() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT ra.id_rol_app, ra.nombre, ra.descripcion, ra.activo, " +
                        "COALESCE(ARRAY(SELECT p.codigo FROM public.permisos p " +
                        "  JOIN public.rol_app_permiso rap ON rap.id_permiso = p.id_permiso " +
                        "  WHERE rap.id_rol_app = ra.id_rol_app), '{}'), " +
                        "ra.id_rol_base " +
                        "FROM public.rol_app ra " +
                        "ORDER BY ra.id_rol_app"
        ).getResultList();

        List<RolAppDto> out = new ArrayList<>();
        for (Object[] r : rows) {
            Integer idRolBase = r[5] != null ? ((Number) r[5]).intValue() : null;
            out.add(new RolAppDto(
                    ((Number) r[0]).intValue(),
                    (String)  r[1],
                    (String)  r[2],
                    (Boolean) r[3],
                    toStringList(r[4]),
                    idRolBase   // ✅ NUEVO
            ));
        }
        return out;
    }

    // ================== CREAR ==================
    public Integer crearRolApp(String nombre, String descripcion, Boolean activo,
                               List<Integer> permisos, Integer idRolBase) {
        String permisosLiteral = toPgIntArrayLiteral(permisos);
        Object result = em.createNativeQuery(
                        "SELECT sp_crear_rol_app(?1, ?2, ?3, CAST(?4 AS int4[]), ?5)"
                )
                .setParameter(1, nombre)
                .setParameter(2, descripcion)
                .setParameter(3, activo)
                .setParameter(4, permisosLiteral)
                .setParameter(5, idRolBase)
                .getSingleResult();
        return ((Number) result).intValue();
    }

    // ================== EDITAR ==================
    public void editarRolApp(Integer id, String nombre, String descripcion,
                             Boolean activo, Integer idRolBase) {
        em.createNativeQuery(
                        "SELECT 1 FROM (SELECT sp_editar_rol_app(?1, ?2, ?3, ?4, ?5)) AS _t"
                )
                .setParameter(1, id)
                .setParameter(2, nombre)
                .setParameter(3, descripcion)
                .setParameter(4, activo)
                .setParameter(5, idRolBase)
                .getSingleResult();
    }

    // ================== CAMBIAR ESTADO ==================
    public void cambiarEstadoRolApp(Integer id, Boolean activo) {
        em.createNativeQuery(
                        "SELECT 1 FROM (SELECT sp_cambiar_estado_rol_app(?1, ?2)) AS _t"
                )
                .setParameter(1, id)
                .setParameter(2, activo)
                .getSingleResult();
    }

    // ================== ASIGNAR PERMISOS ==================
    public void asignarPermisosRolApp(Integer id, List<Integer> permisos) {
        String permisosLiteral = toPgIntArrayLiteral(permisos);
        em.createNativeQuery(
                        "SELECT 1 FROM (SELECT sp_asignar_permisos_rol_app(?1, CAST(?2 AS int4[]))) AS _t"
                )
                .setParameter(1, id)
                .setParameter(2, permisosLiteral)
                .getSingleResult();
    }

    // ================== helpers ==================
    private String toPgIntArrayLiteral(List<Integer> permisos) {
        if (permisos == null || permisos.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < permisos.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(permisos.get(i));
        }
        sb.append("}");
        return sb.toString();
    }

    private List<String> toStringList(Object arrayValue) {
        if (arrayValue == null) return Collections.emptyList();
        try {
            if (arrayValue instanceof java.sql.Array sqlArray) {
                Object raw = sqlArray.getArray();
                if (raw instanceof Object[] arr) {
                    List<String> out = new ArrayList<>();
                    for (Object v : arr) out.add(String.valueOf(v));
                    return out;
                }
            }
            if (arrayValue instanceof String[] arr) return Arrays.asList(arr);
            if (arrayValue instanceof Object[] arr) {
                List<String> out = new ArrayList<>();
                for (Object v : arr) out.add(String.valueOf(v));
                return out;
            }
            if (arrayValue instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object v : list) out.add(String.valueOf(v));
                return out;
            }
            return Collections.singletonList(String.valueOf(arrayValue));
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo leer array permisos", e);
        }
    }
}