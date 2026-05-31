package com.proyecto_bad115.sistema_encuestas.service;

import com.proyecto_bad115.sistema_encuestas.config.PasswordPolicyValidator;
import com.proyecto_bad115.sistema_encuestas.dto.ActualizarUsuarioDTO;
import com.proyecto_bad115.sistema_encuestas.dto.CrearUsuarioDTO;
import com.proyecto_bad115.sistema_encuestas.dto.UsuarioResponseDTO;
import com.proyecto_bad115.sistema_encuestas.model.EstadoUsuario;
import com.proyecto_bad115.sistema_encuestas.model.Usuario;
import com.proyecto_bad115.sistema_encuestas.repository.UsuarioRepository;
import com.proyecto_bad115.sistema_encuestas.repository.UsuarioRolRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          UsuarioRolRepository usuarioRolRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
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
        } catch (Exception ignored) {}

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
        } catch (Exception ignored) {}

        return toDTO(usuario);
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
