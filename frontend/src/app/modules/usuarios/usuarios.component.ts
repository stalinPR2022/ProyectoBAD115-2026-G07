import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { UsuarioService, Usuario, CrearUsuario, ActualizarUsuario } from '../../core/services/usuario.service';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.css'
})
export class UsuariosComponent implements OnInit {
  usuarios: Usuario[] = [];
  cargando = false;
  error = '';
  exito = '';

  mostrarModal = false;
  modoEdicion = false;
  usuarioEditandoId: number | null = null;
  guardando = false;

  form: FormGroup;

  readonly ACTIVO = 1;
  readonly INACTIVO = 0;
  readonly BLOQUEADO = 2;

  constructor(private usuarioService: UsuarioService, private fb: FormBuilder, private cdr: ChangeDetectorRef) {
    this.form = this.fb.group({
      nombreUser: ['', Validators.required],
      emailUser: ['', [Validators.required, Validators.email]],
      contraseniaUser: ['', Validators.required],
      fechaNacimiento: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.cargarUsuarios();
  }

  cargarUsuarios(): void {
    this.cargando = true;
    this.usuarioService.listar().subscribe({
      next: (data) => { this.usuarios = data; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.error = 'Error al cargar usuarios.'; this.cargando = false; }
    });
  }

  abrirCrear(): void {
    this.modoEdicion = false;
    this.usuarioEditandoId = null;
    this.form.reset();
    this.form.get('contraseniaUser')?.setValidators(Validators.required);
    this.form.get('emailUser')?.enable();
    this.mostrarModal = true;
  }

  abrirEditar(u: Usuario): void {
    this.modoEdicion = true;
    this.usuarioEditandoId = u.idUser;
    this.form.patchValue({
      nombreUser: u.nombreUser,
      fechaNacimiento: u.fechaNacimiento,
      emailUser: u.emailUser,
      contraseniaUser: ''
    });
    this.form.get('contraseniaUser')?.clearValidators();
    this.form.get('emailUser')?.disable();
    this.mostrarModal = true;
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.error = '';
  }

  guardar(): void {
    if (this.form.invalid || this.guardando) return;
    this.guardando = true;
    this.error = '';

    if (this.modoEdicion && this.usuarioEditandoId) {
      const dto: ActualizarUsuario = {
        nombreUser: this.form.value.nombreUser,
        fechaNacimiento: this.form.value.fechaNacimiento
      };
      this.usuarioService.actualizar(this.usuarioEditandoId, dto).subscribe({
        next: () => { this.mostrarExito('Usuario actualizado.'); this.cerrarModal(); this.cargarUsuarios(); },
        error: (e) => { this.error = e.error?.mensaje || 'Error al actualizar.'; this.guardando = false; }
      });
    } else {
      const dto: CrearUsuario = this.form.value;
      this.usuarioService.crear(dto).subscribe({
        next: () => { this.mostrarExito('Usuario creado correctamente.'); this.cerrarModal(); this.cargarUsuarios(); },
        error: (e) => { this.error = e.error?.mensaje || 'Error al crear usuario.'; this.guardando = false; }
      });
    }
  }

  activar(id: number): void {
    this.usuarioService.activar(id).subscribe({
      next: () => { this.mostrarExito('Usuario activado.'); this.cargarUsuarios(); },
      error: () => this.mostrarError('Error al activar usuario.')
    });
  }

  darDeBaja(id: number): void {
    if (!confirm('¿Dar de baja a este usuario?')) return;
    this.usuarioService.darDeBaja(id).subscribe({
      next: () => { this.mostrarExito('Usuario dado de baja.'); this.cargarUsuarios(); },
      error: () => this.mostrarError('Error al dar de baja.')
    });
  }

  desbloquear(id: number): void {
    this.usuarioService.desbloquear(id).subscribe({
      next: () => { this.mostrarExito('Usuario desbloqueado. Se envió notificación por correo.'); this.cargarUsuarios(); },
      error: () => this.mostrarError('Error al desbloquear usuario.')
    });
  }

  estadoLabel(estado: number): string {
    if (estado === this.ACTIVO) return 'Activo';
    if (estado === this.INACTIVO) return 'Inactivo';
    if (estado === this.BLOQUEADO) return 'Bloqueado';
    return 'Desconocido';
  }

  private mostrarExito(msg: string): void {
    this.exito = msg;
    this.guardando = false;
    setTimeout(() => this.exito = '', 3500);
  }

  private mostrarError(msg: string): void {
    this.error = msg;
    setTimeout(() => this.error = '', 3500);
  }
}
