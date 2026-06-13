import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PreguntaService, Pregunta, PreguntaRequest } from '../../core/services/pregunta.service';
import { EncuestaService, Encuesta } from '../../core/services/encuesta.service';

type TipoPrincipal = 'ABIERTA' | 'CERRADA';
type TipoTexto = 'corta' | 'larga';
type TipoCerrada = 'DICOTOMICA' | 'ELECCION_UNICA' | 'ELECCION_MULTIPLE' | 'RANKING' | 'ESCALA' | 'LIKERT' | 'NOMINAL';

@Component({
  selector: 'app-preguntas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './preguntas.component.html',
  styleUrl: './preguntas.component.css'
})
export class PreguntasComponent implements OnInit {
  encuesta: Encuesta | null = null;
  preguntas: Pregunta[] = [];
  cargando = true;
  mostrarModal = false;
  editando: Pregunta | null = null;
  error = '';
  errorModal = '';
  form: FormGroup;
  idEncuesta!: number;

  tipoPrincipal: TipoPrincipal = 'ABIERTA';
  tipoTexto: TipoTexto = 'corta';
  tipoCerrada: TipoCerrada = 'DICOTOMICA';
  opciones: string[] = ['', ''];
  esMixtaPregunta = false;

  // CU07 - Criterios de validación
  minCaracteres: number | null = null;
  maxCaracteres: number | null = null;
  maxSelecciones: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private preguntaService: PreguntaService,
    private encuestaService: EncuestaService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      descripcionPregunta: ['', [Validators.required, Validators.maxLength(500)]],
      obligatoriaPregunta: [false]
    });
  }

  ngOnInit(): void {
    this.idEncuesta = Number(this.route.snapshot.paramMap.get('idEncuesta'));
    this.cargarEncuesta();
    this.cargarPreguntas();
  }

  cargarEncuesta(): void {
    this.encuestaService.buscar(this.idEncuesta).subscribe({
      next: (e) => { this.encuesta = e; this.cdr.detectChanges(); },
      error: () => this.router.navigate(['/dashboard/encuestas'])
    });
  }

  cargarPreguntas(): void {
    this.cargando = true;
    this.preguntaService.listar(this.idEncuesta).subscribe({
      next: (data) => { this.preguntas = data; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.error = 'Error al cargar preguntas.'; this.cargando = false; this.cdr.detectChanges(); }
    });
  }

  abrirAgregar(): void {
    this.editando = null;
    this.form.reset({ obligatoriaPregunta: false });
    this.tipoPrincipal = 'ABIERTA';
    this.tipoTexto = 'corta';
    this.tipoCerrada = 'DICOTOMICA';
    this.opciones = ['Sí', 'No'];
    this.esMixtaPregunta = false;
    this.minCaracteres = null;
    this.maxCaracteres = null;
    this.maxSelecciones = null;
    this.errorModal = '';
    this.mostrarModal = true;
  }

  abrirEditar(p: Pregunta): void {
    this.editando = p;
    this.form.patchValue({
      descripcionPregunta: p.descripcionPregunta,
      obligatoriaPregunta: p.obligatoriaPregunta
    });
    this.tipoPrincipal = p.tipoPregunta as TipoPrincipal;
    this.tipoCerrada = (p.tipoPreguntaCerrada as TipoCerrada) ?? 'DICOTOMICA';
    this.esMixtaPregunta = !!p.esMixta;
    this.minCaracteres = p.minCaracteres ?? null;
    this.maxCaracteres = p.maxCaracteres ?? null;
    this.maxSelecciones = p.maxSelecciones ?? null;
    // La opción "Otros" (esMixta) es implícita: no se muestra en el editor
    const editables = p.opciones?.filter(o => !o.esMixta) ?? [];
    this.opciones = editables.length ? editables.map(o => o.textoOpcion) : ['', ''];
    this.errorModal = '';
    this.mostrarModal = true;
  }

  seleccionarTipoPrincipal(tipo: TipoPrincipal): void {
    this.tipoPrincipal = tipo;
    if (tipo === 'CERRADA') {
      this.seleccionarTipoCerrada(this.tipoCerrada);
    }
  }

  seleccionarTipoCerrada(tipo: TipoCerrada): void {
    this.tipoCerrada = tipo;
    if (!this.editando) {
      if (tipo === 'DICOTOMICA') this.opciones = ['Sí', 'No'];
      else if (tipo === 'ESCALA') this.opciones = ['1', '10'];
      else if (tipo === 'LIKERT') this.opciones = ['Totalmente en desacuerdo', 'En desacuerdo', 'Neutral', 'De acuerdo', 'Totalmente de acuerdo'];
      else if (tipo === 'NOMINAL') this.opciones = ['Nunca', 'A veces', 'Siempre'];
      else this.opciones = ['', ''];
    }
    // "Otros" solo aplica a elección única/múltiple
    if (!this.puedeSerMixta()) this.esMixtaPregunta = false;
  }

  // Solo elección única o múltiple admiten campo "Otros" (pregunta mixta)
  puedeSerMixta(): boolean {
    return this.tipoCerrada === 'ELECCION_UNICA' || this.tipoCerrada === 'ELECCION_MULTIPLE';
  }

  esMultiple(): boolean { return this.tipoCerrada === 'ELECCION_MULTIPLE'; }
  esRanking(): boolean { return this.tipoCerrada === 'RANKING'; }
  esEscala(): boolean { return ['ESCALA', 'LIKERT', 'NOMINAL'].includes(this.tipoCerrada); }
  esEscalaNum(): boolean { return this.tipoCerrada === 'ESCALA'; }
  esLikert(): boolean { return this.tipoCerrada === 'LIKERT'; }
  esNominal(): boolean { return this.tipoCerrada === 'NOMINAL'; }

  get minEscala(): number { return this.opciones[0] ? parseInt(this.opciones[0]) || 1 : 1; }
  get maxEscala(): number { return this.opciones[1] ? parseInt(this.opciones[1]) || 5 : 5; }
  get escalaRange(): number[] {
    const min = this.minEscala;
    const max = this.maxEscala;
    return Array.from({ length: max - min + 1 }, (_, i) => min + i);
  }

  agregarOpcion(): void {
    this.opciones.push('');
  }

  eliminarOpcion(index: number): void {
    if (this.opciones.length > 2) this.opciones.splice(index, 1);
  }

  trackByIndex(index: number): number { return index; }

  guardar(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    if (this.tipoPrincipal === 'CERRADA') {
      const validas = this.opciones.filter(o => o.trim() !== '');
      if (validas.length < 2) {
        this.errorModal = 'Debe ingresar al menos 2 opciones.';
        return;
      }
    }

    // CU07 - Validación de criterios
    if (this.tipoPrincipal === 'ABIERTA') {
      if (this.minCaracteres != null && this.minCaracteres < 0) {
        this.errorModal = 'El mínimo de caracteres no puede ser negativo.';
        return;
      }
      if (this.maxCaracteres != null && this.maxCaracteres < 1) {
        this.errorModal = 'El máximo de caracteres debe ser mayor a 0.';
        return;
      }
      if (this.minCaracteres != null && this.maxCaracteres != null && this.minCaracteres > this.maxCaracteres) {
        this.errorModal = 'El valor mínimo no puede ser mayor al máximo.';
        return;
      }
    }
    if (this.esMultiple() && this.maxSelecciones != null) {
      const numOpciones = this.opciones.filter(o => o.trim() !== '').length + (this.esMixtaPregunta ? 1 : 0);
      if (this.maxSelecciones < 1) {
        this.errorModal = 'El máximo de selecciones debe ser al menos 1.';
        return;
      }
      if (this.maxSelecciones > numOpciones) {
        this.errorModal = 'El máximo de selecciones no puede superar el número de opciones.';
        return;
      }
    }

    const tipoCerradaBackend =
      (this.tipoCerrada === 'DICOTOMICA' || this.tipoCerrada === 'ELECCION_UNICA') ? 'ELECCION_UNICA' :
      this.tipoCerrada === 'ELECCION_MULTIPLE' ? 'ELECCION_MULTIPLE' :
      this.tipoCerrada === 'RANKING' ? 'RANKING' :
      this.tipoCerrada === 'LIKERT' ? 'LIKERT' :
      this.tipoCerrada === 'NOMINAL' ? 'NOMINAL' : 'ESCALA';

    const data: PreguntaRequest = {
      descripcionPregunta: this.form.value.descripcionPregunta,
      obligatoriaPregunta: this.form.value.obligatoriaPregunta ?? false,
      tipoPregunta: this.tipoPrincipal,
      ...(this.tipoPrincipal === 'ABIERTA' && {
        minCaracteres: this.minCaracteres,
        maxCaracteres: this.maxCaracteres
      }),
      ...(this.tipoPrincipal === 'CERRADA' && {
        tipoPreguntaCerrada: tipoCerradaBackend,
        esMixta: this.puedeSerMixta() && this.esMixtaPregunta,
        maxSelecciones: this.esMultiple() ? this.maxSelecciones : null,
        opciones: this.opciones.filter(o => o.trim() !== '')
      })
    };

    const accion = this.editando
      ? this.preguntaService.actualizar(this.idEncuesta, this.editando.idPregunta, data)
      : this.preguntaService.agregar(this.idEncuesta, data);

    accion.subscribe({
      next: () => { this.mostrarModal = false; this.cargarPreguntas(); },
      error: (e) => { this.errorModal = e.error?.mensaje || 'Error al guardar.'; this.cdr.detectChanges(); }
    });
  }

  eliminar(p: Pregunta): void {
    if (!confirm(`¿Eliminar "${p.descripcionPregunta}"?`)) return;
    this.preguntaService.eliminar(this.idEncuesta, p.idPregunta).subscribe({
      next: () => this.cargarPreguntas(),
      error: (e) => alert(e.error?.mensaje || 'No se pudo eliminar.')
    });
  }

  volver(): void { this.router.navigate(['/dashboard/encuestas']); }
  cerrarModal(): void { this.mostrarModal = false; }
  get f() { return this.form.controls; }
  get enDiseno(): boolean { return this.encuesta?.estadoEncuesta === 1; }

  esCerradaDicotomica(p: Pregunta): boolean {
    return p.tipoPregunta === 'CERRADA' && p.opciones?.length === 2;
  }

  labelTipo(p: Pregunta): string {
    if (p.tipoPregunta === 'ABIERTA') return 'Abierta';
    if (p.tipoPregunta === 'CERRADA') {
      const map: Record<string, string> = {
        ELECCION_MULTIPLE: 'Elección Múltiple', RANKING: 'Ranking',
        ESCALA: 'Escala Numérica', LIKERT: 'Escala Likert', NOMINAL: 'Escala Nominal'
      };
      if (p.tipoPreguntaCerrada && map[p.tipoPreguntaCerrada]) return map[p.tipoPreguntaCerrada];
      return p.opciones?.length === 2 ? 'Dicotómica' : 'Politómica';
    }
    return p.tipoPregunta;
  }

  colorTipo(p: Pregunta): string {
    if (p.tipoPregunta === 'ABIERTA') return 'tipo-abierta';
    if (p.tipoPregunta === 'CERRADA') {
      const map: Record<string, string> = {
        ELECCION_MULTIPLE: 'tipo-multiple', RANKING: 'tipo-ranking',
        ESCALA: 'tipo-escala', LIKERT: 'tipo-escala', NOMINAL: 'tipo-escala'
      };
      if (p.tipoPreguntaCerrada && map[p.tipoPreguntaCerrada]) return map[p.tipoPreguntaCerrada];
      return p.opciones?.length === 2 ? 'tipo-dicotomica' : 'tipo-politomica';
    }
    return '';
  }

  labelEscala(p: Pregunta): string {
    if (p.tipoPreguntaCerrada === 'ESCALA' && p.opciones?.length >= 2)
      return `Rango: ${p.opciones[0].textoOpcion} – ${p.opciones[1].textoOpcion}`;
    return '';
  }

  // CU07 - Texto descriptivo de la validación configurada
  labelValidacion(p: Pregunta): string {
    if (p.tipoPregunta === 'ABIERTA') {
      if (p.minCaracteres != null && p.maxCaracteres != null) return `${p.minCaracteres}–${p.maxCaracteres} caracteres`;
      if (p.maxCaracteres != null) return `Máx. ${p.maxCaracteres} caracteres`;
      if (p.minCaracteres != null) return `Mín. ${p.minCaracteres} caracteres`;
    }
    if (p.tipoPreguntaCerrada === 'ELECCION_MULTIPLE' && p.maxSelecciones != null) {
      return `Máx. ${p.maxSelecciones} selecciones`;
    }
    return '';
  }
}
