package com.proyecto_bad115.sistema_encuestas.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PreguntaResponseDTO {
    private Integer idPregunta;
    private String descripcionPregunta;
    private Boolean obligatoriaPregunta;
    private String tipoPregunta;
    private String tipoPreguntaCerrada;
    private Boolean esMixta;
    private Integer minCaracteres;
    private Integer maxCaracteres;
    private Integer maxSelecciones;
    private Integer idEncuesta;
    private List<OpcionResponseDTO> opciones;
}
