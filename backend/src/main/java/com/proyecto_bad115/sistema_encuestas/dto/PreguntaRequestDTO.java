package com.proyecto_bad115.sistema_encuestas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PreguntaRequestDTO {

    @NotBlank
    private String descripcionPregunta;

    @NotNull
    private Boolean obligatoriaPregunta;

    @NotBlank
    private String tipoPregunta;

    private String tipoPreguntaCerrada;

    private Boolean esMixta;

    // CU07 - Criterios de validación
    private Integer minCaracteres;
    private Integer maxCaracteres;
    private Integer maxSelecciones;

    private List<String> opciones;
}
