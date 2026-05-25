package com.proyecto_bad115.sistema_encuestas.service;

import com.proyecto_bad115.sistema_encuestas.model.DetalleRespuesta;
import com.proyecto_bad115.sistema_encuestas.repository.DetalleRespuestaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DetalleRespuestaService {

    private final DetalleRespuestaRepository detalleRespuestaRepository;

    public DetalleRespuestaService(DetalleRespuestaRepository detalleRespuestaRepository) {
        this.detalleRespuestaRepository = detalleRespuestaRepository;
    }

    public List<DetalleRespuesta> listarDetalles() {
        return detalleRespuestaRepository.findAll();
    }

    public Optional<DetalleRespuesta> buscarPorId(Integer id) {
        return detalleRespuestaRepository.findById(id);
    }

    public DetalleRespuesta guardarDetalle(DetalleRespuesta detalle) {
        return detalleRespuestaRepository.save(detalle);
    }

    public void eliminarDetalle(Integer id) {
        detalleRespuestaRepository.deleteById(id);
    }
}