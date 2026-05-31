import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

export interface Usuario {
  idUser: number;
  nombreUser: string;
  emailUser: string;
  fechaNacimiento: string;
  estadoUser: number;
  intentosFallidos: number;
  roles: string[];
}

export interface CrearUsuario {
  nombreUser: string;
  emailUser: string;
  contraseniaUser: string;
  fechaNacimiento: string;
}

export interface ActualizarUsuario {
  nombreUser: string;
  fechaNacimiento: string;
}

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly API = 'http://localhost:8080/usuarios';

  constructor(private http: HttpClient, private auth: AuthService) {}

  private headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.auth.getToken()}` });
  }

  listar(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(this.API, { headers: this.headers() });
  }

  crear(dto: CrearUsuario): Observable<Usuario> {
    return this.http.post<Usuario>(this.API, dto, { headers: this.headers() });
  }

  actualizar(id: number, dto: ActualizarUsuario): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.API}/${id}`, dto, { headers: this.headers() });
  }

  activar(id: number): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.API}/${id}/activar`, {}, { headers: this.headers() });
  }

  darDeBaja(id: number): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.API}/${id}/dar-de-baja`, {}, { headers: this.headers() });
  }

  desbloquear(id: number): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.API}/${id}/desbloquear`, {}, { headers: this.headers() });
  }
}
