package com.proyecto_bad115.sistema_encuestas.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Confirmación del envío de respuestas (CU13).
 * numeroRegistro = identificador único de la respuesta registrada.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaConfirmacionDTO {
    private Integer numeroRegistro;
    private LocalDate fechaRespuesta;
}
