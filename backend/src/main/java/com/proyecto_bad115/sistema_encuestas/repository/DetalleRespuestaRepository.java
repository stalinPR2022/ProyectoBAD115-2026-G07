package com.proyecto_bad115.sistema_encuestas.repository;

import com.proyecto_bad115.sistema_encuestas.model.DetalleRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleRespuestaRepository extends JpaRepository<DetalleRespuesta, Integer> {

    // CU09 - Detalles de respuesta de una pregunta (para estadísticas)
    List<DetalleRespuesta> findByPreguntaIdPregunta(Integer idPregunta);
}