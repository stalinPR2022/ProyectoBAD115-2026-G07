import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { MenuService, MenuItem } from '../../core/services/menu.service';

const ICONOS: Record<string, string> = {
  'Gestionar Usuarios':    'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 7a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm8 4v6m3-3h-6',
  'Asignar Roles':         'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z',
  'Gestionar Privilegios': 'M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0 3 3L22 7l-3-3m-3.5 3.5L19 4',
  'Desbloquear Usuarios':  'M8 11V7a4 4 0 0 1 8 0m-4 8v2m-6 4h12a2 2 0 0 0 2-2v-6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2z',
  'Gestionar Encuestas':   'M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2M9 5a2 2 0 0 1 4 0M9 12h6M9 16h4',
  'Mis Encuestas':         'M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2M9 5a2 2 0 0 1 4 0M9 14l2 2 4-4',
  'Responder Encuestas':   'M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z',
  'Ver Resultados':        'M18 20V10M12 20V4M6 20v-6',
};
const ICONO_DEFAULT = 'M4 6h16M4 12h16M4 18h16';

// Mismo orden que el sidebar (grupo Encuestas primero, Responder Encuestas al inicio)
const ORDEN: Record<string, { grupo: string; orden: number }> = {
  'Responder Encuestas':   { grupo: 'Encuestas', orden: 0 },
  'Mis Encuestas':         { grupo: 'Encuestas', orden: 1 },
  'Gestionar Encuestas':   { grupo: 'Encuestas', orden: 2 },
  'Ver Resultados':        { grupo: 'Encuestas', orden: 3 },
  'Gestionar Usuarios':    { grupo: 'Administración', orden: 1 },
  'Asignar Roles':         { grupo: 'Administración', orden: 2 },
  'Gestionar Privilegios': { grupo: 'Administración', orden: 3 },
  'Desbloquear Usuarios':  { grupo: 'Administración', orden: 4 },
};
const ORDEN_GRUPOS = ['Encuestas', 'Administración', 'General'];

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  user: { nombre: string; email: string } | null = null;
  accesos: MenuItem[] = [];

  constructor(
    private auth: AuthService,
    private menuService: MenuService,
    private cdr: ChangeDetectorRef
  ) {
    this.user = this.auth.getUser();
  }

  ngOnInit(): void {
    this.menuService.obtenerMenu().subscribe({
      next: (items) => { this.accesos = this.ordenar(items); this.cdr.detectChanges(); },
      error: () => {}
    });
  }

  private ordenar(items: MenuItem[]): MenuItem[] {
    return [...items].sort((a, b) => {
      const ca = ORDEN[a.nombre] ?? { grupo: 'General', orden: 99 };
      const cb = ORDEN[b.nombre] ?? { grupo: 'General', orden: 99 };
      const ga = ORDEN_GRUPOS.indexOf(ca.grupo);
      const gb = ORDEN_GRUPOS.indexOf(cb.grupo);
      return ga !== gb ? ga - gb : ca.orden - cb.orden;
    });
  }

  get saludo(): string {
    const h = new Date().getHours();
    if (h < 12) return 'Buenos días';
    if (h < 19) return 'Buenas tardes';
    return 'Buenas noches';
  }

  get fecha(): string {
    const f = new Intl.DateTimeFormat('es-ES', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    }).format(new Date());
    return f.charAt(0).toUpperCase() + f.slice(1);
  }

  getIcono(nombre: string): string {
    return ICONOS[nombre] ?? ICONO_DEFAULT;
  }
}
