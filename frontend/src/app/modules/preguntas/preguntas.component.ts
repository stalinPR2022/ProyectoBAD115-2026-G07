import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PreguntaService, Pregunta, PreguntaRequest } from '../../core/services/pregunta.service';
import { EncuestaService, Encuesta } from '../../core/services/encuesta.service';

type TipoPrincipal = 'ABIERTA' | 'CERRADA';
type TipoTexto = 'corta' | 'larga';
type TipoCerrada = 'DICOTOMICA' | 'ELECCION_UNICA' | 'ELECCION_MULTIPLE' | 'RANKING';

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
    this.opciones = p.opciones?.length ? p.opciones.map(o => o.textoOpcion) : ['', ''];
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
    if (tipo === 'DICOTOMICA') {
      this.opciones = ['Sí', 'No'];
    } else if (!this.editando) {
      this.opciones = ['', ''];
    }
  }

  esMultiple(): boolean { return this.tipoCerrada === 'ELECCION_MULTIPLE'; }
  esRanking(): boolean { return this.tipoCerrada === 'RANKING'; }

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

    const tipoCerradaBackend =
      this.tipoCerrada === 'DICOTOMICA' || this.tipoCerrada === 'ELECCION_UNICA' ? 'ELECCION_UNICA' :
      this.tipoCerrada === 'ELECCION_MULTIPLE' ? 'ELECCION_MULTIPLE' : 'RANKING';

    const data: PreguntaRequest = {
      descripcionPregunta: this.form.value.descripcionPregunta,
      obligatoriaPregunta: this.form.value.obligatoriaPregunta ?? false,
      tipoPregunta: this.tipoPrincipal,
      ...(this.tipoPrincipal === 'CERRADA' && {
        tipoPreguntaCerrada: tipoCerradaBackend,
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
      if (p.tipoPreguntaCerrada === 'ELECCION_MULTIPLE') return 'Elección Múltiple';
      if (p.tipoPreguntaCerrada === 'RANKING') return 'Ranking';
      return p.opciones?.length === 2 ? 'Dicotómica' : 'Politómica';
    }
    return p.tipoPregunta;
  }

  colorTipo(p: Pregunta): string {
    if (p.tipoPregunta === 'ABIERTA') return 'tipo-abierta';
    if (p.tipoPregunta === 'CERRADA') {
      if (p.tipoPreguntaCerrada === 'ELECCION_MULTIPLE') return 'tipo-multiple';
      if (p.tipoPreguntaCerrada === 'RANKING') return 'tipo-ranking';
      return p.opciones?.length === 2 ? 'tipo-dicotomica' : 'tipo-politomica';
    }
    return '';
  }
}
