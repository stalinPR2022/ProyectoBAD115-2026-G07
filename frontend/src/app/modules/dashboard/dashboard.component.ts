import { Component, OnInit, OnDestroy, ChangeDetectorRef, signal, computed } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import { MenuService, MenuItem } from '../../core/services/menu.service';
import { ConfirmService } from '../../core/services/confirm.service';

interface Crumb { label: string; url: string; home?: boolean; }

// Etiquetas legibles por segmento de ruta para el breadcrumb
const CRUMB_LABELS: Record<string, string> = {
  usuarios: 'Usuarios',
  bloqueados: 'Bloqueados',
  roles: 'Roles',
  privilegios: 'Privilegios',
  encuestas: 'Encuestas',
  preguntas: 'Preguntas',
  resultados: 'Resultados',
  'mis-encuestas': 'Mis Encuestas',
  responder: 'Responder Encuestas',
};

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

// Agrupación y orden lógico de las opciones del menú (por nombre de privilegio)
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

interface MenuGrupo {
  titulo: string;
  items: MenuItem[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
  menuGrupos: MenuGrupo[] = [];
  user: { nombre: string; email: string } | null = null;
  sidebarColapsado = false;

  // Breadcrumb (ruta de navegación) según la URL actual
  readonly breadcrumbs = signal<Crumb[]>([]);
  private routerSub?: Subscription;

  // Reloj del sistema para la barra superior (reactivo)
  private reloj = signal(new Date());
  private intervaloReloj?: ReturnType<typeof setInterval>;
  readonly fechaTexto = computed(() =>
    new Intl.DateTimeFormat('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(this.reloj())
  );

  constructor(
    private authService: AuthService,
    private menuService: MenuService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private confirm: ConfirmService
  ) {
    this.user = this.authService.getUser();
  }

  ngOnInit(): void {
    this.intervaloReloj = setInterval(() => this.reloj.set(new Date()), 60000);

    this.breadcrumbs.set(this.construirBreadcrumbs(this.router.url));
    this.routerSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(e => this.breadcrumbs.set(this.construirBreadcrumbs((e as NavigationEnd).urlAfterRedirects)));

    this.menuService.obtenerMenu().subscribe({
      next: (items) => { this.menuGrupos = this.agrupar(items); this.cdr.detectChanges(); },
      error: () => this.cerrarSesion()
    });
  }

  ngOnDestroy(): void {
    if (this.intervaloReloj) clearInterval(this.intervaloReloj);
    this.routerSub?.unsubscribe();
  }

  private construirBreadcrumbs(url: string): Crumb[] {
    const path = url.split('?')[0].split('#')[0];
    const segmentos = path.split('/').filter(s => s.length);
    const idx = segmentos.indexOf('dashboard');
    const resto = idx >= 0 ? segmentos.slice(idx + 1) : segmentos;

    const crumbs: Crumb[] = [{ label: 'Inicio', url: '/dashboard', home: true }];
    let acc = '/dashboard';
    for (const seg of resto) {
      acc += `/${seg}`;
      if (/^\d+$/.test(seg)) continue; // ids numéricos: mantienen la ruta pero no muestran etiqueta
      crumbs.push({ label: CRUMB_LABELS[seg] ?? this.capitalizar(seg), url: acc });
    }
    return crumbs;
  }

  private capitalizar(s: string): string {
    const t = s.replace(/-/g, ' ');
    return t.charAt(0).toUpperCase() + t.slice(1);
  }

  private agrupar(items: MenuItem[]): MenuGrupo[] {
    const mapa = new Map<string, MenuItem[]>();
    for (const item of items) {
      const grupo = ORDEN[item.nombre]?.grupo ?? 'General';
      if (!mapa.has(grupo)) mapa.set(grupo, []);
      mapa.get(grupo)!.push(item);
    }
    for (const arr of mapa.values()) {
      arr.sort((a, b) => (ORDEN[a.nombre]?.orden ?? 99) - (ORDEN[b.nombre]?.orden ?? 99));
    }
    return ORDEN_GRUPOS
      .filter(g => mapa.has(g))
      .map(g => ({ titulo: g, items: mapa.get(g)! }));
  }

  getIcono(nombre: string): string {
    return ICONOS[nombre] ?? ICONO_DEFAULT;
  }

  toggleSidebar(): void {
    this.sidebarColapsado = !this.sidebarColapsado;
  }

  async logout(): Promise<void> {
    const ok = await this.confirm.ask({
      title: 'Cerrar sesión',
      message: '¿Seguro que deseas cerrar tu sesión actual?',
      confirmText: 'Cerrar sesión',
      cancelText: 'Cancelar',
      variant: 'primary'
    });
    if (ok) this.cerrarSesion();
  }

  private cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
