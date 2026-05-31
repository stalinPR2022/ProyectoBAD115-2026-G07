package com.proyecto_bad115.sistema_encuestas.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;
    private String nombreUser;
    private String emailUser;
    private List<String> roles;
}
