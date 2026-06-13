package com.proyecto_bad115.sistema_encuestas.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Respuesta del encuestado a una pregunta (CU13).
 * Solo se llenan los campos que correspondan al tipo de pregunta.
 */
@Getter
@Setter
public class DetalleEnvioDTO {
    private Integer idPregunta;
    private String texto;            // pregunta abierta
    private Integer idOpcion;        // selección única / likert / nominal
    private List<Integer> idOpciones; // selección múltiple
    private Integer valor;           // escala numérica
    private List<Integer> ranking;   // orden de idOpcionRespuesta (ranking)
    private String otrosTexto;       // texto libre de la opción "Otros" (mixta)
}
