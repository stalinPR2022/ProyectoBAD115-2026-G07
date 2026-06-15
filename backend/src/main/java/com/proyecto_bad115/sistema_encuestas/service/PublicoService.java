package com.proyecto_bad115.sistema_encuestas.service;

import com.proyecto_bad115.sistema_encuestas.dto.*;
import com.proyecto_bad115.sistema_encuestas.model.*;
import com.proyecto_bad115.sistema_encuestas.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Flujo de respuesta del encuestado. La bienvenida y las preguntas se cargan
 * públicamente; el envío requiere autenticación (el encuestado se identifica por su cuenta).
 */
@Service
public class PublicoService {

    private final EncuestaRepository encuestaRepository;
    private final PreguntaRepository preguntaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RespuestaRepository respuestaRepository;
    private final DetalleRespuestaRepository detalleRespuestaRepository;
    private final OpcionRespuestaRepository opcionRespuestaRepository;
    private final PreguntaService preguntaService;

    public PublicoService(EncuestaRepository encuestaRepository,
                          PreguntaRepository preguntaRepository,
                          UsuarioRepository usuarioRepository,
                          RespuestaRepository respuestaRepository,
                          DetalleRespuestaRepository detalleRespuestaRepository,
                          OpcionRespuestaRepository opcionRespuestaRepository,
                          PreguntaService preguntaService) {
        this.encuestaRepository = encuestaRepository;
        this.preguntaRepository = preguntaRepository;
        this.usuarioRepository = usuarioRepository;
        this.respuestaRepository = respuestaRepository;
        this.detalleRespuestaRepository = detalleRespuestaRepository;
        this.opcionRespuestaRepository = opcionRespuestaRepository;
        this.preguntaService = preguntaService;
    }

    /** Carga la encuesta vigente a partir del token (pantalla de bienvenida, pública). */
    public EncuestaPublicaDTO cargarEncuesta(String token) {
        return toPublicaDTO(obtenerVigente(token));
    }

    /** CU12 - Carga las preguntas de la encuesta vigente para responderla. */
    public List<PreguntaResponseDTO> cargarPreguntas(String token) {
        Encuesta encuesta = obtenerVigente(token);
        return preguntaService.listarPorEncuesta(encuesta.getIdEncuesta());
    }

    /** Indica si el encuestado autenticado ya respondió esta encuesta. */
    public boolean yaRespondio(String token, String email) {
        Encuesta encuesta = obtenerVigente(token);
        return respuestaRepository.existsByEncuestaIdEncuestaAndUsuarioEmailUser(
                encuesta.getIdEncuesta(), normalizar(email));
    }

    /** CU13 - Registra de forma definitiva todas las respuestas del encuestado autenticado. */
    @Transactional
    public RespuestaConfirmacionDTO enviarRespuestas(String token, String email, List<DetalleEnvioDTO> respuestas) {
        Encuesta encuesta = obtenerVigente(token); // si cerró durante el llenado, falla aquí
        String correo = normalizar(email);

        Usuario usuario = usuarioRepository.findByEmailUser(correo)
                .orElseThrow(() -> new IllegalArgumentException("Tu sesión no es válida. Vuelve a iniciar sesión."));

        if (respuestaRepository.existsByEncuestaIdEncuestaAndUsuarioEmailUser(encuesta.getIdEncuesta(), correo)) {
            throw new IllegalStateException("Ya enviaste tus respuestas para esta encuesta");
        }

        List<Pregunta> preguntas = preguntaRepository.findByEncuestaIdEncuesta(encuesta.getIdEncuesta());
        Map<Integer, DetalleEnvioDTO> mapa = new HashMap<>();
        if (respuestas != null) {
            for (DetalleEnvioDTO d : respuestas) {
                if (d.getIdPregunta() != null) mapa.put(d.getIdPregunta(), d);
            }
        }

        // Validación final: todas las preguntas obligatorias deben estar respondidas
        for (Pregunta p : preguntas) {
            if (Boolean.TRUE.equals(p.getObligatoriaPregunta()) && !estaRespondida(p, mapa.get(p.getIdPregunta()))) {
                throw new IllegalArgumentException("Falta responder la pregunta obligatoria: \"" + p.getDescripcionPregunta() + "\"");
            }
        }

        Respuesta respuesta = new Respuesta();
        respuesta.setFechaRespuesta(LocalDate.now());
        respuesta.setUsuario(usuario);
        respuesta.setEncuesta(encuesta);
        Respuesta guardada = respuestaRepository.save(respuesta);

        for (Pregunta p : preguntas) {
            DetalleEnvioDTO d = mapa.get(p.getIdPregunta());
            if (d != null) guardarDetalles(p, d, guardada);
        }

        return new RespuestaConfirmacionDTO(guardada.getIdRespuesta(), guardada.getFechaRespuesta());
    }

    // ── Helpers ──────────────────────────────────────────────

    private boolean estaRespondida(Pregunta p, DetalleEnvioDTO d) {
        if (d == null) return false;
        if (p.getTipoPregunta() == TipoPregunta.ABIERTA) {
            return d.getTexto() != null && !d.getTexto().trim().isEmpty();
        }
        TipoPreguntaCerrada tc = p.getTipoPreguntaCerrada();
        if (tc == TipoPreguntaCerrada.ELECCION_MULTIPLE) {
            return d.getIdOpciones() != null && !d.getIdOpciones().isEmpty();
        }
        if (tc == TipoPreguntaCerrada.ESCALA) {
            return d.getValor() != null;
        }
        if (tc == TipoPreguntaCerrada.RANKING) {
            return d.getRanking() != null && !d.getRanking().isEmpty();
        }
        return d.getIdOpcion() != null; // única / likert / nominal
    }

    private void guardarDetalles(Pregunta p, DetalleEnvioDTO d, Respuesta respuesta) {
        if (p.getTipoPregunta() == TipoPregunta.ABIERTA) {
            if (d.getTexto() != null && !d.getTexto().trim().isEmpty()) {
                crearDetalle(respuesta, p, null, recortar(d.getTexto()), null);
            }
            return;
        }

        TipoPreguntaCerrada tc = p.getTipoPreguntaCerrada();
        if (tc == TipoPreguntaCerrada.ELECCION_MULTIPLE) {
            if (d.getIdOpciones() != null) {
                for (Integer idOp : d.getIdOpciones()) {
                    OpcionRespuesta op = buscarOpcion(idOp, p);
                    if (op != null) {
                        String texto = Boolean.TRUE.equals(op.getEsMixta()) ? recortar(d.getOtrosTexto()) : null;
                        crearDetalle(respuesta, p, op, texto, null);
                    }
                }
            }
        } else if (tc == TipoPreguntaCerrada.ESCALA) {
            if (d.getValor() != null) crearDetalle(respuesta, p, null, null, d.getValor());
        } else if (tc == TipoPreguntaCerrada.RANKING) {
            if (d.getRanking() != null) {
                int posicion = 1;
                for (Integer idOp : d.getRanking()) {
                    OpcionRespuesta op = buscarOpcion(idOp, p);
                    if (op != null) crearDetalle(respuesta, p, op, null, posicion);
                    posicion++;
                }
            }
        } else {
            // única / likert / nominal
            if (d.getIdOpcion() != null) {
                OpcionRespuesta op = buscarOpcion(d.getIdOpcion(), p);
                if (op != null) {
                    String texto = Boolean.TRUE.equals(op.getEsMixta()) ? recortar(d.getOtrosTexto()) : null;
                    crearDetalle(respuesta, p, op, texto, null);
                }
            }
        }
    }

    private OpcionRespuesta buscarOpcion(Integer idOpcion, Pregunta p) {
        if (idOpcion == null) return null;
        return opcionRespuestaRepository.findById(idOpcion)
                .filter(o -> o.getPregunta().getIdPregunta().equals(p.getIdPregunta()))
                .orElse(null);
    }

    private void crearDetalle(Respuesta respuesta, Pregunta pregunta, OpcionRespuesta opcion,
                              String texto, Integer valor) {
        DetalleRespuesta det = new DetalleRespuesta();
        det.setRespuesta(respuesta);
        det.setPregunta(pregunta);
        det.setOpcionRespuesta(opcion);
        det.setTextoRespuesta(texto);
        det.setValorRespuesta(valor);
        detalleRespuestaRepository.save(det);
    }

    private String recortar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() > 500 ? t.substring(0, 500) : t;
    }

    private Encuesta obtenerVigente(String token) {
        Encuesta encuesta = encuestaRepository.findByTokenPublico(token)
                .orElseThrow(() -> new NoSuchElementException("La encuesta no existe o el enlace no es válido"));

        if (encuesta.getEstadoEncuesta() == null || encuesta.getEstadoEncuesta() != EstadoEncuesta.PUBLICADA) {
            throw new IllegalStateException("Esta encuesta no está disponible para responder");
        }
        if (encuesta.getFechaCierre() != null && encuesta.getFechaCierre().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Esta encuesta ya cerró y no permite nuevas respuestas");
        }
        return encuesta;
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private EncuestaPublicaDTO toPublicaDTO(Encuesta e) {
        EncuestaPublicaDTO dto = new EncuestaPublicaDTO();
        dto.setIdEncuesta(e.getIdEncuesta());
        dto.setTituloEncuesta(e.getTituloEncuesta());
        dto.setObjetivoEncuesta(e.getObjetivoEncuesta());
        dto.setInstruccionesEncuesta(e.getInstruccionesEncuesta());
        dto.setGrupoMeta(e.getGrupoMeta());
        dto.setFechaCierre(e.getFechaCierre());
        dto.setTotalPreguntas(preguntaRepository.countByEncuestaIdEncuesta(e.getIdEncuesta()));
        return dto;
    }
}
