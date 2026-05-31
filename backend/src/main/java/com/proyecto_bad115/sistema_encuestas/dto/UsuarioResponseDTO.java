package com.proyecto_bad115.sistema_encuestas.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UsuarioResponseDTO {

    private Integer idUser;
    private String nombreUser;
    private String emailUser;
    private LocalDate fechaNacimiento;
    private Integer estadoUser;
    private Integer intentosFallidos;
    private List<String> roles;
}
