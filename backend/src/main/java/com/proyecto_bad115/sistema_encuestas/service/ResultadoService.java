package com.proyecto_bad115.sistema_encuestas.service;

import com.proyecto_bad115.sistema_encuestas.dto.ConteoOpcionDTO;
import com.proyecto_bad115.sistema_encuestas.dto.ResultadoPreguntaDTO;
import com.proyecto_bad115.sistema_encuestas.dto.ResultadosDTO;
import com.proyecto_bad115.sistema_encuestas.model.*;
import com.proyecto_bad115.sistema_encuestas.repository.DetalleRespuestaRepository;
import com.proyecto_bad115.sistema_encuestas.repository.EncuestaRepository;
import com.proyecto_bad115.sistema_encuestas.repository.OpcionRespuestaRepository;
import com.proyecto_bad115.sistema_encuestas.repository.PreguntaRepository;
import com.proyecto_bad115.sistema_encuestas.repository.RespuestaRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * CU09 - Procesa las respuestas de una encuesta y construye las estadísticas
 * de frecuencia por pregunta para el panel de resultados.
 */
@Service
public class ResultadoService {

    private final EncuestaRepository encuestaRepository;
    private final PreguntaRepository preguntaRepository;
    private final OpcionRespuestaRepository opcionRespuestaRepository;
    private final DetalleRespuestaRepository detalleRespuestaRepository;
    private final RespuestaRepository respuestaRepository;

    public ResultadoService(EncuestaRepository encuestaRepository,
                            PreguntaRepository preguntaRepository,
                            OpcionRespuestaRepository opcionRespuestaRepository,
                            DetalleRespuestaRepository detalleRespuestaRepository,
                            RespuestaRepository respuestaRepository) {
        this.encuestaRepository = encuestaRepository;
        this.preguntaRepository = preguntaRepository;
        this.opcionRespuestaRepository = opcionRespuestaRepository;
        this.detalleRespuestaRepository = detalleRespuestaRepository;
        this.respuestaRepository = respuestaRepository;
    }

    public ResultadosDTO obtener(Integer idEncuesta) {
        Encuesta encuesta = encuestaRepository.findById(idEncuesta)
                .orElseThrow(() -> new NoSuchElementException("Encuesta no encontrada"));

        ResultadosDTO dto = new ResultadosDTO();
        dto.setIdEncuesta(encuesta.getIdEncuesta());
        dto.setTituloEncuesta(encuesta.getTituloEncuesta());
        dto.setEstadoEncuesta(encuesta.getEstadoEncuesta());
        dto.setEstadoNombre(nombreEstado(encuesta.getEstadoEncuesta()));
        dto.setTotalRespuestas(respuestaRepository.countByEncuestaIdEncuesta(idEncuesta));

        long maxGlobal = 0;
        String masSeleccionada = null;

        for (Pregunta p : preguntaRepository.findByEncuestaIdEncuesta(idEncuesta)) {
            ResultadoPreguntaDTO rp = procesar(p);
            dto.getPreguntas().add(rp);

            if (!"texto".equals(rp.getGraficoSugerido())) {
                for (ConteoOpcionDTO c : rp.getOpciones()) {
                    if (c.getCantidad() > maxGlobal) {
                        maxGlobal = c.getCantidad();
                        masSeleccionada = c.getEtiqueta();
                    }
                }
            }
        }
        dto.setOpcionMasSeleccionada(masSeleccionada);
        return dto;
    }

    private ResultadoPreguntaDTO procesar(Pregunta p) {
        ResultadoPreguntaDTO rp = new ResultadoPreguntaDTO();
        rp.setIdPregunta(p.getIdPregunta());
        rp.setDescripcionPregunta(p.getDescripcionPregunta());
        rp.setTipoPregunta(p.getTipoPregunta() != null ? p.getTipoPregunta().name() : null);
        rp.setTipoPreguntaCerrada(p.getTipoPreguntaCerrada() != null ? p.getTipoPreguntaCerrada().name() : null);
        rp.setEsMixta(p.getEsMixta());
        rp.setGraficoSugerido(sugerir(p));

        List<DetalleRespuesta> detalles = detalleRespuestaRepository.findByPreguntaIdPregunta(p.getIdPregunta());

        Set<Integer> respondentes = new HashSet<>();
        for (DetalleRespuesta d : detalles) respondentes.add(d.getRespuesta().getIdRespuesta());
        rp.setTotalRespuestas(respondentes.size());

        // ABIERTA: lista de textos
        if (p.getTipoPregunta() == TipoPregunta.ABIERTA) {
            for (DetalleRespuesta d : detalles) {
                if (d.getTextoRespuesta() != null && !d.getTextoRespuesta().isBlank()) {
                    rp.getRespuestasTexto().add(d.getTextoRespuesta());
                }
            }
            return rp;
        }

        List<OpcionRespuesta> opciones =
                opcionRespuestaRepository.findByPreguntaIdPreguntaOrderByValorNumericoAsc(p.getIdPregunta());
        TipoPreguntaCerrada tc = p.getTipoPreguntaCerrada();

        // ESCALA: distribución de valores del rango
        if (tc == TipoPreguntaCerrada.ESCALA && opciones.size() >= 2) {
            int min = parseEntero(opciones.get(0).getTextoOpcion(), 1);
            int max = parseEntero(opciones.get(1).getTextoOpcion(), 5);
            Map<Integer, Long> conteo = new TreeMap<>();
            for (int v = min; v <= max; v++) conteo.put(v, 0L);
            for (DetalleRespuesta d : detalles) {
                if (d.getValorRespuesta() != null) conteo.merge(d.getValorRespuesta(), 1L, Long::sum);
            }
            long total = conteo.values().stream().mapToLong(Long::longValue).sum();
            conteo.forEach((v, c) ->
                    rp.getOpciones().add(new ConteoOpcionDTO(String.valueOf(v), c, porcentaje(c, total))));
            return rp;
        }

        // RANKING: puntaje ponderado por posición (más alto = más preferido)
        if (tc == TipoPreguntaCerrada.RANKING) {
            int n = opciones.size();
            Map<Integer, Long> puntaje = new LinkedHashMap<>();
            Map<Integer, String> texto = new LinkedHashMap<>();
            for (OpcionRespuesta o : opciones) {
                puntaje.put(o.getIdOpcionRespuesta(), 0L);
                texto.put(o.getIdOpcionRespuesta(), o.getTextoOpcion());
            }
            for (DetalleRespuesta d : detalles) {
                if (d.getOpcionRespuesta() != null && d.getValorRespuesta() != null) {
                    long pts = n - d.getValorRespuesta() + 1L;
                    puntaje.merge(d.getOpcionRespuesta().getIdOpcionRespuesta(), pts, Long::sum);
                }
            }
            long total = puntaje.values().stream().mapToLong(Long::longValue).sum();
            puntaje.forEach((id, s) ->
                    rp.getOpciones().add(new ConteoOpcionDTO(texto.get(id), s, porcentaje(s, total))));
            return rp;
        }

        // ÚNICA / MÚLTIPLE / LIKERT / NOMINAL: conteo por opción
        Map<Integer, Long> conteo = new LinkedHashMap<>();
        Map<Integer, String> etiqueta = new LinkedHashMap<>();
        for (OpcionRespuesta o : opciones) {
            conteo.put(o.getIdOpcionRespuesta(), 0L);
            etiqueta.put(o.getIdOpcionRespuesta(), Boolean.TRUE.equals(o.getEsMixta()) ? "Otros" : o.getTextoOpcion());
        }
        for (DetalleRespuesta d : detalles) {
            if (d.getOpcionRespuesta() != null) {
                conteo.merge(d.getOpcionRespuesta().getIdOpcionRespuesta(), 1L, Long::sum);
            }
        }
        long total = conteo.values().stream().mapToLong(Long::longValue).sum();
        conteo.forEach((id, c) ->
                rp.getOpciones().add(new ConteoOpcionDTO(etiqueta.get(id), c, porcentaje(c, total))));
        return rp;
    }

    private String sugerir(Pregunta p) {
        if (p.getTipoPregunta() == TipoPregunta.ABIERTA) return "texto";
        if (p.getTipoPreguntaCerrada() == null) return "barras";
        return switch (p.getTipoPreguntaCerrada()) {
            case ELECCION_MULTIPLE, RANKING, NOMINAL -> "barras";
            case ESCALA, LIKERT -> "linea";
            default -> "pastel"; // ELECCION_UNICA (dicotómica / politómica)
        };
    }

    private int parseEntero(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double porcentaje(long cantidad, long total) {
        if (total == 0) return 0;
        return Math.round(cantidad * 1000.0 / total) / 10.0;
    }

    private String nombreEstado(Integer estado) {
        if (estado == null) return "Desconocido";
        return switch (estado) {
            case EstadoEncuesta.EN_DISENO -> "En Diseño";
            case EstadoEncuesta.PUBLICADA -> "Publicada";
            case EstadoEncuesta.CERRADA -> "Cerrada";
            default -> "Desconocido";
        };
    }
}
