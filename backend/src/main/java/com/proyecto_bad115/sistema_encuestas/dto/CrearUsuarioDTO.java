package com.proyecto_bad115.sistema_encuestas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CrearUsuarioDTO {

    @NotBlank
    private String nombreUser;

    @NotBlank
    @Email
    private String emailUser;

    @NotBlank
    private String contraseniaUser;

    @NotNull
    private LocalDate fechaNacimiento;
}
