package com.erwin.backend.repository;

import com.erwin.backend.dtos.UsuarioAdminDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.Connection;
import java.util.List;

@Repository
public class UsuarioSpRepository {

    private final JdbcTemplate jdbc;

    public UsuarioSpRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ====================================================
    // CREAR v3  ✅ 11 PARÁMETROS
    // ====================================================
    public Integer crearUsuarioV3(
            String cedula,
            String correo,
            String username,
            String passwordHash,
            String nombres,
            String apellidos,
            Boolean activo,
            Integer[] idsRolApp,
            String usernameDb,
            String passwordDbEncrypted,
            String passwordDbPlain
    ) {

        if (idsRolApp == null || idsRolApp.length == 0) {
            throw new RuntimeException(
                    "Debe enviar al menos un rol (idsRolApp).");
        }

        return jdbc.execute((Connection con) -> {

            Array sqlArray =
                    con.createArrayOf("integer", idsRolApp);

            // ✅ AHORA 11 ?
            String sql =
                    "SELECT public.sp_crear_usuario_v5(?,?,?,?,?,?,?,?,?,?,?)";

            return jdbc.queryForObject(
                    sql,
                    Integer.class,
                    cedula,
                    correo,
                    username,
                    passwordHash,
                    nombres,
                    apellidos,
                    activo,
                    sqlArray,
                    usernameDb,
                    passwordDbEncrypted,
                    passwordDbPlain   // ✅ NUEVO
            );
        });
    }

    // ====================================================
    // EDITAR
    // ====================================================
    public void editarUsuarioV3(Integer idUsuario,
                                String nombres,
                                String apellidos,
                                Boolean activo,
                                String password,
                                Integer[] idsRolApp) {

        jdbc.execute((Connection con) -> {

            Array sqlArray =
                    (idsRolApp == null)
                            ? null
                            : con.createArrayOf("integer", idsRolApp);

            String sql =
                    "SELECT public.sp_editar_usuario_v3(?,?,?,?,?,?)";

            jdbc.queryForObject(
                    sql,
                    Object.class,
                    idUsuario,
                    nombres,
                    apellidos,
                    activo,
                    password,
                    sqlArray
            );

            return null;
        });
    }

    // ====================================================
    // LISTAR
    // ====================================================
    public List<UsuarioAdminDto> listarUsuarios() {
        String sql =
                "SELECT * FROM public.vw_usuario_admin_v3 ORDER BY id_usuario";
        return jdbc.query(sql, usuarioAdminMapperV3());
    }

    // ====================================================
    // OBTENER POR ID
    // ====================================================
    public UsuarioAdminDto obtenerPorId(Integer id) {

        String sql =
                "SELECT * FROM public.vw_usuario_admin_v3 WHERE id_usuario = ?";

        List<UsuarioAdminDto> list =
                jdbc.query(sql, usuarioAdminMapperV3(), id);

        return list.isEmpty() ? null : list.get(0);
    }

    // ====================================================
    // CAMBIAR ESTADO
    // ====================================================
    public void cambiarEstado(Integer id, Boolean activo) {
        String sql =
                "SELECT public.sp_cambiar_estado_usuario(?,?)";

        jdbc.queryForObject(sql, Object.class, id, activo);
    }

    // ====================================================
    // MAPPER
    // ====================================================
    private RowMapper<UsuarioAdminDto> usuarioAdminMapperV3() {

        return (rs, rowNum) -> {

            UsuarioAdminDto dto = new UsuarioAdminDto();

            dto.setIdUsuario(rs.getInt("id_usuario"));
            dto.setUsername(rs.getString("username"));
            dto.setNombres(rs.getString("nombres"));
            dto.setApellidos(rs.getString("apellidos"));
            dto.setActivo(rs.getBoolean("activo"));

            Integer idRolAppPrincipal =
                    (Integer) rs.getObject("id_rol_app_principal");

            dto.setIdRolApp(idRolAppPrincipal);
            dto.setRolApp(rs.getString("rol_app_principal"));

            dto.setRolesApp(rs.getString("roles_app"));

            Array arr = rs.getArray("ids_rol_app");

            if (arr != null) {
                dto.setIdsRolApp((Integer[]) arr.getArray());
            } else {
                dto.setIdsRolApp(new Integer[0]);
            }

            if (dto.getRolApp() == null ||
                    dto.getRolApp().isBlank()) {

                dto.setRolApp(dto.getRolesApp());
            }

            return dto;
        };
    }
}