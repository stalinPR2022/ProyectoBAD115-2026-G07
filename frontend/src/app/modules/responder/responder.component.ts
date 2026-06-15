import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PublicoService, EncuestaPublica, RespuestaConfirmacion } from '../../core/services/publico.service';
import { Pregunta, Opcion } from '../../core/services/pregunta.service';
import { AuthService } from '../../core/services/auth.service';

type Paso = 'cargando' | 'error' | 'auth' | 'preguntas' | 'resumen' | 'fin' | 'yaRespondido';

interface RespuestaItem {
  idPregunta: number;
  texto: string;            // pregunta abierta
  idOpcion: number | null;  // selección única / likert / nominal / dicotómica
  idOpciones: number[];     // selección múltiple
  valor: number | null;     // escala numérica
  ranking: number[];        // orden de idOpcionRespuesta (ranking)
  otrosTexto: string;       // texto libre de la opción "Otros" (mixta)
}

@Component({
  selector: 'app-responder',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './responder.component.html',
  styleUrl: './responder.component.css'
})
export class ResponderComponent implements OnInit {
  paso: Paso = 'cargando';
  token = '';
  encuesta: EncuestaPublica | null = null;
  errorCarga = '';

  errorForm = '';
  enviando = false;

  // CU12 - Cuestionario
  preguntas: Pregunta[] = [];
  indiceActual = 0;
  respuestas: Record<number, RespuestaItem> = {};
  errorPregunta = '';

  // CU13 - Envío
  confirmando = false;
  confirmacion: RespuestaConfirmacion | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private publicoService: PublicoService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') ?? '';
    this.cargar();
  }

  get autenticado(): boolean { return this.authService.isLoggedIn(); }
  get nombreUsuario(): string { return this.authService.getUser()?.nombre ?? ''; }
  get redirectParams() { return { redirect: `/responder/${this.token}` }; }

  cargar(): void {
    this.publicoService.cargarEncuesta(this.token).subscribe({
      next: (e) => {
        this.encuesta = e;
        if (!this.autenticado) {
          this.paso = 'auth';
          this.cdr.detectChanges();
        } else {
          this.verificarEstado();
        }
      },
      error: (err) => {
        this.errorCarga = err.error?.mensaje || 'No se pudo cargar la encuesta.';
        this.paso = 'error';
        this.cdr.detectChanges();
      }
    });
  }

  private verificarEstado(): void {
    this.publicoService.estado(this.token).subscribe({
      next: (res) => {
        if (res.yaRespondido) {
          this.paso = 'yaRespondido';
          this.cdr.detectChanges();
        } else {
          this.cargarPreguntas();
        }
      },
      error: (err) => {
        this.errorCarga = err.error?.mensaje || 'No se pudo verificar tu estado.';
        this.paso = 'error';
        this.cdr.detectChanges();
      }
    });
  }

  irAlInicio(): void {
    this.router.navigateByUrl('/dashboard');
  }

  private cargarPreguntas(): void {
    this.publicoService.cargarPreguntas(this.token).subscribe({
      next: (data) => {
        this.preguntas = data;
        this.preguntas.forEach(p => this.initRespuesta(p));
        this.indiceActual = 0;
        this.enviando = false;
        this.paso = 'preguntas';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.enviando = false;
        this.errorForm = err.error?.mensaje || 'No se pudieron cargar las preguntas.';
        this.cdr.detectChanges();
      }
    });
  }

  // ── Estado de respuestas ──────────────────────────────
  private initRespuesta(p: Pregunta): void {
    this.respuestas[p.idPregunta] = {
      idPregunta: p.idPregunta,
      texto: '',
      idOpcion: null,
      idOpciones: [],
      valor: null,
      ranking: this.esRanking(p) ? p.opciones.map(o => o.idOpcionRespuesta) : [],
      otrosTexto: ''
    };
  }

  resp(p: Pregunta): RespuestaItem {
    return this.respuestas[p.idPregunta];
  }

  // ── Tipos de pregunta ─────────────────────────────────
  esAbierta(p: Pregunta): boolean { return p.tipoPregunta === 'ABIERTA'; }
  esMultiple(p: Pregunta): boolean { return p.tipoPreguntaCerrada === 'ELECCION_MULTIPLE'; }
  esEscala(p: Pregunta): boolean { return p.tipoPreguntaCerrada === 'ESCALA'; }
  esRanking(p: Pregunta): boolean { return p.tipoPreguntaCerrada === 'RANKING'; }
  esLikert(p: Pregunta): boolean { return p.tipoPreguntaCerrada === 'LIKERT'; }
  esUnica(p: Pregunta): boolean {
    return p.tipoPregunta === 'CERRADA' && !this.esMultiple(p) && !this.esEscala(p) && !this.esRanking(p);
  }

  get preguntaActual(): Pregunta | null {
    return this.preguntas[this.indiceActual] ?? null;
  }
  get progreso(): number {
    return this.preguntas.length ? Math.round(((this.indiceActual + 1) / this.preguntas.length) * 100) : 0;
  }
  get esUltima(): boolean { return this.indiceActual === this.preguntas.length - 1; }

  // ── Setters de respuesta ──────────────────────────────
  setTexto(p: Pregunta, valor: string): void { this.resp(p).texto = valor; }
  setOtros(p: Pregunta, valor: string): void { this.resp(p).otrosTexto = valor; }
  setUnica(p: Pregunta, idOpcion: number): void { this.resp(p).idOpcion = idOpcion; }
  setEscala(p: Pregunta, valor: number): void { this.resp(p).valor = valor; }

  toggleMultiple(p: Pregunta, idOpcion: number): void {
    const r = this.resp(p);
    const i = r.idOpciones.indexOf(idOpcion);
    if (i >= 0) {
      r.idOpciones.splice(i, 1);
    } else {
      if (p.maxSelecciones != null && r.idOpciones.length >= p.maxSelecciones) {
        this.errorPregunta = `Solo puedes seleccionar hasta ${p.maxSelecciones} opción${p.maxSelecciones === 1 ? '' : 'es'}.`;
        return;
      }
      r.idOpciones.push(idOpcion);
    }
    this.errorPregunta = '';
  }

  estaMarcada(p: Pregunta, idOpcion: number): boolean {
    return this.resp(p).idOpciones.includes(idOpcion);
  }

  // Ranking: mover un ítem arriba/abajo
  subir(p: Pregunta, idx: number): void {
    if (idx <= 0) return;
    const r = this.resp(p).ranking;
    [r[idx - 1], r[idx]] = [r[idx], r[idx - 1]];
  }
  bajar(p: Pregunta, idx: number): void {
    const r = this.resp(p).ranking;
    if (idx >= r.length - 1) return;
    [r[idx + 1], r[idx]] = [r[idx], r[idx + 1]];
  }
  opcionPorId(p: Pregunta, id: number): Opcion | undefined {
    return p.opciones.find(o => o.idOpcionRespuesta === id);
  }

  // Escala numérica
  escalaMin(p: Pregunta): number { return parseInt(p.opciones[0]?.textoOpcion, 10) || 1; }
  escalaMax(p: Pregunta): number { return parseInt(p.opciones[1]?.textoOpcion, 10) || 5; }
  escalaValores(p: Pregunta): number[] {
    const min = this.escalaMin(p), max = this.escalaMax(p);
    return Array.from({ length: Math.max(0, max - min + 1) }, (_, i) => min + i);
  }

  // Opción "Otros" (mixta)
  opcionEsOtros(o: Opcion): boolean { return !!o.esMixta; }
  otrosSeleccionado(p: Pregunta): boolean {
    const otros = p.opciones.find(o => o.esMixta);
    if (!otros) return false;
    return this.esMultiple(p)
      ? this.resp(p).idOpciones.includes(otros.idOpcionRespuesta)
      : this.resp(p).idOpcion === otros.idOpcionRespuesta;
  }

  // ── Navegación ────────────────────────────────────────
  private validarActual(): boolean {
    const p = this.preguntaActual;
    if (!p) return true;
    const r = this.resp(p);
    this.errorPregunta = '';

    const vacia =
      (this.esAbierta(p) && !r.texto.trim()) ||
      (this.esUnica(p) && r.idOpcion == null) ||
      (this.esMultiple(p) && r.idOpciones.length === 0) ||
      (this.esEscala(p) && r.valor == null);

    if (p.obligatoriaPregunta && vacia) {
      this.errorPregunta = 'Esta pregunta es obligatoria.';
      return false;
    }

    // Validación de longitud (abierta)
    if (this.esAbierta(p) && r.texto.trim()) {
      const len = r.texto.trim().length;
      if (p.maxCaracteres != null && len > p.maxCaracteres) {
        this.errorPregunta = `Máximo ${p.maxCaracteres} caracteres.`;
        return false;
      }
      if (p.minCaracteres != null && len < p.minCaracteres) {
        this.errorPregunta = `Mínimo ${p.minCaracteres} caracteres.`;
        return false;
      }
    }

    // Máximo de selecciones (múltiple)
    if (this.esMultiple(p) && p.maxSelecciones != null && r.idOpciones.length > p.maxSelecciones) {
      this.errorPregunta = `Selecciona como máximo ${p.maxSelecciones} opciones.`;
      return false;
    }

    // "Otros" seleccionado requiere texto
    if (this.otrosSeleccionado(p) && !r.otrosTexto.trim()) {
      this.errorPregunta = 'Especifica tu respuesta en el campo "Otros".';
      return false;
    }

    return true;
  }

  siguiente(): void {
    if (!this.validarActual()) return;
    if (this.esUltima) {
      this.paso = 'resumen';
    } else {
      this.indiceActual++;
    }
    this.errorPregunta = '';
  }

  anterior(): void {
    this.errorPregunta = '';
    if (this.paso === 'resumen') { this.paso = 'preguntas'; return; }
    if (this.indiceActual > 0) this.indiceActual--;
  }

  irAPregunta(idx: number): void {
    this.paso = 'preguntas';
    this.indiceActual = idx;
    this.errorPregunta = '';
  }

  // ── Resumen ───────────────────────────────────────────
  resumenRespuesta(p: Pregunta): string {
    const r = this.resp(p);
    if (this.esAbierta(p)) return r.texto.trim() || '(sin responder)';
    if (this.esEscala(p)) return r.valor != null ? String(r.valor) : '(sin responder)';
    if (this.esRanking(p)) {
      return r.ranking.map((id, i) => `${i + 1}. ${this.opcionPorId(p, id)?.textoOpcion}`).join('  ·  ');
    }
    if (this.esMultiple(p)) {
      if (!r.idOpciones.length) return '(sin responder)';
      const textos = r.idOpciones.map(id => {
        const o = this.opcionPorId(p, id);
        return o?.esMixta ? `Otros: ${r.otrosTexto}` : o?.textoOpcion;
      });
      return textos.join(', ');
    }
    // única / likert / nominal
    if (r.idOpcion == null) return '(sin responder)';
    const o = this.opcionPorId(p, r.idOpcion);
    return o?.esMixta ? `Otros: ${r.otrosTexto}` : (o?.textoOpcion ?? '(sin responder)');
  }

  // ── CU13 - Envío final ────────────────────────────────
  enviar(): void {
    this.errorForm = '';
    this.confirmando = true;
  }

  cancelarEnvio(): void {
    this.confirmando = false;
  }

  confirmarEnvio(): void {
    if (this.enviando) return;
    this.confirmando = false;
    this.enviando = true;
    this.errorForm = '';

    const respuestas = this.preguntas.map(p => {
      const r = this.resp(p);
      return {
        idPregunta: p.idPregunta,
        texto: r.texto,
        idOpcion: r.idOpcion,
        idOpciones: r.idOpciones,
        valor: r.valor,
        ranking: r.ranking,
        otrosTexto: r.otrosTexto
      };
    });

    this.publicoService.enviar(this.token, respuestas).subscribe({
      next: (res) => {
        this.confirmacion = res;
        this.enviando = false;
        this.paso = 'fin';
        this.cdr.detectChanges();
      },
      error: (err) => {
        // Las respuestas se conservan en el cliente; el encuestado puede reintentar
        this.enviando = false;
        this.errorForm = err.error?.mensaje || 'No se pudieron enviar tus respuestas. Inténtalo de nuevo.';
        this.cdr.detectChanges();
      }
    });
  }
}
