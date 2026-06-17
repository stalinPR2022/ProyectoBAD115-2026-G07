package com.proyecto_bad115.sistema_encuestas.service;

import com.proyecto_bad115.sistema_encuestas.config.PasswordPolicyValidator;
import com.proyecto_bad115.sistema_encuestas.dto.ActualizarUsuarioDTO;
import com.proyecto_bad115.sistema_encuestas.dto.CrearUsuarioDTO;
import com.proyecto_bad115.sistema_encuestas.dto.UsuarioResponseDTO;
import com.proyecto_bad115.sistema_encuestas.model.EstadoUsuario;
import com.proyecto_bad115.sistema_encuestas.model.Usuario;
import com.proyecto_bad115.sistema_encuestas.repository.EncuestaRepository;
import com.proyecto_bad115.sistema_encuestas.repository.RespuestaRepository;
import com.proyecto_bad115.sistema_encuestas.repository.UsuarioRepository;
import com.proyecto_bad115.sistema_encuestas.repository.UsuarioRolRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final EncuestaRepository encuestaRepository;
    private final RespuestaRepository respuestaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          UsuarioRolRepository usuarioRolRepository,
                          EncuestaRepository encuestaRepository,
                          RespuestaRepository respuestaRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.encuestaRepository = encuestaRepository;
        this.respuestaRepository = respuestaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public UsuarioResponseDTO buscarPorId(Integer id) {
        return usuarioRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
    }

    public UsuarioResponseDTO crearUsuario(CrearUsuarioDTO dto) {
        if (usuarioRepository.existsByEmailUser(dto.getEmailUser())) {
            throw new IllegalArgumentException("El correo ya esta registrado");
        }
        PasswordPolicyValidator.validate(dto.getContraseniaUser());

        Usuario usuario = new Usuario();
        usuario.setNombreUser(dto.getNombreUser());
        usuario.setEmailUser(dto.getEmailUser());
        usuario.setContraseniaUser(passwordEncoder.encode(dto.getContraseniaUser()));
        usuario.setFechaNacimiento(dto.getFechaNacimiento());
        usuario.setEstadoUser(EstadoUsuario.ACTIVO);
        usuario.setIntentosFallidos(0);

        usuarioRepository.save(usuario);

        try {
            emailService.enviarBienvenida(dto.getEmailUser(), dto.getNombreUser(), dto.getContraseniaUser());
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de bienvenida a {}: {}", dto.getEmailUser(), e.getMessage());
        }

        return toDTO(usuario);
    }

    public UsuarioResponseDTO actualizarUsuario(Integer id, ActualizarUsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        usuario.setNombreUser(dto.getNombreUser());
        usuario.setFechaNacimiento(dto.getFechaNacimiento());
        usuarioRepository.save(usuario);
        return toDTO(usuario);
    }

    public UsuarioResponseDTO activarUsuario(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
        usuario.setEstadoUser(EstadoUsuario.ACTIVO);
        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);
        return toDTO(usuario);
    }

    public UsuarioResponseDTO darDeBaja(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
        usuario.setEstadoUser(EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);
        return toDTO(usuario);
    }

    public UsuarioResponseDTO desbloquearUsuario(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
        usuario.setEstadoUser(EstadoUsuario.ACTIVO);
        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);

        try {
            emailService.enviarDesbloqueo(usuario.getEmailUser(), usuario.getNombreUser());
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de desbloqueo a {}: {}", usuario.getEmailUser(), e.getMessage());
        }

        return toDTO(usuario);
    }

    /**
     * Eliminación segura: bloquea si el usuario es dueño de encuestas o tiene
     * respuestas registradas (para no romper integridad), y no permite que un
     * usuario se elimine a sí mismo. Limpia primero los vínculos de roles.
     */
    @Transactional
    public void eliminarUsuario(Integer id, String emailSolicitante) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        if (emailSolicitante != null && usuario.getEmailUser().equalsIgnoreCase(emailSolicitante)) {
            throw new IllegalStateException("No puedes eliminar tu propia cuenta.");
        }
        if (encuestaRepository.existsByUsuarioIdUser(id)) {
            throw new IllegalStateException(
                    "No se puede eliminar: el usuario tiene encuestas creadas. " +
                    "Reasigna o elimina esas encuestas, o da de baja al usuario.");
        }
        if (respuestaRepository.existsByUsuarioIdUser(id)) {
            throw new IllegalStateException(
                    "No se puede eliminar: el usuario tiene respuestas registradas en encuestas. " +
                    "Considera darlo de baja en su lugar.");
        }

        usuarioRolRepository.deleteAll(usuarioRolRepository.findByUsuarioIdUser(id));
        usuarioRepository.delete(usuario);
    }

    private UsuarioResponseDTO toDTO(Usuario u) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setIdUser(u.getIdUser());
        dto.setNombreUser(u.getNombreUser());
        dto.setEmailUser(u.getEmailUser());
        dto.setFechaNacimiento(u.getFechaNacimiento());
        dto.setEstadoUser(u.getEstadoUser());
        dto.setIntentosFallidos(u.getIntentosFallidos());
        dto.setRoles(usuarioRolRepository.findByUsuario(u).stream()
                .map(ur -> ur.getRol().getNombreRol())
                .toList());
        return dto;
    }
}
