package com.proyecto_bad115.sistema_encuestas.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Envío final de respuestas del encuestado autenticado (CU13).
 * El participante se identifica por su sesión (JWT), no por el correo en el body.
 */
@Getter
@Setter
public class RespuestaEnvioDTO {
    private List<DetalleEnvioDTO> respuestas;
}
