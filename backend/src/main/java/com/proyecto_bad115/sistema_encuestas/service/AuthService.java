package com.proyecto_bad115.sistema_encuestas.service;

import com.proyecto_bad115.sistema_encuestas.dto.LoginRequestDTO;
import com.proyecto_bad115.sistema_encuestas.dto.LoginResponseDTO;
import com.proyecto_bad115.sistema_encuestas.model.EstadoUsuario;
import com.proyecto_bad115.sistema_encuestas.model.Usuario;
import com.proyecto_bad115.sistema_encuestas.repository.UsuarioRepository;
import com.proyecto_bad115.sistema_encuestas.repository.UsuarioRolRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private static final int MAX_INTENTOS = 3;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository,
                       UsuarioRolRepository usuarioRolRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmailUser(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));

        if (usuario.getEstadoUser() == EstadoUsuario.BLOQUEADO) {
            throw new LockedException("Cuenta bloqueada por multiples intentos fallidos. Contacte al administrador.");
        }

        if (usuario.getEstadoUser() == EstadoUsuario.INACTIVO) {
            throw new DisabledException("Cuenta inactiva. Contacte al administrador.");
        }

        if (!passwordEncoder.matches(request.getContrasenia(), usuario.getContraseniaUser())) {
            int intentos = usuario.getIntentosFallidos() + 1;
            usuario.setIntentosFallidos(intentos);

            if (intentos >= MAX_INTENTOS) {
                usuario.setEstadoUser(EstadoUsuario.BLOQUEADO);
                usuarioRepository.save(usuario);
                throw new LockedException("Cuenta bloqueada por " + MAX_INTENTOS + " intentos fallidos. Contacte al administrador.");
            }

            usuarioRepository.save(usuario);
            int restantes = MAX_INTENTOS - intentos;
            throw new BadCredentialsException("Credenciales invalidas. Intentos restantes: " + restantes);
        }

        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);

        List<String> roles = usuarioRolRepository.findByUsuario(usuario)
                .stream()
                .map(ur -> ur.getRol().getNombreRol())
                .toList();

        String token = jwtService.generateToken(usuario.getEmailUser());
        return new LoginResponseDTO(token, usuario.getNombreUser(), usuario.getEmailUser(), roles);
    }
}
