package com.erwin.backend.controller;

import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.RolSistema;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.CarreraRepository;
import com.erwin.backend.repository.EstudianteRepository;
import com.erwin.backend.entities.Loginaplicativo;
import com.erwin.backend.entities.Loginbd;
import com.erwin.backend.repository.LoginAplicativoRepository;
import com.erwin.backend.repository.LoginBdRepository;
import com.erwin.backend.repository.RolesSistemaRepository;
import com.erwin.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/estudiantes")
@CrossOrigin(origins = "http://localhost:4200")
public class EstudianteController {

    private static final String ROL_ESTUDIANTE = "ESTUDIANTE";

    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarreraRepository carreraRepository;
    private final RolesSistemaRepository rolesSistemaRepository;
    private final LoginAplicativoRepository loginAplicativoRepository;
    private final LoginBdRepository loginBdRepository;

    public EstudianteController(EstudianteRepository estudianteRepository,
                                UsuarioRepository usuarioRepository,
                                CarreraRepository carreraRepository,
                                RolesSistemaRepository rolesSistemaRepository,
                                LoginAplicativoRepository loginAplicativoRepository,
                                LoginBdRepository loginBdRepository) {
        this.estudianteRepository = estudianteRepository;
        this.usuarioRepository = usuarioRepository;
        this.carreraRepository = carreraRepository;
        this.rolesSistemaRepository = rolesSistemaRepository;
        this.loginAplicativoRepository = loginAplicativoRepository;
        this.loginBdRepository = loginBdRepository;
    }

    @GetMapping
    public List<Estudiante> listarEstudiantes() {
        return estudianteRepository.findAll();
    }

    @PostMapping
    public Estudiante crear(@RequestBody Estudiante estudiante) {
        return estudianteRepository.save(estudiante);
    }

    @PostMapping("/crear-completo")
    public Estudiante crearCompleto(@RequestBody Estudiante estudiante) {
        return estudianteRepository.save(estudiante);
    }

    /**
     * Crea un estudiante de prueba listo para flujo E2E.
     *
     * Puedes enviar body opcional:
     * {
     *   "nombres": "Ana",
     *   "apellidos": "Pérez",
     *   "cedula": "0102030405",
     *   "correoInstitucional": "ana.perez@demo.edu.ec",
     *   "username": "ana.perez",
     *   "promedioRecord80": 9.12,
     *   "discapacidad": false,
     *   "idCarrera": 1
     * }
     */
    @PostMapping("/crear-demo")
    @Transactional
    public Estudiante crearEstudianteDemo(@RequestBody(required = false) CrearDemoEstudianteRequest req) {
        CrearDemoEstudianteRequest request = req == null ? new CrearDemoEstudianteRequest() : req;
        RolSistema rolEstudiante = resolverOCrearRolEstudiante();

        Carrera carrera = resolverCarrera(request.getIdCarrera());
        String unico = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Usuario usuario = new Usuario();
        usuario.setCedula(valorOGenerado(request.getCedula(), "999" + unico.substring(0, 7)));
        usuario.setCorreoInstitucional(valorOGenerado(request.getCorreoInstitucional(), "estudiante." + unico + "@demo.edu.ec"));
        usuario.setRol(ROL_ESTUDIANTE);
        usuario.setUsername(valorOGenerado(request.getUsername(), "estudiante_demo_" + unico));
        String passwordDemo = valorOGenerado(request.getPassword(), "demo123");
        usuario.setPasswordHash(passwordDemo);
        usuario.setNombres(valorOGenerado(request.getNombres(), "Estudiante"));
        usuario.setApellidos(valorOGenerado(request.getApellidos(), "Demo " + unico.toUpperCase()));
        usuario.setActivo(Boolean.TRUE);
        usuario.setRolSistema(rolEstudiante);
        usuario.setRolAsignado(ROL_ESTUDIANTE);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // También se registran credenciales en las tablas de login del proyecto
        Loginaplicativo loginAplicativo = new Loginaplicativo();
        loginAplicativo.setUsuario(usuarioGuardado);
        loginAplicativo.setUsuarioLogin(usuarioGuardado.getUsername());
        loginAplicativo.setPasswordLogin(passwordDemo);
        loginAplicativo.setEstado(Boolean.TRUE);
        loginAplicativoRepository.save(loginAplicativo);

        Loginbd loginBd = new Loginbd();
        loginBd.setUsuario(usuarioGuardado);
        loginBd.setUsernameBd(usuarioGuardado.getUsername());
        loginBd.setPasswordBd(passwordDemo);
        loginBd.setEstado(Boolean.TRUE);
        loginBdRepository.save(loginBd);

        Estudiante estudiante = new Estudiante();
        estudiante.setUsuario(usuarioGuardado);
        estudiante.setCarrera(carrera);
        estudiante.setPromedioRecord80(normalizarPromedio(request.getPromedioRecord80()));
        estudiante.setDiscapacidad(request.getDiscapacidad() != null ? request.getDiscapacidad() : Boolean.FALSE);

        return estudianteRepository.save(estudiante);
    }


    private RolSistema resolverOCrearRolEstudiante() {
        return rolesSistemaRepository.findByNombreRol(ROL_ESTUDIANTE)
                .or(() -> rolesSistemaRepository.findByNombreRol("ROLE_ESTUDIANTE"))
                .or(() -> rolesSistemaRepository.findAll().stream()
                        .filter(r -> r.getNombreRol() != null && "ESTUDIANTE".equalsIgnoreCase(r.getNombreRol().trim()))
                        .findFirst())
                .orElseGet(() -> {
                    RolSistema nuevoRol = new RolSistema();
                    nuevoRol.setNombreRol(ROL_ESTUDIANTE);
                    return rolesSistemaRepository.save(nuevoRol);
                });
    }

    private Carrera resolverCarrera(Integer idCarrera) {
        if (idCarrera != null) {
            return carreraRepository.findById(idCarrera)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "La carrera indicada no existe"
                    ));
        }

        return carreraRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No existe ninguna carrera registrada para asociar al estudiante"
                ));
    }

    private BigDecimal normalizarPromedio(BigDecimal promedio) {
        BigDecimal valor = promedio == null ? new BigDecimal("8.50") : promedio;

        if (valor.compareTo(BigDecimal.ZERO) < 0 || valor.compareTo(new BigDecimal("10.00")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "promedioRecord80 debe estar entre 0 y 10");
        }

        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String valorOGenerado(String valor, String fallback) {
        return (valor == null || valor.isBlank()) ? fallback : valor.trim();
    }

    public static class CrearDemoEstudianteRequest {
        private String nombres;
        private String apellidos;
        private String cedula;
        private String correoInstitucional;
        private String username;
        private BigDecimal promedioRecord80;
        private Boolean discapacidad;
        private Integer idCarrera;
        private String password;

        public String getNombres() {
            return nombres;
        }

        public void setNombres(String nombres) {
            this.nombres = nombres;
        }

        public String getApellidos() {
            return apellidos;
        }

        public void setApellidos(String apellidos) {
            this.apellidos = apellidos;
        }

        public String getCedula() {
            return cedula;
        }

        public void setCedula(String cedula) {
            this.cedula = cedula;
        }

        public String getCorreoInstitucional() {
            return correoInstitucional;
        }

        public void setCorreoInstitucional(String correoInstitucional) {
            this.correoInstitucional = correoInstitucional;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public BigDecimal getPromedioRecord80() {
            return promedioRecord80;
        }

        public void setPromedioRecord80(BigDecimal promedioRecord80) {
            this.promedioRecord80 = promedioRecord80;
        }

        public Boolean getDiscapacidad() {
            return discapacidad;
        }

        public void setDiscapacidad(Boolean discapacidad) {
            this.discapacidad = discapacidad;
        }

        public Integer getIdCarrera() {
            return idCarrera;
        }

        public void setIdCarrera(Integer idCarrera) {
            this.idCarrera = idCarrera;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}