package com.proyecto_bad115.sistema_encuestas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pregunta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPregunta;

    @Column(nullable = false, length = 500)
    private String descripcionPregunta;

    private Boolean obligatoriaPregunta;

    @Enumerated(EnumType.STRING)
    private TipoPregunta tipoPregunta;

    @Enumerated(EnumType.STRING)
    private TipoPreguntaCerrada tipoPreguntaCerrada;

    private Boolean esMixta;

    // CU07 - Criterios de validación (nullable; solo aplican según el tipo)
    @Column(name = "min_caracteres")
    private Integer minCaracteres;

    @Column(name = "max_caracteres")
    private Integer maxCaracteres;

    @Column(name = "max_selecciones")
    private Integer maxSelecciones;

    @ManyToOne
    @JoinColumn(name = "id_encuesta", nullable = false)
    private Encuesta encuesta;

    public Integer getIdPregunta() {
        return idPregunta;
    }

    public void setIdPregunta(Integer idPregunta) {
        this.idPregunta = idPregunta;
    }

    public String getDescripcionPregunta() {
        return descripcionPregunta;
    }

    public void setDescripcionPregunta(String descripcionPregunta) {
        this.descripcionPregunta = descripcionPregunta;
    }

    public Boolean getObligatoriaPregunta() {
        return obligatoriaPregunta;
    }

    public void setObligatoriaPregunta(Boolean obligatoriaPregunta) {
        this.obligatoriaPregunta = obligatoriaPregunta;
    }

    public TipoPregunta getTipoPregunta() {
        return tipoPregunta;
    }

    public void setTipoPregunta(TipoPregunta tipoPregunta) {
        this.tipoPregunta = tipoPregunta;
    }

    public TipoPreguntaCerrada getTipoPreguntaCerrada() {
        return tipoPreguntaCerrada;
    }

    public void setTipoPreguntaCerrada(TipoPreguntaCerrada tipoPreguntaCerrada) {
        this.tipoPreguntaCerrada = tipoPreguntaCerrada;
    }

    public Boolean getEsMixta() {
        return esMixta;
    }

    public void setEsMixta(Boolean esMixta) {
        this.esMixta = esMixta;
    }

    public Encuesta getEncuesta() {
        return encuesta;
    }

    public void setEncuesta(Encuesta encuesta) {
        this.encuesta = encuesta;
    }
}