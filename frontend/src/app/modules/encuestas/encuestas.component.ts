import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { EncuestaService, Encuesta, EncuestaRequest } from '../../core/services/encuesta.service';
import { PreguntaService, Pregunta } from '../../core/services/pregunta.service';

@Component({
  selector: 'app-encuestas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './encuestas.component.html',
  styleUrl: './encuestas.component.css'
})
export class EncuestasComponent implements OnInit {
  encuestas: Encuesta[] = [];
  cargando = true;
  mostrarModal = false;
  editando: Encuesta | null = null;
  error = '';
  errorModal = '';
  form: FormGroup;

  // CU08 - Publicación
  mostrarPublicar = false;
  encuestaPublicar: Encuesta | null = null;
  preguntasPreview: Pregunta[] = [];
  errorPublicar = '';
  publicando = false;
  publicadaOk: Encuesta | null = null;
  copiado = false;

  constructor(
    private encuestaService: EncuestaService,
    private preguntaService: PreguntaService,
    private fb: FormBuilder,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      tituloEncuesta: ['', [Validators.required, Validators.maxLength(150)]],
      objetivoEncuesta: ['', Validators.maxLength(500)],
      instruccionesEncuesta: ['', Validators.maxLength(500)],
      grupoMeta: ['', Validators.maxLength(150)],
      fechaCierre: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.encuestaService.listar().subscribe({
      next: (data) => { this.encuestas = data; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.error = 'Error al cargar encuestas.'; this.cargando = false; this.cdr.detectChanges(); }
    });
  }

  abrirCrear(): void {
    this.editando = null;
    this.form.reset();
    this.errorModal = '';
    this.mostrarModal = true;
  }

  abrirEditar(e: Encuesta): void {
    this.editando = e;
    this.form.patchValue({
      tituloEncuesta: e.tituloEncuesta,
      objetivoEncuesta: e.objetivoEncuesta,
      instruccionesEncuesta: e.instruccionesEncuesta,
      grupoMeta: e.grupoMeta,
      fechaCierre: e.fechaCierre
    });
    this.errorModal = '';
    this.mostrarModal = true;
  }

  guardar(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const data: EncuestaRequest = this.form.value;

    const accion = this.editando
      ? this.encuestaService.actualizar(this.editando.idEncuesta, data)
      : this.encuestaService.crear(data);

    accion.subscribe({
      next: () => { this.mostrarModal = false; this.cargar(); },
      error: (e) => { this.errorModal = e.error?.mensaje || 'Error al guardar.'; this.cdr.detectChanges(); }
    });
  }

  eliminar(e: Encuesta): void {
    if (!confirm(`¿Eliminar la encuesta "${e.tituloEncuesta}"?`)) return;
    this.encuestaService.eliminar(e.idEncuesta).subscribe({
      next: () => this.cargar(),
      error: (err) => { alert(err.error?.mensaje || 'No se pudo eliminar.'); }
    });
  }

  irAPreguntas(e: Encuesta): void {
    this.router.navigate(['/dashboard/encuestas', e.idEncuesta, 'preguntas']);
  }

  irAResultados(e: Encuesta): void {
    this.router.navigate(['/dashboard/encuestas', e.idEncuesta, 'resultados']);
  }

  cerrarModal(): void {
    this.mostrarModal = false;
  }

  // ── CU08 - Publicar ──────────────────────────────────
  abrirPublicar(e: Encuesta): void {
    this.encuestaPublicar = e;
    this.preguntasPreview = [];
    this.errorPublicar = '';
    this.publicadaOk = null;
    this.copiado = false;
    this.mostrarPublicar = true;
    this.preguntaService.listar(e.idEncuesta).subscribe({
      next: (data) => { this.preguntasPreview = data; this.cdr.detectChanges(); },
      error: () => this.cdr.detectChanges()
    });
  }

  confirmarPublicar(): void {
    if (!this.encuestaPublicar || this.publicando) return;
    this.publicando = true;
    this.errorPublicar = '';
    this.encuestaService.publicar(this.encuestaPublicar.idEncuesta).subscribe({
      next: (res) => {
        this.publicando = false;
        this.publicadaOk = res;
        this.cargar();
        this.cdr.detectChanges();
      },
      error: (e) => {
        this.publicando = false;
        this.errorPublicar = e.error?.mensaje || 'No se pudo publicar la encuesta.';
        this.cdr.detectChanges();
      }
    });
  }

  cerrarPublicar(): void {
    this.mostrarPublicar = false;
    this.encuestaPublicar = null;
    this.publicadaOk = null;
  }

  linkPublico(token: string | null): string {
    return token ? `${window.location.origin}/responder/${token}` : '';
  }

  copiarEnlace(token: string | null): void {
    if (!token) return;
    navigator.clipboard?.writeText(this.linkPublico(token));
    this.copiado = true;
    this.cdr.detectChanges();
    setTimeout(() => { this.copiado = false; this.cdr.detectChanges(); }, 2000);
  }

  get f() { return this.form.controls; }

  estadoClass(estado: number): string {
    return { 1: 'badge-diseno', 2: 'badge-publicada', 3: 'badge-cerrada' }[estado] ?? '';
  }
}
