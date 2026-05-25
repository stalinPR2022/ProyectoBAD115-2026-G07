package com.proyecto_bad115.sistema_encuestas.service;

import com.proyecto_bad115.sistema_encuestas.model.Respuesta;
import com.proyecto_bad115.sistema_encuestas.repository.RespuestaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RespuestaService {

    private final RespuestaRepository respuestaRepository;

    public RespuestaService(RespuestaRepository respuestaRepository) {
        this.respuestaRepository = respuestaRepository;
    }

    public List<Respuesta> listarRespuestas() {
        return respuestaRepository.findAll();
    }

    public Optional<Respuesta> buscarPorId(Integer id) {
        return respuestaRepository.findById(id);
    }

    public Respuesta guardarRespuesta(Respuesta respuesta) {
        return respuestaRepository.save(respuesta);
    }

    public void eliminarRespuesta(Integer id) {
        respuestaRepository.deleteById(id);
    }
}