package com.proyecto_bad115.sistema_encuestas.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Panel de resultados de una encuesta (CU09). */
@Getter
@Setter
public class ResultadosDTO {
    private Integer idEncuesta;
    private String tituloEncuesta;
    private Integer estadoEncuesta;
    private String estadoNombre;
    private long totalRespuestas;
    private String opcionMasSeleccionada;
    private List<ResultadoPreguntaDTO> preguntas = new ArrayList<>();
}
