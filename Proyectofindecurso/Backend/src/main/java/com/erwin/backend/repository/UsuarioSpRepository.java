
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
    // CREAR v4  ✅ 12 PARÁMETROS (soporta multi-rol con roles de distintas bases)
    //
    // Firma BD: sp_crear_usuario_v4(
    //   p_cedula, p_correo, p_username, p_password, p_nombres, p_apellidos,
    //   p_activo, p_ids_rol_app integer[], p_id_rol_app_principal integer,
    //   p_username_db, p_password_db_plain, p_password_db_encrypted
    // ) RETURNS integer
    // ====================================================
    public Integer crearUsuarioV4(
            String cedula,
            String correo,
            String username,
            String passwordHash,
            String nombres,
            String apellidos,
            Boolean activo,
            Integer[] idsRolApp,
            Integer idRolAppPrincipal,
            String usernameDb,
            String passwordDbPlain,
            String passwordDbEncrypted
    ) {
        if (idsRolApp == null || idsRolApp.length == 0) {
            throw new RuntimeException("Debe enviar al menos un rol (idsRolApp).");
        }
        if (idRolAppPrincipal == null) {
            throw new RuntimeException("Debe enviar el rol principal (idRolAppPrincipal).");
        }

        return jdbc.execute((Connection con) -> {

            Array sqlArray = con.createArrayOf("integer", idsRolApp);

            String sql = "SELECT public.sp_crear_usuario_v4(?,?,?,?,?,?,?,?,?,?,?,?)";

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
                    idRolAppPrincipal,
                    usernameDb,
                    passwordDbPlain,
                    passwordDbEncrypted
            );
        });
    }

    // ====================================================
    // ASIGNAR ROLES EXTRA a usuario ya existente
    // sp_asignar_roles_usuario(p_id_usuario integer, p_ids_rol_app integer[])
    // ====================================================
    public void asignarRolesUsuario(Integer idUsuario, Integer[] idsRolApp) {
        jdbc.execute((Connection con) -> {
            Array sqlArray = con.createArrayOf("integer", idsRolApp);
            String sql = "CALL public.sp_asignar_roles_usuario(?,?)";
            jdbc.update(sql, idUsuario, sqlArray);
            return null;
        });
    }

    // ====================================================
    // EDITAR (sin cambios en la firma)
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

            String sql = "SELECT public.sp_editar_usuario_v3(?,?,?,?,?,?)";

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
        String sql = "SELECT * FROM public.vw_usuario_admin_v3 ORDER BY id_usuario";
        return jdbc.query(sql, usuarioAdminMapperV3());
    }

    // ====================================================
    // OBTENER POR ID
    // ====================================================
    public UsuarioAdminDto obtenerPorId(Integer id) {
        String sql = "SELECT * FROM public.vw_usuario_admin_v3 WHERE id_usuario = ?";
        List<UsuarioAdminDto> list = jdbc.query(sql, usuarioAdminMapperV3(), id);
        return list.isEmpty() ? null : list.get(0);
    }

    // ====================================================
    // CAMBIAR ESTADO
    // ====================================================
    public void cambiarEstado(Integer id, Boolean activo) {
        String sql = "SELECT public.sp_cambiar_estado_usuario(?,?)";
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

            Integer idRolAppPrincipal = (Integer) rs.getObject("id_rol_app_principal");
            dto.setIdRolApp(idRolAppPrincipal);
            dto.setRolApp(rs.getString("rol_app_principal"));
            dto.setRolesApp(rs.getString("roles_app"));

            Array arr = rs.getArray("ids_rol_app");
            if (arr != null) {
                dto.setIdsRolApp((Integer[]) arr.getArray());
            } else {
                dto.setIdsRolApp(new Integer[0]);
            }

            if (dto.getRolApp() == null || dto.getRolApp().isBlank()) {
                dto.setRolApp(dto.getRolesApp());
            }

            return dto;
        };
    }
}