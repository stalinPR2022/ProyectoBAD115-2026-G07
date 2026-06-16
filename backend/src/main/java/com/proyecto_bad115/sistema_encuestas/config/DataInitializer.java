package com.proyecto_bad115.sistema_encuestas.config;

import com.proyecto_bad115.sistema_encuestas.model.*;
import com.proyecto_bad115.sistema_encuestas.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PrivilegioRepository privilegioRepository;
    private final RolPrivilegioRepository rolPrivilegioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RespuestaRepository respuestaRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           PrivilegioRepository privilegioRepository,
                           RolPrivilegioRepository rolPrivilegioRepository,
                           UsuarioRolRepository usuarioRolRepository,
                           RespuestaRepository respuestaRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.privilegioRepository = privilegioRepository;
        this.rolPrivilegioRepository = rolPrivilegioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.respuestaRepository = respuestaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        crearRoles();
        crearPrivilegios();
        asignarPrivilegiosAdministrador();
        asignarPrivilegiosEncuestado();
        crearUsuarioAdmin();
        migrarRespuestasLegacy();
    }

    // Etapa 17 - Respuestas creadas antes de los borradores se marcan como ENVIADAS
    private void migrarRespuestasLegacy() {
        List<Respuesta> sinEstado = respuestaRepository.findByEstadoRespuestaIsNull();
        if (!sinEstado.isEmpty()) {
            sinEstado.forEach(r -> r.setEstadoRespuesta(EstadoRespuesta.ENVIADA));
            respuestaRepository.saveAll(sinEstado);
            System.out.println(">>> Migradas " + sinEstado.size() + " respuestas previas a estado ENVIADA");
        }
    }

    private void crearRoles() {
        List.of(
            new String[]{"ADMINISTRADOR", "Acceso total al sistema"},
            new String[]{"ENCUESTADOR", "Puede crear y gestionar encuestas"},
            new String[]{"ENCUESTADO", "Puede responder encuestas"}
        ).forEach(r -> {
            if (!rolRepository.existsByNombreRol(r[0])) {
                Rol rol = new Rol();
                rol.setNombreRol(r[0]);
                rol.setDescripcionRol(r[1]);
                rolRepository.save(rol);
            }
        });
    }

    private void crearPrivilegios() {
        List.of(
            new String[]{"Gestionar Usuarios", "CRUD de usuarios del sistema", "/dashboard/usuarios"},
            new String[]{"Asignar Roles", "Vincular roles a usuarios", "/dashboard/roles"},
            new String[]{"Gestionar Privilegios", "Administrar opciones de menu y permisos", "/dashboard/privilegios"},
            new String[]{"Desbloquear Usuarios", "Reactivar cuentas bloqueadas", "/dashboard/usuarios/bloqueados"},
            new String[]{"Gestionar Encuestas", "Crear, editar y eliminar encuestas", "/dashboard/encuestas"},
            new String[]{"Responder Encuestas", "Ver y responder encuestas disponibles", "/dashboard/responder"},
            new String[]{"Mis Encuestas", "Encuestas en progreso y respondidas del encuestado", "/dashboard/mis-encuestas"},
            new String[]{"Ver Resultados", "Consultar resultados de encuestas", "/dashboard/resultados"}
        ).forEach(p -> {
            if (!privilegioRepository.existsByNombrePrivilegio(p[0])) {
                Privilegio privilegio = new Privilegio();
                privilegio.setNombrePrivilegio(p[0]);
                privilegio.setDescripcionPrivilegio(p[1]);
                privilegio.setUrlPrivilegio(p[2]);
                privilegioRepository.save(privilegio);
            }
        });
    }

    private void asignarPrivilegiosAdministrador() {
        rolRepository.findByNombreRol("ADMINISTRADOR").ifPresent(admin -> {
            List<String> todosLosPrivilegios = List.of(
                "Gestionar Usuarios", "Asignar Roles", "Gestionar Privilegios",
                "Desbloquear Usuarios", "Gestionar Encuestas", "Ver Resultados"
            );
            todosLosPrivilegios.forEach(nombre ->
                privilegioRepository.findByNombrePrivilegio(nombre).ifPresent(priv -> {
                    boolean yaAsignado = rolPrivilegioRepository.findByRolIdRol(admin.getIdRol())
                            .stream().anyMatch(rp -> rp.getPrivilegio().getIdPrivilegio().equals(priv.getIdPrivilegio()));
                    if (!yaAsignado) {
                        RolPrivilegio rp = new RolPrivilegio();
                        rp.setRol(admin);
                        rp.setPrivilegio(priv);
                        rolPrivilegioRepository.save(rp);
                    }
                })
            );
        });
    }

    private void asignarPrivilegiosEncuestado() {
        rolRepository.findByNombreRol("ENCUESTADO").ifPresent(encuestado ->
            List.of("Responder Encuestas", "Mis Encuestas").forEach(nombre ->
                privilegioRepository.findByNombrePrivilegio(nombre).ifPresent(priv -> {
                    boolean yaAsignado = rolPrivilegioRepository.findByRolIdRol(encuestado.getIdRol())
                            .stream().anyMatch(rp -> rp.getPrivilegio().getIdPrivilegio().equals(priv.getIdPrivilegio()));
                    if (!yaAsignado) {
                        RolPrivilegio rp = new RolPrivilegio();
                        rp.setRol(encuestado);
                        rp.setPrivilegio(priv);
                        rolPrivilegioRepository.save(rp);
                    }
                })
            )
        );
    }

    private void crearUsuarioAdmin() {
        if (!usuarioRepository.existsByEmailUser("admin@sondevia.com")) {
            Usuario admin = new Usuario();
            admin.setNombreUser("Administrador");
            admin.setEmailUser("admin@sondevia.com");
            admin.setContraseniaUser(passwordEncoder.encode("Admin@2026"));
            admin.setFechaNacimiento(LocalDate.of(1990, 1, 1));
            admin.setEstadoUser(EstadoUsuario.ACTIVO);
            admin.setIntentosFallidos(0);
            usuarioRepository.save(admin);

            rolRepository.findByNombreRol("ADMINISTRADOR").ifPresent(rol -> {
                UsuarioRol ur = new UsuarioRol();
                ur.setUsuario(admin);
                ur.setRol(rol);
                usuarioRolRepository.save(ur);
            });

            System.out.println(">>> Admin creado: admin@sondevia.com / Admin@2026");
        }
    }
}
