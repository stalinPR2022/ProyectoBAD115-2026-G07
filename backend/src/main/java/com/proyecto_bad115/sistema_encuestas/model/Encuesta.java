package com.proyecto_bad115.sistema_encuestas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "encuesta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Encuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEncuesta;

    @Column(nullable = false, length = 150)
    private String tituloEncuesta;

    @Column(length = 500)
    private String objetivoEncuesta;

    @Column(length = 500)
    private String instruccionesEncuesta;

    private Integer estadoEncuesta;

    private LocalDate fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "id_user", nullable = false)
    private Usuario usuario;

    public Integer getIdEncuesta() {
        return idEncuesta;
    }

    public void setIdEncuesta(Integer idEncuesta) {
        this.idEncuesta = idEncuesta;
    }

    public String getTituloEncuesta() {
        return tituloEncuesta;
    }

    public void setTituloEncuesta(String tituloEncuesta) {
        this.tituloEncuesta = tituloEncuesta;
    }

    public String getObjetivoEncuesta() {
        return objetivoEncuesta;
    }

    public void setObjetivoEncuesta(String objetivoEncuesta) {
        this.objetivoEncuesta = objetivoEncuesta;
    }

    public String getInstruccionesEncuesta() {
        return instruccionesEncuesta;
    }

    public void setInstruccionesEncuesta(String instruccionesEncuesta) {
        this.instruccionesEncuesta = instruccionesEncuesta;
    }

    public Integer getEstadoEncuesta() {
        return estadoEncuesta;
    }

    public void setEstadoEncuesta(Integer estadoEncuesta) {
        this.estadoEncuesta = estadoEncuesta;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}