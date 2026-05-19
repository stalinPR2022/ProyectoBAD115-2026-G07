package com.proyecto_bad115.sistema_encuestas.service;

import com.proyecto_bad115.sistema_encuestas.model.OpcionRespuesta;
import com.proyecto_bad115.sistema_encuestas.repository.OpcionRespuestaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OpcionRespuestaService {

    private final OpcionRespuestaRepository opcionRespuestaRepository;

    public OpcionRespuestaService(OpcionRespuestaRepository opcionRespuestaRepository) {
        this.opcionRespuestaRepository = opcionRespuestaRepository;
    }

    public List<OpcionRespuesta> listarOpciones() {
        return opcionRespuestaRepository.findAll();
    }

    public Optional<OpcionRespuesta> buscarPorId(Integer id) {
        return opcionRespuestaRepository.findById(id);
    }

    public OpcionRespuesta guardarOpcion(OpcionRespuesta opcion) {
        return opcionRespuestaRepository.save(opcion);
    }

    public void eliminarOpcion(Integer id) {
        opcionRespuestaRepository.deleteById(id);
    }
}