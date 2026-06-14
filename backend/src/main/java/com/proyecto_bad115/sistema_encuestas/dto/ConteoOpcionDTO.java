package com.proyecto_bad115.sistema_encuestas.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Frecuencia de una opción/valor en los resultados (CU09). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConteoOpcionDTO {
    private String etiqueta;
    private long cantidad;
    private double porcentaje;
}
