package com.proyecto_bad115.sistema_encuestas.config;

public class PasswordPolicyValidator {

    // Min 8 chars, starts with letter, at least one uppercase, one special char
    private static final String PATTERN =
            "^[a-zA-Z](?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{7,}$";

    public static void validate(String password) {
        if (password == null || !password.matches(PATTERN)) {
            throw new IllegalArgumentException(
                "La contraseña debe tener minimo 8 caracteres, iniciar con letra, " +
                "contener al menos una mayuscula y un caracter especial."
            );
        }
    }

    private PasswordPolicyValidator() {}
}
