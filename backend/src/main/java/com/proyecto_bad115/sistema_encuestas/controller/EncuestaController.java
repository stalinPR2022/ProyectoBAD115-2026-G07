package com.proyecto_bad115.sistema_encuestas.controller;

import com.proyecto_bad115.sistema_encuestas.dto.EncuestaRequestDTO;
import com.proyecto_bad115.sistema_encuestas.dto.EncuestaResponseDTO;
import com.proyecto_bad115.sistema_encuestas.service.EncuestaService;
import com.proyecto_bad115.sistema_encuestas.service.ResultadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/encuestas")
@CrossOrigin(origins = "*")
public class EncuestaController {

    private final EncuestaService encuestaService;
    private final ResultadoService resultadoService;

    public EncuestaController(EncuestaService encuestaService, ResultadoService resultadoService) {
        this.encuestaService = encuestaService;
        this.resultadoService = resultadoService;
    }

    @GetMapping
    public ResponseEntity<List<EncuestaResponseDTO>> listar(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(encuestaService.listarPorUsuario(email));
    }

    @GetMapping("/todas")
    public ResponseEntity<List<EncuestaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(encuestaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(encuestaService.buscarPorId(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody EncuestaRequestDTO dto,
                                   @AuthenticationPrincipal String email) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(encuestaService.crear(dto, email));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id,
                                        @Valid @RequestBody EncuestaRequestDTO dto) {
        try {
            return ResponseEntity.ok(encuestaService.actualizar(id, dto));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", e.getMessage()));
        }
    }

    @GetMapping("/{id}/resultados")
    public ResponseEntity<?> resultados(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(resultadoService.obtener(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PostMapping("/{id}/publicar")
    public ResponseEntity<?> publicar(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(encuestaService.publicar(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            encuestaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", e.getMessage()));
        }
    }
}
