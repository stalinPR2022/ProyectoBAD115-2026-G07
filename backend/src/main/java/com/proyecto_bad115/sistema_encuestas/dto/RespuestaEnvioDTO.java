package com.proyecto_bad115.sistema_encuestas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Envío final de respuestas del encuestado (CU13).
 * El correo identifica al participante registrado en CU11.
 */
@Getter
@Setter
public class RespuestaEnvioDTO {

    @NotBlank
    @Email
    private String email;

    private List<DetalleEnvioDTO> respuestas;
}
