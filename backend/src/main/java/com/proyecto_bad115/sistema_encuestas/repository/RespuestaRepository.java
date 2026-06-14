package com.proyecto_bad115.sistema_encuestas.repository;

import com.proyecto_bad115.sistema_encuestas.model.Respuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RespuestaRepository extends JpaRepository<Respuesta, Integer> {

    // CU11 - Detecta respuestas duplicadas: mismo correo en la misma encuesta
    boolean existsByEncuestaIdEncuestaAndUsuarioEmailUser(Integer idEncuesta, String emailUser);

    // CU09 - Total de respuestas registradas en una encuesta
    long countByEncuestaIdEncuesta(Integer idEncuesta);
}