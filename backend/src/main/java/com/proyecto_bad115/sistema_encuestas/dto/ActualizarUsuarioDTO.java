package com.proyecto_bad115.sistema_encuestas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ActualizarUsuarioDTO {

    @NotBlank
    private String nombreUser;

    @NotNull
    private LocalDate fechaNacimiento;
}
