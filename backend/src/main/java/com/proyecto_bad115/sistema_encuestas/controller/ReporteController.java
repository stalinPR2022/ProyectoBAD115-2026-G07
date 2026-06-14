package com.proyecto_bad115.sistema_encuestas.controller;

import com.proyecto_bad115.sistema_encuestas.service.ReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * CU10 - Descarga de reportes de resultados en distintos formatos.
 */
@RestController
@RequestMapping("/encuestas")
@CrossOrigin(origins = "*")
public class ReporteController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/{id}/reporte/excel")
    public ResponseEntity<?> excel(@PathVariable Integer id) {
        return generar(() -> reporteService.generarExcel(id), "reporte_encuesta_" + id + ".xlsx", XLSX);
    }

    @GetMapping("/{id}/reporte/word")
    public ResponseEntity<?> word(@PathVariable Integer id) {
        return generar(() -> reporteService.generarWord(id), "reporte_encuesta_" + id + ".docx", DOCX);
    }

    @GetMapping("/{id}/reporte/pdf")
    public ResponseEntity<?> pdf(@PathVariable Integer id) {
        return generar(() -> reporteService.generarPdf(id), "reporte_encuesta_" + id + ".pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    private ResponseEntity<?> generar(Supplier<byte[]> generador, String nombre, String contentType) {
        try {
            byte[] data = generador.get();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(data);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", e.getMessage()));
        }
    }
}
