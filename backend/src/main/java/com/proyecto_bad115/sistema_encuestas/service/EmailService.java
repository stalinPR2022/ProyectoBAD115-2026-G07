package com.proyecto_bad115.sistema_encuestas.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarDesbloqueo(String destinatario, String nombreUsuario) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom("sondevia.sistema@gmail.com");
        mensaje.setTo(destinatario);
        mensaje.setSubject("Sondevia - Cuenta desbloqueada");
        mensaje.setText(
            "Hola " + nombreUsuario + ",\n\n" +
            "Tu cuenta en Sondevia ha sido desbloqueada por el administrador del sistema.\n" +
            "Ya puedes iniciar sesion con tus credenciales.\n\n" +
            "Si no solicitaste este desbloqueo, contacta al administrador.\n\n" +
            "Equipo Sondevia"
        );
        mailSender.send(mensaje);
    }

    public void enviarBienvenida(String destinatario, String nombreUsuario, String contraseniaTemporal) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom("sondevia.sistema@gmail.com");
        mensaje.setTo(destinatario);
        mensaje.setSubject("Sondevia - Bienvenido al sistema");
        mensaje.setText(
            "Hola " + nombreUsuario + ",\n\n" +
            "Tu cuenta en Sondevia ha sido creada exitosamente.\n" +
            "Tus credenciales de acceso son:\n" +
            "  Correo: " + destinatario + "\n" +
            "  Contraseña temporal: " + contraseniaTemporal + "\n\n" +
            "Por favor cambia tu contraseña al iniciar sesion por primera vez.\n\n" +
            "Equipo Sondevia"
        );
        mailSender.send(mensaje);
    }
}
