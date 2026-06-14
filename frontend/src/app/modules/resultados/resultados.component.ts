import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ResultadoService, Resultados, ResultadoPregunta, ConteoOpcion } from '../../core/services/resultado.service';

type TipoGrafico = 'pastel' | 'barras' | 'linea';

interface Slice extends ConteoOpcion { d: string; color: string; }
interface Punto { x: number; y: number; etiqueta: string; cantidad: number; }

@Component({
  selector: 'app-resultados',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './resultados.component.html',
  styleUrl: './resultados.component.css'
})
export class ResultadosComponent implements OnInit {
  idEncuesta!: number;
  resultados: Resultados | null = null;
  cargando = true;
  error = '';
  tipoGrafico: Record<number, TipoGrafico> = {};

  // CU10 - Reporte
  mostrarReporte = false;
  generando = '';
  errorReporte = '';

  private readonly paleta = [
    '#2B579A', '#00A99D', '#E8833A', '#9B59B6', '#3FA796',
    '#D7556A', '#5B8DEF', '#F2B134', '#48C78E', '#6C7A89'
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private resultadoService: ResultadoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.idEncuesta = Number(this.route.snapshot.paramMap.get('idEncuesta'));
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.error = '';
    this.resultadoService.obtener(this.idEncuesta).subscribe({
      next: (data) => {
        this.resultados = data;
        data.preguntas.forEach(p => {
          this.tipoGrafico[p.idPregunta] = p.graficoSugerido === 'texto' ? 'barras' : p.graficoSugerido;
        });
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (e) => {
        this.error = e.error?.mensaje || 'No se pudieron cargar los resultados.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  volver(): void { this.router.navigate(['/dashboard/encuestas']); }

  color(i: number): string { return this.paleta[i % this.paleta.length]; }

  esTexto(p: ResultadoPregunta): boolean { return p.graficoSugerido === 'texto'; }

  cambiarGrafico(p: ResultadoPregunta, tipo: TipoGrafico): void {
    this.tipoGrafico[p.idPregunta] = tipo;
  }

  totalConteo(p: ResultadoPregunta): number {
    return p.opciones.reduce((s, o) => s + o.cantidad, 0);
  }

  maxConteo(p: ResultadoPregunta): number {
    return Math.max(1, ...p.opciones.map(o => o.cantidad));
  }

  // ── Gráfico de pastel ─────────────────────────────────
  slices(p: ResultadoPregunta): Slice[] {
    const total = this.totalConteo(p);
    if (total === 0) return [];
    let ang = -Math.PI / 2;
    return p.opciones.map((o, i) => {
      const frac = o.cantidad / total;
      const start = ang;
      const end = ang + frac * 2 * Math.PI;
      ang = end;
      return { ...o, color: this.color(i), d: this.arco(100, 100, 85, start, end) };
    });
  }

  private arco(cx: number, cy: number, r: number, start: number, end: number): string {
    if (end - start >= 2 * Math.PI - 0.0001) {
      return `M ${cx - r} ${cy} A ${r} ${r} 0 1 1 ${cx + r} ${cy} A ${r} ${r} 0 1 1 ${cx - r} ${cy} Z`;
    }
    const x1 = cx + r * Math.cos(start), y1 = cy + r * Math.sin(start);
    const x2 = cx + r * Math.cos(end), y2 = cy + r * Math.sin(end);
    const large = (end - start) > Math.PI ? 1 : 0;
    return `M ${cx} ${cy} L ${x1} ${y1} A ${r} ${r} 0 ${large} 1 ${x2} ${y2} Z`;
  }

  // ── Gráfico de líneas ─────────────────────────────────
  puntosLinea(p: ResultadoPregunta): Punto[] {
    const op = p.opciones;
    const max = this.maxConteo(p);
    const W = 320, padX = 26, padTop = 14, padBottom = 30;
    const innerW = W - padX * 2, innerH = 160 - padTop - padBottom;
    const n = op.length;
    return op.map((o, i) => ({
      x: n === 1 ? W / 2 : padX + i * (innerW / (n - 1)),
      y: padTop + innerH - (o.cantidad / max) * innerH,
      etiqueta: o.etiqueta,
      cantidad: o.cantidad
    }));
  }

  polilinea(p: ResultadoPregunta): string {
    return this.puntosLinea(p).map(pt => `${pt.x},${pt.y}`).join(' ');
  }

  get baseLinea(): number { return 14 + (160 - 14 - 30); }

  // ── CU10 - Generar reporte ────────────────────────────
  abrirReporte(): void {
    this.errorReporte = '';
    this.mostrarReporte = true;
  }
  cerrarReporte(): void {
    this.mostrarReporte = false;
    this.generando = '';
  }

  descargarFormato(formato: 'excel' | 'pdf' | 'word'): void {
    if (!this.resultados || this.generando) return;
    this.generando = formato;
    this.errorReporte = '';
    const ext = formato === 'excel' ? 'xlsx' : formato === 'word' ? 'docx' : 'pdf';
    const id = this.resultados.idEncuesta;
    this.resultadoService.descargar(id, formato).subscribe({
      next: (blob) => {
        this.guardarBlob(blob, `reporte_encuesta_${id}.${ext}`);
        this.generando = '';
        this.mostrarReporte = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorReporte = 'No se pudo generar el reporte. Inténtalo de nuevo.';
        this.generando = '';
        this.cdr.detectChanges();
      }
    });
  }

  private guardarBlob(blob: Blob, nombre: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombre;
    a.click();
    URL.revokeObjectURL(url);
  }

  private recortar(s: string, max: number): string {
    return s.length > max ? s.slice(0, max - 1) + '…' : s;
  }

  // Genera una imagen (PNG/JPG) del reporte dibujando en un canvas
  descargarImagen(tipo: 'png' | 'jpg'): void {
    const r = this.resultados;
    if (!r) return;

    const W = 780, padX = 40, barH = 26, gap = 10;
    let h = 110;
    for (const p of r.preguntas) {
      h += 40;
      const filas = this.esTexto(p) ? Math.max(1, p.respuestasTexto.length) : p.opciones.length;
      h += filas * (barH + gap) + 16;
    }

    const canvas = document.createElement('canvas');
    canvas.width = W;
    canvas.height = h;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, W, h);

    let y = 48;
    ctx.fillStyle = '#2B579A';
    ctx.font = 'bold 22px Segoe UI, sans-serif';
    ctx.fillText(this.recortar(r.tituloEncuesta, 60), padX, y);
    y += 26;
    ctx.fillStyle = '#6b7280';
    ctx.font = '13px Segoe UI, sans-serif';
    const sub = `Total de respuestas: ${r.totalRespuestas}` +
      (r.opcionMasSeleccionada ? `   ·   Más seleccionada: ${r.opcionMasSeleccionada}` : '');
    ctx.fillText(sub, padX, y);
    y += 28;

    let n = 1;
    for (const p of r.preguntas) {
      y += 24;
      ctx.fillStyle = '#111827';
      ctx.font = 'bold 15px Segoe UI, sans-serif';
      ctx.textAlign = 'left';
      ctx.fillText(this.recortar(`${n++}. ${p.descripcionPregunta}`, 72), padX, y);

      if (this.esTexto(p)) {
        ctx.fillStyle = '#6b7280';
        ctx.font = '13px Segoe UI, sans-serif';
        if (p.respuestasTexto.length === 0) {
          y += barH; ctx.fillText('(sin respuestas)', padX, y);
        } else {
          for (const t of p.respuestasTexto) { y += barH; ctx.fillText('• ' + this.recortar(t, 95), padX, y); }
        }
        y += gap;
      } else {
        const max = this.maxConteo(p);
        const labelW = 190;
        const barMaxW = W - padX * 2 - labelW - 100;
        for (let i = 0; i < p.opciones.length; i++) {
          const o = p.opciones[i];
          y += barH + gap;
          ctx.fillStyle = '#374151';
          ctx.font = '12px Segoe UI, sans-serif';
          ctx.textAlign = 'right';
          ctx.fillText(this.recortar(o.etiqueta, 26), padX + labelW, y - 7);
          ctx.textAlign = 'left';
          ctx.fillStyle = '#f3f4f6';
          ctx.fillRect(padX + labelW + 10, y - barH + 5, barMaxW, 18);
          ctx.fillStyle = this.color(i);
          ctx.fillRect(padX + labelW + 10, y - barH + 5, Math.max(2, barMaxW * (o.cantidad / max)), 18);
          ctx.fillStyle = '#6b7280';
          ctx.fillText(`${o.cantidad} (${o.porcentaje}%)`, padX + labelW + 10 + barMaxW + 8, y - 7);
        }
        y += 8;
      }
    }

    const mime = tipo === 'jpg' ? 'image/jpeg' : 'image/png';
    canvas.toBlob((blob) => {
      if (blob) this.guardarBlob(blob, `reporte_encuesta_${r.idEncuesta}.${tipo}`);
      this.mostrarReporte = false;
      this.cdr.detectChanges();
    }, mime, 0.95);
  }
}
