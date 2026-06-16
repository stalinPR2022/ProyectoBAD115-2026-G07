package com.proyecto_bad115.sistema_encuestas.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Encuesta publicada y vigente, mostrada en el catálogo "Responder Encuestas".
 * estadoRespuesta indica si el usuario ya interactuó con ella.
 */
@Getter
@Setter
public class EncuestaDisponibleDTO {
    private Integer idEncuesta;
    private String tituloEncuesta;
    private String objetivoEncuesta;
    private String grupoMeta;
    private LocalDate fechaCierre;
    private Integer totalPreguntas;
    private String tokenPublico;
    private Integer estadoRespuesta; // null = sin responder, 1 = borrador, 2 = enviada
}
