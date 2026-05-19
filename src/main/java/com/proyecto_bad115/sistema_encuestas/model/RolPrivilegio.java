package com.proyecto_bad115.sistema_encuestas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rol_privilegio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolPrivilegio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idRolPrivilegio;

    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @ManyToOne
    @JoinColumn(name = "id_privilegio", nullable = false)
    private Privilegio privilegio;
}