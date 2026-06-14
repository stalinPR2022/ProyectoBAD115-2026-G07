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
}
