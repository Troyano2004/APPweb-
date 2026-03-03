
package com.erwin.backend.service;

import com.erwin.backend.dtos.UsuarioAdminDto;
import com.erwin.backend.dtos.UsuarioCreateRequest;
import com.erwin.backend.dtos.UsuarioEstadoRequest;
import com.erwin.backend.dtos.UsuarioUpdateRequest;
import com.erwin.backend.repository.UsuarioRepository;
import com.erwin.backend.repository.UsuarioSpRepository;
import com.erwin.backend.security.CryptoUtil;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class AdminUsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final UsuarioSpRepository usuarioSpRepo;
    private final PasswordEncoder passwordEncoder;

    //---------------------------------------------------
    // CONSTRUCTOR
    //---------------------------------------------------
    public AdminUsuarioService(
            UsuarioRepository usuarioRepo,
            UsuarioSpRepository usuarioSpRepo,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepo = usuarioRepo;
        this.usuarioSpRepo = usuarioSpRepo;
        this.passwordEncoder = passwordEncoder;
    }

    //---------------------------------------------------
    // LISTAR
    //---------------------------------------------------
    public List<UsuarioAdminDto> listar() {
        return usuarioSpRepo.listarUsuarios();
    }

    //---------------------------------------------------
    // CREAR USUARIO
    //---------------------------------------------------
    @Transactional
    public UsuarioAdminDto crear(UsuarioCreateRequest req) {

        validarCreate(req);

        String username = req.getUsername().trim();

        if (usuarioRepo.existsByUsername(username)) {
            throw new RuntimeException("El usuario ya existe");
        }

        //------------------------------------------------
        // PASSWORD APP → BCrypt
        //------------------------------------------------
        String passwordHash =
                passwordEncoder.encode(req.getPasswordApp().trim());

        //------------------------------------------------
        // ✅ GENERAR PASSWORD BD AUTOMÁTICA
        //------------------------------------------------
        String passwordDbPlain = generarPasswordSegura();

        //------------------------------------------------
        // ✅ CIFRAR PASSWORD BD
        //------------------------------------------------
        String passwordDbEncrypted =
                CryptoUtil.encrypt(passwordDbPlain);

        //------------------------------------------------
        // username_db = username
        //------------------------------------------------
        String usernameDb = username;

        Integer[] idsRolApp = req.getIdsRolApp();

        //------------------------------------------------
        // ✅ AHORA SE ENVÍAN LOS 3 VALORES
        //------------------------------------------------
        Integer newId = usuarioSpRepo.crearUsuarioV3(
                req.getCedula().trim(),
                req.getCorreoInstitucional() != null
                        ? req.getCorreoInstitucional().trim()
                        : null,
                username,
                passwordHash,
                req.getNombres().trim(),
                req.getApellidos().trim(),
                req.getActivo() != null ? req.getActivo() : true,
                idsRolApp,
                usernameDb,
                passwordDbEncrypted,
                passwordDbPlain   // ✅ NUEVO
        );

        UsuarioAdminDto dto =
                usuarioSpRepo.obtenerPorId(newId);

        if (dto == null) {
            throw new RuntimeException("Usuario no existe");
        }

        return dto;
    }

    //---------------------------------------------------
    // EDITAR
    //---------------------------------------------------
    @Transactional
    public UsuarioAdminDto editar(Integer id, UsuarioUpdateRequest req) {

        if (req == null)
            throw new RuntimeException("Body requerido");

        String nombres =
                req.getNombres() != null ? req.getNombres().trim() : null;

        String apellidos =
                req.getApellidos() != null ? req.getApellidos().trim() : null;

        String password = null;

        if (req.getPassword() != null &&
                !req.getPassword().trim().isEmpty()) {

            password =
                    passwordEncoder.encode(req.getPassword().trim());
        }

        Integer[] idsRolApp = req.getIdsRolApp();

        usuarioSpRepo.editarUsuarioV3(
                id,
                nombres,
                apellidos,
                req.getActivo(),
                password,
                idsRolApp
        );

        UsuarioAdminDto dto =
                usuarioSpRepo.obtenerPorId(id);

        if (dto == null)
            throw new RuntimeException("Usuario no existe");

        return dto;
    }

    //---------------------------------------------------
    // CAMBIAR ESTADO
    //---------------------------------------------------
    @Transactional
    public UsuarioAdminDto cambiarEstado(
            Integer id,
            UsuarioEstadoRequest req) {

        if (req == null || req.getActivo() == null) {
            throw new RuntimeException("Debe enviar activo=true/false");
        }

        usuarioSpRepo.cambiarEstado(id, req.getActivo());

        UsuarioAdminDto dto =
                usuarioSpRepo.obtenerPorId(id);

        if (dto == null)
            throw new RuntimeException("Usuario no existe");

        return dto;
    }

    //---------------------------------------------------
    // VALIDACIONES CREATE
    //---------------------------------------------------
    private void validarCreate(UsuarioCreateRequest req) {

        if (req == null)
            throw new RuntimeException("Body requerido");

        if (vacio(req.getCedula()))
            throw new RuntimeException("Cédula requerida");

        if (vacio(req.getUsername()))
            throw new RuntimeException("Username requerido");

        if (vacio(req.getPasswordApp()))
            throw new RuntimeException("Password App requerida");

        if (vacio(req.getNombres()))
            throw new RuntimeException("Nombres requeridos");

        if (vacio(req.getApellidos()))
            throw new RuntimeException("Apellidos requeridos");

        if (req.getIdsRolApp() == null ||
                req.getIdsRolApp().length == 0) {

            throw new RuntimeException(
                    "Seleccione al menos un Rol del Aplicativo");
        }
    }

    //---------------------------------------------------
    // GENERADOR PASSWORD SEGURA
    //---------------------------------------------------
    private String generarPasswordSegura() {

        String caracteres =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#";

        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < 12; i++) {
            sb.append(
                    caracteres.charAt(
                            random.nextInt(caracteres.length())
                    )
            );
        }

        return sb.toString();
    }

    //---------------------------------------------------
    private boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }
}