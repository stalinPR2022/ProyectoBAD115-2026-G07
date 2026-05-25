package com.proyecto_bad115.sistema_encuestas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "opcion_respuesta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpcionRespuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idOpcionRespuesta;

    @Column(nullable = false, length = 255)
    private String textoOpcion;

    private Integer valorNumerico;

    private Boolean esMixta;

    @ManyToOne
    @JoinColumn(name = "id_pregunta", nullable = false)
    private Pregunta pregunta;

    public Integer getIdOpcionRespuesta() {
        return idOpcionRespuesta;
    }

    public void setIdOpcionRespuesta(Integer idOpcionRespuesta) {
        this.idOpcionRespuesta = idOpcionRespuesta;
    }

    public String getTextoOpcion() {
        return textoOpcion;
    }

    public void setTextoOpcion(String textoOpcion) {
        this.textoOpcion = textoOpcion;
    }

    public Integer getValorNumerico() {
        return valorNumerico;
    }

    public void setValorNumerico(Integer valorNumerico) {
        this.valorNumerico = valorNumerico;
    }

    public Boolean getEsMixta() {
        return esMixta;
    }

    public void setEsMixta(Boolean esMixta) {
        this.esMixta = esMixta;
    }

    public Pregunta getPregunta() {
        return pregunta;
    }

    public void setPregunta(Pregunta pregunta) {
        this.pregunta = pregunta;
    }
}