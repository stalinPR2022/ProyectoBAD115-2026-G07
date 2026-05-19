package com.proyecto_bad115.sistema_encuestas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "privilegio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Privilegio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPrivilegio;

    @Column(nullable = false, length = 128)
    private String nombrePrivilegio;

    @Column(length = 256)
    private String descripcionPrivilegio;

    @Column(length = 512)
    private String urlPrivilegio;

    public Integer getIdPrivilegio() {
        return idPrivilegio;
    }

    public void setIdPrivilegio(Integer idPrivilegio) {
        this.idPrivilegio = idPrivilegio;
    }

    public String getNombrePrivilegio() {
        return nombrePrivilegio;
    }

    public void setNombrePrivilegio(String nombrePrivilegio) {
        this.nombrePrivilegio = nombrePrivilegio;
    }

    public String getDescripcionPrivilegio() {
        return descripcionPrivilegio;
    }

    public void setDescripcionPrivilegio(String descripcionPrivilegio) {
        this.descripcionPrivilegio = descripcionPrivilegio;
    }

    public String getUrlPrivilegio() {
        return urlPrivilegio;
    }

    public void setUrlPrivilegio(String urlPrivilegio) {
        this.urlPrivilegio = urlPrivilegio;
    }
}
