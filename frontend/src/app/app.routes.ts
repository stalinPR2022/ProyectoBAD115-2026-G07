import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./modules/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'responder/:token',
    loadComponent: () => import('./modules/responder/responder.component').then(m => m.ResponderComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./modules/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./modules/dashboard/home.component').then(m => m.HomeComponent) },
      { path: 'usuarios', loadComponent: () => import('./modules/usuarios/usuarios.component').then(m => m.UsuariosComponent) },
      { path: 'roles', loadComponent: () => import('./modules/roles/roles.component').then(m => m.RolesComponent) },
      { path: 'privilegios', loadComponent: () => import('./modules/privilegios/privilegios.component').then(m => m.PrivilegiosComponent) },
      { path: 'encuestas', loadComponent: () => import('./modules/encuestas/encuestas.component').then(m => m.EncuestasComponent) },
      { path: 'encuestas/:idEncuesta/preguntas', loadComponent: () => import('./modules/preguntas/preguntas.component').then(m => m.PreguntasComponent) },
      { path: 'encuestas/:idEncuesta/resultados', loadComponent: () => import('./modules/resultados/resultados.component').then(m => m.ResultadosComponent) },
    ]
  },
  { path: '**', redirectTo: 'login' }
];
