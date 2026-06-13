package com.proyecto_bad115.sistema_encuestas.controller;

import com.proyecto_bad115.sistema_encuestas.dto.ParticipanteRequestDTO;
import com.proyecto_bad115.sistema_encuestas.dto.RespuestaEnvioDTO;
import com.proyecto_bad115.sistema_encuestas.service.PublicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Endpoints públicos del flujo del encuestado (sin autenticación). CU11-CU13.
 */
@RestController
@RequestMapping("/publico")
@CrossOrigin(origins = "*")
public class PublicoController {

    private final PublicoService publicoService;

    public PublicoController(PublicoService publicoService) {
        this.publicoService = publicoService;
    }

    @GetMapping("/encuestas/{token}")
    public ResponseEntity<?> cargar(@PathVariable String token) {
        try {
            return ResponseEntity.ok(publicoService.cargarEncuesta(token));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage()));
        }
    }

    @GetMapping("/encuestas/{token}/preguntas")
    public ResponseEntity<?> preguntas(@PathVariable String token) {
        try {
            return ResponseEntity.ok(publicoService.cargarPreguntas(token));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PostMapping("/encuestas/{token}/participar")
    public ResponseEntity<?> participar(@PathVariable String token,
                                        @Valid @RequestBody ParticipanteRequestDTO dto) {
        try {
            return ResponseEntity.ok(publicoService.registrarParticipante(token, dto));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PostMapping("/encuestas/{token}/responder")
    public ResponseEntity<?> responder(@PathVariable String token,
                                       @Valid @RequestBody RespuestaEnvioDTO dto) {
        try {
            return ResponseEntity.ok(publicoService.enviarRespuestas(token, dto));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", e.getMessage()));
        }
    }
}
