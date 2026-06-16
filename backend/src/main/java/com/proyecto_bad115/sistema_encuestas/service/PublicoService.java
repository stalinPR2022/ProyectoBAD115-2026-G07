package com.proyecto_bad115.sistema_encuestas.service;

import com.proyecto_bad115.sistema_encuestas.dto.*;
import com.proyecto_bad115.sistema_encuestas.model.*;
import com.proyecto_bad115.sistema_encuestas.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

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

    /** Etapa 18 - Encuestas del encuestado: borradores (en progreso) + respondidas. */
    public List<MiEncuestaDTO> misEncuestas(String email) {
        String correo = normalizar(email);
        List<MiEncuestaDTO> resultado = new ArrayList<>();
        respuestaRepository.findByUsuarioEmailUserAndEstadoRespuesta(correo, EstadoRespuesta.BORRADOR)
                .forEach(r -> resultado.add(toMiEncuestaDTO(r)));
        respuestaRepository.findByUsuarioEmailUserAndEstadoRespuesta(correo, EstadoRespuesta.ENVIADA)
                .forEach(r -> resultado.add(toMiEncuestaDTO(r)));
        return resultado;
    }

    /** Catálogo de encuestas publicadas y vigentes que cualquier usuario puede responder. */
    public List<EncuestaDisponibleDTO> encuestasDisponibles(String email) {
        String correo = normalizar(email);
        LocalDate hoy = LocalDate.now();
        return encuestaRepository.findByEstadoEncuesta(EstadoEncuesta.PUBLICADA).stream()
                .filter(e -> e.getFechaCierre() == null || !e.getFechaCierre().isBefore(hoy))
                .map(e -> toDisponibleDTO(e, correo))
                .toList();
    }

    private EncuestaDisponibleDTO toDisponibleDTO(Encuesta e, String correo) {
        EncuestaDisponibleDTO dto = new EncuestaDisponibleDTO();
        dto.setIdEncuesta(e.getIdEncuesta());
        dto.setTituloEncuesta(e.getTituloEncuesta());
        dto.setObjetivoEncuesta(e.getObjetivoEncuesta());
        dto.setGrupoMeta(e.getGrupoMeta());
        dto.setFechaCierre(e.getFechaCierre());
        dto.setTotalPreguntas(preguntaRepository.countByEncuestaIdEncuesta(e.getIdEncuesta()));
        dto.setTokenPublico(e.getTokenPublico());

        Integer estado = null;
        if (respuestaRepository.existsByEncuestaIdEncuestaAndUsuarioEmailUserAndEstadoRespuesta(
                e.getIdEncuesta(), correo, EstadoRespuesta.ENVIADA)) {
            estado = EstadoRespuesta.ENVIADA;
        } else if (respuestaRepository.findFirstByEncuestaIdEncuestaAndUsuarioEmailUserAndEstadoRespuesta(
                e.getIdEncuesta(), correo, EstadoRespuesta.BORRADOR).isPresent()) {
            estado = EstadoRespuesta.BORRADOR;
        }
        dto.setEstadoRespuesta(estado);
        return dto;
    }

    private MiEncuestaDTO toMiEncuestaDTO(Respuesta r) {
        Encuesta e = r.getEncuesta();
        boolean borrador = r.getEstadoRespuesta() != null && r.getEstadoRespuesta() == EstadoRespuesta.BORRADOR;
        MiEncuestaDTO dto = new MiEncuestaDTO();
        dto.setIdEncuesta(e.getIdEncuesta());
        dto.setTituloEncuesta(e.getTituloEncuesta());
        dto.setObjetivoEncuesta(e.getObjetivoEncuesta());
        dto.setTokenPublico(e.getTokenPublico());
        dto.setEstadoRespuesta(r.getEstadoRespuesta());
        dto.setEstadoNombre(borrador ? "En progreso" : "Respondida");
        dto.setFecha(borrador ? r.getFechaActualizacion() : r.getFechaRespuesta());
        dto.setNumeroRegistro(borrador ? null : r.getIdRespuesta());
        return dto;
    }

    /** Indica si el encuestado autenticado ya ENVIÓ esta encuesta. */
    public boolean yaRespondio(String token, String email) {
        Encuesta encuesta = obtenerVigente(token);
        return respuestaRepository.existsByEncuestaIdEncuestaAndUsuarioEmailUserAndEstadoRespuesta(
                encuesta.getIdEncuesta(), normalizar(email), EstadoRespuesta.ENVIADA);
    }

    /** Indica si el encuestado tiene un borrador guardado para reanudar. */
    public boolean tieneBorrador(String token, String email) {
        Encuesta encuesta = obtenerVigente(token);
        return respuestaRepository.findFirstByEncuestaIdEncuestaAndUsuarioEmailUserAndEstadoRespuesta(
                encuesta.getIdEncuesta(), normalizar(email), EstadoRespuesta.BORRADOR).isPresent();
    }

    /** Etapa 17 - Devuelve las respuestas guardadas en el borrador (para reanudar). */
    public List<DetalleEnvioDTO> obtenerBorrador(String token, String email) {
        Encuesta encuesta = obtenerVigente(token);
        return respuestaRepository.findFirstByEncuestaIdEncuestaAndUsuarioEmailUserAndEstadoRespuesta(
                        encuesta.getIdEncuesta(), normalizar(email), EstadoRespuesta.BORRADOR)
                .map(b -> reconstruir(detalleRespuestaRepository.findByRespuestaIdRespuesta(b.getIdRespuesta())))
                .orElseGet(ArrayList::new);
    }

    /** Etapa 17 - Guarda (o actualiza) el progreso parcial como borrador ("terminar más tarde"). */
    @Transactional
    public void guardarBorrador(String token, String email, List<DetalleEnvioDTO> respuestas) {
        Encuesta encuesta = obtenerVigente(token);
        String correo = normalizar(email);
        Usuario usuario = usuarioRepository.findByEmailUser(correo)
                .orElseThrow(() -> new IllegalArgumentException("Tu sesión no es válida. Vuelve a iniciar sesión."));

        if (respuestaRepository.existsByEncuestaIdEncuestaAndUsuarioEmailUserAndEstadoRespuesta(
                encuesta.getIdEncuesta(), correo, EstadoRespuesta.ENVIADA)) {
            throw new IllegalStateException("Ya enviaste esta encuesta; no puedes guardar un borrador");
        }

        Respuesta borrador = respuestaRepository
                .findFirstByEncuestaIdEncuestaAndUsuarioEmailUserAndEstadoRespuesta(
                        encuesta.getIdEncuesta(), correo, EstadoRespuesta.BORRADOR)
                .orElseGet(() -> {
                    Respuesta r = new Respuesta();
                    r.setUsuario(usuario);
                    r.setEncuesta(encuesta);
                    r.setEstadoRespuesta(EstadoRespuesta.BORRADOR);
                    return r;
                });
        borrador.setFechaActualizacion(LocalDate.now());
        Respuesta guardada = respuestaRepository.save(borrador);

        // Reemplaza los detalles previos del borrador
        detalleRespuestaRepository.deleteByRespuestaIdRespuesta(guardada.getIdRespuesta());
        guardarTodos(encuesta, respuestas, guardada);
    }

    /** CU13 - Registra de forma definitiva todas las respuestas del encuestado autenticado. */
    @Transactional
    public RespuestaConfirmacionDTO enviarRespuestas(String token, String email, List<DetalleEnvioDTO> respuestas) {
        Encuesta encuesta = obtenerVigente(token); // si cerró durante el llenado, falla aquí
        String correo = normalizar(email);

        Usuario usuario = usuarioRepository.findByEmailUser(correo)
                .orElseThrow(() -> new IllegalArgumentException("Tu sesión no es válida. Vuelve a iniciar sesión."));

        if (respuestaRepository.existsByEncuestaIdEncuestaAndUsuarioEmailUserAndEstadoRespuesta(
                encuesta.getIdEncuesta(), correo, EstadoRespuesta.ENVIADA)) {
            throw new IllegalStateException("Ya enviaste tus respuestas para esta encuesta");
        }

        List<Pregunta> preguntas = preguntaRepository.findByEncuestaIdEncuesta(encuesta.getIdEncuesta());
        Map<Integer, DetalleEnvioDTO> mapa = indexar(respuestas);

        // Validación final: todas las preguntas obligatorias deben estar respondidas
        for (Pregunta p : preguntas) {
            if (Boolean.TRUE.equals(p.getObligatoriaPregunta()) && !estaRespondida(p, mapa.get(p.getIdPregunta()))) {
                throw new IllegalArgumentException("Falta responder la pregunta obligatoria: \"" + p.getDescripcionPregunta() + "\"");
            }
        }

        // Reutiliza el borrador si existe (lo convierte en ENVIADA); si no, crea uno nuevo
        Respuesta respuesta = respuestaRepository
                .findFirstByEncuestaIdEncuestaAndUsuarioEmailUserAndEstadoRespuesta(
                        encuesta.getIdEncuesta(), correo, EstadoRespuesta.BORRADOR)
                .orElseGet(Respuesta::new);
        respuesta.setUsuario(usuario);
        respuesta.setEncuesta(encuesta);
        respuesta.setEstadoRespuesta(EstadoRespuesta.ENVIADA);
        respuesta.setFechaRespuesta(LocalDate.now());
        respuesta.setFechaActualizacion(LocalDate.now());
        Respuesta guardada = respuestaRepository.save(respuesta);

        // Si venía de un borrador, limpia sus detalles previos antes de re-guardar
        detalleRespuestaRepository.deleteByRespuestaIdRespuesta(guardada.getIdRespuesta());
        guardarTodos(encuesta, respuestas, guardada);

        return new RespuestaConfirmacionDTO(guardada.getIdRespuesta(), guardada.getFechaRespuesta());
    }

    private void guardarTodos(Encuesta encuesta, List<DetalleEnvioDTO> respuestas, Respuesta destino) {
        Map<Integer, DetalleEnvioDTO> mapa = indexar(respuestas);
        for (Pregunta p : preguntaRepository.findByEncuestaIdEncuesta(encuesta.getIdEncuesta())) {
            DetalleEnvioDTO d = mapa.get(p.getIdPregunta());
            if (d != null) guardarDetalles(p, d, destino);
        }
    }

    private Map<Integer, DetalleEnvioDTO> indexar(List<DetalleEnvioDTO> respuestas) {
        Map<Integer, DetalleEnvioDTO> mapa = new HashMap<>();
        if (respuestas != null) {
            for (DetalleEnvioDTO d : respuestas) {
                if (d.getIdPregunta() != null) mapa.put(d.getIdPregunta(), d);
            }
        }
        return mapa;
    }

    /** Reconstruye los DetalleEnvioDTO (formato del cliente) a partir de los detalles guardados. */
    private List<DetalleEnvioDTO> reconstruir(List<DetalleRespuesta> detalles) {
        Map<Integer, DetalleEnvioDTO> mapa = new LinkedHashMap<>();
        Map<Integer, TreeMap<Integer, Integer>> rankings = new HashMap<>(); // idPregunta -> (posición -> idOpcion)

        for (DetalleRespuesta d : detalles) {
            int idPreg = d.getPregunta().getIdPregunta();
            DetalleEnvioDTO dto = mapa.computeIfAbsent(idPreg, k -> {
                DetalleEnvioDTO x = new DetalleEnvioDTO();
                x.setIdPregunta(k);
                x.setIdOpciones(new ArrayList<>());
                x.setRanking(new ArrayList<>());
                return x;
            });

            OpcionRespuesta op = d.getOpcionRespuesta();
            if (op == null) {
                if (d.getValorRespuesta() != null) dto.setValor(d.getValorRespuesta());      // escala
                else if (d.getTextoRespuesta() != null) dto.setTexto(d.getTextoRespuesta()); // abierta
            } else if (d.getValorRespuesta() != null) {
                // ranking: la posición es el valorRespuesta
                rankings.computeIfAbsent(idPreg, k -> new TreeMap<>())
                        .put(d.getValorRespuesta(), op.getIdOpcionRespuesta());
            } else {
                // selección única o múltiple
                dto.getIdOpciones().add(op.getIdOpcionRespuesta());
                if (dto.getIdOpcion() == null) dto.setIdOpcion(op.getIdOpcionRespuesta());
                if (Boolean.TRUE.equals(op.getEsMixta()) && d.getTextoRespuesta() != null) {
                    dto.setOtrosTexto(d.getTextoRespuesta());
                }
            }
        }
        rankings.forEach((idPreg, posMap) -> mapa.get(idPreg).setRanking(new ArrayList<>(posMap.values())));
        return new ArrayList<>(mapa.values());
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
