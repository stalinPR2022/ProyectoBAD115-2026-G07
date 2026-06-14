package com.proyecto_bad115.sistema_encuestas.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Estadísticas de una pregunta (CU09). */
@Getter
@Setter
public class ResultadoPreguntaDTO {
    private Integer idPregunta;
    private String descripcionPregunta;
    private String tipoPregunta;
    private String tipoPreguntaCerrada;
    private Boolean esMixta;
    private long totalRespuestas;
    private String graficoSugerido; // "pastel" | "barras" | "linea" | "texto"
    private List<ConteoOpcionDTO> opciones = new ArrayList<>();
    private List<String> respuestasTexto = new ArrayList<>();
}
