package com.proyecto_bad115.sistema_encuestas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "detalle_respuesta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleRespuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDetalleRespuesta;

    @Column(length = 500)
    private String textoRespuesta;

    private Integer valorRespuesta;

    @ManyToOne
    @JoinColumn(name = "id_respuesta", nullable = false)
    private Respuesta respuesta;

    @ManyToOne
    @JoinColumn(name = "id_pregunta", nullable = false)
    private Pregunta pregunta;

    @ManyToOne
    @JoinColumn(name = "id_opcion_respuesta")
    private OpcionRespuesta opcionRespuesta;

    public Integer getIdDetalleRespuesta() {
        return idDetalleRespuesta;
    }

    public void setIdDetalleRespuesta(Integer idDetalleRespuesta) {
        this.idDetalleRespuesta = idDetalleRespuesta;
    }

    public String getTextoRespuesta() {
        return textoRespuesta;
    }

    public void setTextoRespuesta(String textoRespuesta) {
        this.textoRespuesta = textoRespuesta;
    }

    public Integer getValorRespuesta() {
        return valorRespuesta;
    }

    public void setValorRespuesta(Integer valorRespuesta) {
        this.valorRespuesta = valorRespuesta;
    }

    public Respuesta getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(Respuesta respuesta) {
        this.respuesta = respuesta;
    }

    public Pregunta getPregunta() {
        return pregunta;
    }

    public void setPregunta(Pregunta pregunta) {
        this.pregunta = pregunta;
    }

    public OpcionRespuesta getOpcionRespuesta() {
        return opcionRespuesta;
    }

    public void setOpcionRespuesta(OpcionRespuesta opcionRespuesta) {
        this.opcionRespuesta = opcionRespuesta;
    }
}
