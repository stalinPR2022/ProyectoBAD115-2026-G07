package com.proyecto_bad115.sistema_encuestas.service;

import com.proyecto_bad115.sistema_encuestas.dto.OpcionResponseDTO;
import com.proyecto_bad115.sistema_encuestas.dto.PreguntaRequestDTO;
import com.proyecto_bad115.sistema_encuestas.dto.PreguntaResponseDTO;
import com.proyecto_bad115.sistema_encuestas.model.*;
import com.proyecto_bad115.sistema_encuestas.repository.EncuestaRepository;
import com.proyecto_bad115.sistema_encuestas.repository.OpcionRespuestaRepository;
import com.proyecto_bad115.sistema_encuestas.repository.PreguntaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PreguntaService {

    private final PreguntaRepository preguntaRepository;
    private final EncuestaRepository encuestaRepository;
    private final OpcionRespuestaRepository opcionRespuestaRepository;

    public PreguntaService(PreguntaRepository preguntaRepository,
                           EncuestaRepository encuestaRepository,
                           OpcionRespuestaRepository opcionRespuestaRepository) {
        this.preguntaRepository = preguntaRepository;
        this.encuestaRepository = encuestaRepository;
        this.opcionRespuestaRepository = opcionRespuestaRepository;
    }

    public List<PreguntaResponseDTO> listarPorEncuesta(Integer idEncuesta) {
        return preguntaRepository.findByEncuestaIdEncuesta(idEncuesta)
                .stream().map(this::toDTO).toList();
    }

    @Transactional
    public PreguntaResponseDTO agregar(Integer idEncuesta, PreguntaRequestDTO dto) {
        Encuesta encuesta = encuestaRepository.findById(idEncuesta)
                .orElseThrow(() -> new NoSuchElementException("Encuesta no encontrada"));

        if (encuesta.getEstadoEncuesta() != EstadoEncuesta.EN_DISENO) {
            throw new IllegalStateException("Solo se pueden agregar preguntas a encuestas en estado 'En diseño'");
        }

        TipoPregunta tipo = TipoPregunta.valueOf(dto.getTipoPregunta());

        if (tipo == TipoPregunta.CERRADA) {
            validarPreguntaCerrada(dto);
        }

        Pregunta pregunta = new Pregunta();
        pregunta.setDescripcionPregunta(dto.getDescripcionPregunta());
        pregunta.setObligatoriaPregunta(dto.getObligatoriaPregunta());
        pregunta.setTipoPregunta(tipo);
        pregunta.setEsMixta(false);
        pregunta.setEncuesta(encuesta);

        if (tipo == TipoPregunta.CERRADA && dto.getTipoPreguntaCerrada() != null) {
            pregunta.setTipoPreguntaCerrada(TipoPreguntaCerrada.valueOf(dto.getTipoPreguntaCerrada()));
        }

        Pregunta guardada = preguntaRepository.save(pregunta);
        guardarOpciones(guardada, dto.getOpciones());

        return toDTO(guardada);
    }

    @Transactional
    public PreguntaResponseDTO actualizar(Integer idPregunta, PreguntaRequestDTO dto) {
        Pregunta pregunta = preguntaRepository.findById(idPregunta)
                .orElseThrow(() -> new NoSuchElementException("Pregunta no encontrada"));

        if (pregunta.getEncuesta().getEstadoEncuesta() != EstadoEncuesta.EN_DISENO) {
            throw new IllegalStateException("No se pueden modificar preguntas de una encuesta publicada");
        }

        if (pregunta.getTipoPregunta() == TipoPregunta.CERRADA) {
            validarPreguntaCerrada(dto);
        }

        pregunta.setDescripcionPregunta(dto.getDescripcionPregunta());
        pregunta.setObligatoriaPregunta(dto.getObligatoriaPregunta());

        opcionRespuestaRepository.deleteByPreguntaIdPregunta(idPregunta);
        guardarOpciones(pregunta, dto.getOpciones());

        return toDTO(preguntaRepository.save(pregunta));
    }

    @Transactional
    public void eliminar(Integer idPregunta) {
        Pregunta pregunta = preguntaRepository.findById(idPregunta)
                .orElseThrow(() -> new NoSuchElementException("Pregunta no encontrada"));

        if (pregunta.getEncuesta().getEstadoEncuesta() != EstadoEncuesta.EN_DISENO) {
            throw new IllegalStateException("No se pueden eliminar preguntas de una encuesta publicada");
        }

        opcionRespuestaRepository.deleteByPreguntaIdPregunta(idPregunta);
        preguntaRepository.deleteById(idPregunta);
    }

    private void guardarOpciones(Pregunta pregunta, List<String> textos) {
        if (textos == null || textos.isEmpty()) return;
        for (int i = 0; i < textos.size(); i++) {
            OpcionRespuesta op = new OpcionRespuesta();
            op.setTextoOpcion(textos.get(i));
            op.setValorNumerico(i + 1);
            op.setEsMixta(false);
            op.setPregunta(pregunta);
            opcionRespuestaRepository.save(op);
        }
    }

    private void validarPreguntaCerrada(PreguntaRequestDTO dto) {
        if (dto.getTipoPreguntaCerrada() == null || dto.getTipoPreguntaCerrada().isBlank()) {
            throw new IllegalArgumentException("Debe especificar el tipo de pregunta cerrada");
        }
        if (dto.getOpciones() == null || dto.getOpciones().stream().filter(o -> !o.isBlank()).count() < 2) {
            throw new IllegalArgumentException("La pregunta debe tener al menos 2 opciones o valores");
        }
        if (dto.getTipoPreguntaCerrada().equals("ESCALA")) {
            try {
                int min = Integer.parseInt(dto.getOpciones().get(0).trim());
                int max = Integer.parseInt(dto.getOpciones().get(1).trim());
                if (min >= max) throw new IllegalArgumentException("El valor mínimo debe ser menor al máximo");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Los valores de la escala numérica deben ser números enteros");
            }
        }
    }

    private PreguntaResponseDTO toDTO(Pregunta p) {
        PreguntaResponseDTO dto = new PreguntaResponseDTO();
        dto.setIdPregunta(p.getIdPregunta());
        dto.setDescripcionPregunta(p.getDescripcionPregunta());
        dto.setObligatoriaPregunta(p.getObligatoriaPregunta());
        dto.setTipoPregunta(p.getTipoPregunta() != null ? p.getTipoPregunta().name() : null);
        dto.setTipoPreguntaCerrada(p.getTipoPreguntaCerrada() != null ? p.getTipoPreguntaCerrada().name() : null);
        dto.setEsMixta(p.getEsMixta());
        dto.setIdEncuesta(p.getEncuesta().getIdEncuesta());

        List<OpcionResponseDTO> opciones = opcionRespuestaRepository
                .findByPreguntaIdPreguntaOrderByValorNumericoAsc(p.getIdPregunta())
                .stream().map(op -> {
                    OpcionResponseDTO o = new OpcionResponseDTO();
                    o.setIdOpcionRespuesta(op.getIdOpcionRespuesta());
                    o.setTextoOpcion(op.getTextoOpcion());
                    o.setValorNumerico(op.getValorNumerico());
                    return o;
                }).toList();
        dto.setOpciones(opciones);

        return dto;
    }
}
