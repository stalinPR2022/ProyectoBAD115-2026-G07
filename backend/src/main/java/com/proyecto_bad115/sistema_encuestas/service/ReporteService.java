package com.proyecto_bad115.sistema_encuestas.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.proyecto_bad115.sistema_encuestas.dto.ConteoOpcionDTO;
import com.proyecto_bad115.sistema_encuestas.dto.ResultadoPreguntaDTO;
import com.proyecto_bad115.sistema_encuestas.dto.ResultadosDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * CU10 - Genera reportes de resultados en Excel (XLSX), PDF y Word (DOCX).
 * Los reportes contienen tablas de frecuencia por pregunta.
 */
@Service
public class ReporteService {

    private static final Color AZUL = new Color(43, 87, 154);
    private final ResultadoService resultadoService;

    public ReporteService(ResultadoService resultadoService) {
        this.resultadoService = resultadoService;
    }

    private ResultadosDTO obtenerValidado(Integer idEncuesta) {
        ResultadosDTO r = resultadoService.obtener(idEncuesta);
        if (r.getTotalRespuestas() == 0) {
            throw new IllegalStateException("No hay datos suficientes para generar un reporte");
        }
        return r;
    }

    // ── EXCEL ─────────────────────────────────────────────
    public byte[] generarExcel(Integer idEncuesta) {
        ResultadosDTO r = obtenerValidado(idEncuesta);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Font bold = wb.createFont();
            bold.setBold(true);
            CellStyle estiloBold = wb.createCellStyle();
            estiloBold.setFont(bold);

            Sheet resumen = wb.createSheet("Resumen");
            int fila = 0;
            Cell tituloCell = resumen.createRow(fila++).createCell(0);
            tituloCell.setCellValue(r.getTituloEncuesta());
            tituloCell.setCellStyle(estiloBold);
            resumen.createRow(fila++).createCell(0).setCellValue("Estado: " + r.getEstadoNombre());
            resumen.createRow(fila++).createCell(0).setCellValue("Total de respuestas: " + r.getTotalRespuestas());
            if (r.getOpcionMasSeleccionada() != null) {
                resumen.createRow(fila++).createCell(0).setCellValue("Opción más seleccionada: " + r.getOpcionMasSeleccionada());
            }
            resumen.setColumnWidth(0, 14000);

            Sheet hoja = wb.createSheet("Resultados");
            int row = 0;
            int n = 1;
            for (ResultadoPreguntaDTO p : r.getPreguntas()) {
                Cell preg = hoja.createRow(row++).createCell(0);
                preg.setCellValue(n++ + ". " + p.getDescripcionPregunta());
                preg.setCellStyle(estiloBold);

                if ("texto".equals(p.getGraficoSugerido())) {
                    hoja.createRow(row++).createCell(0).setCellValue("Respuestas abiertas:");
                    if (p.getRespuestasTexto().isEmpty()) {
                        hoja.createRow(row++).createCell(0).setCellValue("(sin respuestas)");
                    } else {
                        for (String t : p.getRespuestasTexto()) {
                            hoja.createRow(row++).createCell(0).setCellValue("• " + t);
                        }
                    }
                } else {
                    Row encabezado = hoja.createRow(row++);
                    encabezado.createCell(0).setCellValue("Opción");
                    encabezado.createCell(1).setCellValue("Cantidad");
                    encabezado.createCell(2).setCellValue("Porcentaje");
                    encabezado.getCell(0).setCellStyle(estiloBold);
                    encabezado.getCell(1).setCellStyle(estiloBold);
                    encabezado.getCell(2).setCellStyle(estiloBold);
                    for (ConteoOpcionDTO o : p.getOpciones()) {
                        Row dr = hoja.createRow(row++);
                        dr.createCell(0).setCellValue(o.getEtiqueta());
                        dr.createCell(1).setCellValue(o.getCantidad());
                        dr.createCell(2).setCellValue(o.getPorcentaje() + "%");
                    }
                }
                row++; // fila en blanco
            }
            hoja.setColumnWidth(0, 16000);
            hoja.setColumnWidth(1, 4000);
            hoja.setColumnWidth(2, 4000);

            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el reporte Excel", e);
        }
    }

    // ── WORD ──────────────────────────────────────────────
    public byte[] generarWord(Integer idEncuesta) {
        ResultadosDTO r = obtenerValidado(idEncuesta);
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            XWPFParagraph titulo = doc.createParagraph();
            titulo.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun tr = titulo.createRun();
            tr.setText(r.getTituloEncuesta());
            tr.setBold(true);
            tr.setFontSize(18);

            lineaWord(doc, "Estado: " + r.getEstadoNombre());
            lineaWord(doc, "Total de respuestas: " + r.getTotalRespuestas());
            if (r.getOpcionMasSeleccionada() != null) {
                lineaWord(doc, "Opción más seleccionada: " + r.getOpcionMasSeleccionada());
            }

            int n = 1;
            for (ResultadoPreguntaDTO p : r.getPreguntas()) {
                XWPFParagraph hp = doc.createParagraph();
                hp.setSpacingBefore(220);
                XWPFRun hr = hp.createRun();
                hr.setText(n++ + ". " + p.getDescripcionPregunta());
                hr.setBold(true);
                hr.setFontSize(12);

                if ("texto".equals(p.getGraficoSugerido())) {
                    if (p.getRespuestasTexto().isEmpty()) {
                        lineaWord(doc, "(sin respuestas)");
                    } else {
                        for (String t : p.getRespuestasTexto()) lineaWord(doc, "• " + t);
                    }
                } else {
                    XWPFTable tabla = doc.createTable(1, 3);
                    tabla.getRow(0).getCell(0).setText("Opción");
                    tabla.getRow(0).getCell(1).setText("Cantidad");
                    tabla.getRow(0).getCell(2).setText("Porcentaje");
                    for (ConteoOpcionDTO o : p.getOpciones()) {
                        XWPFTableRow tabRow = tabla.createRow();
                        tabRow.getCell(0).setText(o.getEtiqueta());
                        tabRow.getCell(1).setText(String.valueOf(o.getCantidad()));
                        tabRow.getCell(2).setText(o.getPorcentaje() + "%");
                    }
                }
            }

            doc.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el reporte Word", e);
        }
    }

    private void lineaWord(XWPFDocument doc, String texto) {
        XWPFParagraph p = doc.createParagraph();
        p.createRun().setText(texto);
    }

    // ── PDF ───────────────────────────────────────────────
    public byte[] generarPdf(Integer idEncuesta) {
        ResultadosDTO r = obtenerValidado(idEncuesta);
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, bos);
            document.open();

            Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, AZUL);
            Font fSub = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);
            Font fPreg = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fCell = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            document.add(new Paragraph(r.getTituloEncuesta(), fTitulo));
            document.add(new Paragraph("Estado: " + r.getEstadoNombre(), fSub));
            document.add(new Paragraph("Total de respuestas: " + r.getTotalRespuestas(), fSub));
            if (r.getOpcionMasSeleccionada() != null) {
                document.add(new Paragraph("Opción más seleccionada: " + r.getOpcionMasSeleccionada(), fSub));
            }
            document.add(Chunk.NEWLINE);

            int n = 1;
            for (ResultadoPreguntaDTO p : r.getPreguntas()) {
                Paragraph hp = new Paragraph(n++ + ". " + p.getDescripcionPregunta(), fPreg);
                hp.setSpacingBefore(12);
                hp.setSpacingAfter(6);
                document.add(hp);

                if ("texto".equals(p.getGraficoSugerido())) {
                    if (p.getRespuestasTexto().isEmpty()) {
                        document.add(new Paragraph("(sin respuestas)", fCell));
                    } else {
                        for (String t : p.getRespuestasTexto()) document.add(new Paragraph("• " + t, fCell));
                    }
                } else {
                    PdfPTable tabla = new PdfPTable(3);
                    tabla.setWidthPercentage(100);
                    tabla.setWidths(new float[]{3, 1, 1});
                    celdaCabecera(tabla, "Opción", fHead);
                    celdaCabecera(tabla, "Cantidad", fHead);
                    celdaCabecera(tabla, "Porcentaje", fHead);
                    for (ConteoOpcionDTO o : p.getOpciones()) {
                        tabla.addCell(new Phrase(o.getEtiqueta(), fCell));
                        tabla.addCell(new Phrase(String.valueOf(o.getCantidad()), fCell));
                        tabla.addCell(new Phrase(o.getPorcentaje() + "%", fCell));
                    }
                    document.add(tabla);
                }
            }

            document.close();
            return bos.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar el reporte PDF", e);
        }
    }

    private void celdaCabecera(PdfPTable tabla, String texto, Font font) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, font));
        celda.setBackgroundColor(AZUL);
        celda.setPadding(5);
        tabla.addCell(celda);
    }
}
