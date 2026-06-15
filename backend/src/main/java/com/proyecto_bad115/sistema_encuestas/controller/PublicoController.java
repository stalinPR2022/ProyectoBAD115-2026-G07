package com.proyecto_bad115.sistema_encuestas.controller;

import com.proyecto_bad115.sistema_encuestas.service.PublicoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Endpoints públicos del flujo del encuestado (sin autenticación):
 * bienvenida y preguntas. El envío está en ResponderController (autenticado).
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
}
