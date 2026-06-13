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

        boolean esMixta = tipo == TipoPregunta.CERRADA && Boolean.TRUE.equals(dto.getEsMixta());

        Pregunta pregunta = new Pregunta();
        pregunta.setDescripcionPregunta(dto.getDescripcionPregunta());
        pregunta.setObligatoriaPregunta(dto.getObligatoriaPregunta());
        pregunta.setTipoPregunta(tipo);
        pregunta.setEsMixta(esMixta);
        pregunta.setEncuesta(encuesta);

        if (tipo == TipoPregunta.CERRADA && dto.getTipoPreguntaCerrada() != null) {
            pregunta.setTipoPreguntaCerrada(TipoPreguntaCerrada.valueOf(dto.getTipoPreguntaCerrada()));
        }

        aplicarValidaciones(pregunta, dto, tipo, esMixta);

        Pregunta guardada = preguntaRepository.save(pregunta);
        guardarOpciones(guardada, dto.getOpciones(), esMixta);

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

        boolean esMixta = pregunta.getTipoPregunta() == TipoPregunta.CERRADA
                && Boolean.TRUE.equals(dto.getEsMixta());

        pregunta.setDescripcionPregunta(dto.getDescripcionPregunta());
        pregunta.setObligatoriaPregunta(dto.getObligatoriaPregunta());
        pregunta.setEsMixta(esMixta);

        aplicarValidaciones(pregunta, dto, pregunta.getTipoPregunta(), esMixta);

        opcionRespuestaRepository.deleteByPreguntaIdPregunta(idPregunta);
        guardarOpciones(pregunta, dto.getOpciones(), esMixta);

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

    private void guardarOpciones(Pregunta pregunta, List<String> textos, boolean esMixta) {
        if (textos == null || textos.isEmpty()) return;
        int orden = 0;
        for (; orden < textos.size(); orden++) {
            OpcionRespuesta op = new OpcionRespuesta();
            op.setTextoOpcion(textos.get(orden));
            op.setValorNumerico(orden + 1);
            op.setEsMixta(false);
            op.setPregunta(pregunta);
            opcionRespuestaRepository.save(op);
        }
        if (esMixta) {
            OpcionRespuesta otros = new OpcionRespuesta();
            otros.setTextoOpcion("Otros");
            otros.setValorNumerico(orden + 1);
            otros.setEsMixta(true);
            otros.setPregunta(pregunta);
            opcionRespuestaRepository.save(otros);
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

    /**
     * CU07 - Aplica y valida los criterios de validación de la pregunta.
     * ABIERTA: mín/máx de caracteres. ELECCION_MULTIPLE: máx de selecciones.
     * Limpia los campos que no apliquen al tipo.
     */
    private void aplicarValidaciones(Pregunta pregunta, PreguntaRequestDTO dto,
                                     TipoPregunta tipo, boolean esMixta) {
        if (tipo == TipoPregunta.ABIERTA) {
            Integer min = dto.getMinCaracteres();
            Integer max = dto.getMaxCaracteres();
            if (min != null && min < 0) {
                throw new IllegalArgumentException("El mínimo de caracteres no puede ser negativo");
            }
            if (max != null && max < 1) {
                throw new IllegalArgumentException("El máximo de caracteres debe ser mayor a 0");
            }
            if (min != null && max != null && min > max) {
                throw new IllegalArgumentException("El valor mínimo no puede ser mayor al máximo");
            }
            pregunta.setMinCaracteres(min);
            pregunta.setMaxCaracteres(max);
            pregunta.setMaxSelecciones(null);
        } else {
            // CERRADA: máx de selecciones solo para elección múltiple
            if ("ELECCION_MULTIPLE".equals(dto.getTipoPreguntaCerrada()) && dto.getMaxSelecciones() != null) {
                int maxSel = dto.getMaxSelecciones();
                long numOpciones = dto.getOpciones() == null ? 0
                        : dto.getOpciones().stream().filter(o -> o != null && !o.isBlank()).count();
                if (esMixta) numOpciones += 1; // la opción "Otros" también cuenta
                if (maxSel < 1) {
                    throw new IllegalArgumentException("El máximo de selecciones debe ser al menos 1");
                }
                if (maxSel > numOpciones) {
                    throw new IllegalArgumentException("El máximo de selecciones no puede superar el número de opciones");
                }
                pregunta.setMaxSelecciones(maxSel);
            } else {
                pregunta.setMaxSelecciones(null);
            }
            pregunta.setMinCaracteres(null);
            pregunta.setMaxCaracteres(null);
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
        dto.setMinCaracteres(p.getMinCaracteres());
        dto.setMaxCaracteres(p.getMaxCaracteres());
        dto.setMaxSelecciones(p.getMaxSelecciones());
        dto.setIdEncuesta(p.getEncuesta().getIdEncuesta());

        List<OpcionResponseDTO> opciones = opcionRespuestaRepository
                .findByPreguntaIdPreguntaOrderByValorNumericoAsc(p.getIdPregunta())
                .stream().map(op -> {
                    OpcionResponseDTO o = new OpcionResponseDTO();
                    o.setIdOpcionRespuesta(op.getIdOpcionRespuesta());
                    o.setTextoOpcion(op.getTextoOpcion());
                    o.setValorNumerico(op.getValorNumerico());
                    o.setEsMixta(op.getEsMixta());
                    return o;
                }).toList();
        dto.setOpciones(opciones);

        return dto;
    }
}
