package com.proyecto_bad115.sistema_encuestas.controller;

import com.proyecto_bad115.sistema_encuestas.dto.EncuestaDisponibleDTO;
import com.proyecto_bad115.sistema_encuestas.service.PublicoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catálogo de encuestas publicadas que cualquier usuario autenticado puede responder.
 */
@RestController
@RequestMapping("/encuestas-disponibles")
@CrossOrigin(origins = "*")
public class EncuestasDisponiblesController {

    private final PublicoService publicoService;

    public EncuestasDisponiblesController(PublicoService publicoService) {
        this.publicoService = publicoService;
    }

    @GetMapping
    public ResponseEntity<List<EncuestaDisponibleDTO>> listar(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(publicoService.encuestasDisponibles(email));
    }
}
