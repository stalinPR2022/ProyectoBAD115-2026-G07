package com.proyecto_bad115.sistema_encuestas.config;

public class PasswordPolicyValidator {

    // Min 8 caracteres, al menos una mayuscula, un numero y un caracter especial (en cualquier orden)
    private static final String PATTERN =
            "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";

    public static void validate(String password) {
        if (password == null || !password.matches(PATTERN)) {
            throw new IllegalArgumentException(
                "La contraseña debe tener mínimo 8 caracteres e incluir al menos " +
                "una mayúscula, un número y un carácter especial."
            );
        }
    }

    private PasswordPolicyValidator() {}
}
