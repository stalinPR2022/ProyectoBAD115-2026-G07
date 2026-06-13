import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Opcion {
  idOpcionRespuesta: number;
  textoOpcion: string;
  valorNumerico: number;
  esMixta?: boolean;
}

export interface Pregunta {
  idPregunta: number;
  descripcionPregunta: string;
  obligatoriaPregunta: boolean;
  tipoPregunta: string;
  tipoPreguntaCerrada: string | null;
  esMixta: boolean;
  minCaracteres: number | null;
  maxCaracteres: number | null;
  maxSelecciones: number | null;
  idEncuesta: number;
  opciones: Opcion[];
}

export interface PreguntaRequest {
  descripcionPregunta: string;
  obligatoriaPregunta: boolean;
  tipoPregunta: string;
  tipoPreguntaCerrada?: string;
  esMixta?: boolean;
  minCaracteres?: number | null;
  maxCaracteres?: number | null;
  maxSelecciones?: number | null;
  opciones?: string[];
}

@Injectable({ providedIn: 'root' })
export class PreguntaService {
  private api = 'http://localhost:8080/encuestas';

  constructor(private http: HttpClient) {}

  listar(idEncuesta: number): Observable<Pregunta[]> {
    return this.http.get<Pregunta[]>(`${this.api}/${idEncuesta}/preguntas`);
  }

  agregar(idEncuesta: number, data: PreguntaRequest): Observable<Pregunta> {
    return this.http.post<Pregunta>(`${this.api}/${idEncuesta}/preguntas`, data);
  }

  actualizar(idEncuesta: number, idPregunta: number, data: PreguntaRequest): Observable<Pregunta> {
    return this.http.put<Pregunta>(`${this.api}/${idEncuesta}/preguntas/${idPregunta}`, data);
  }

  eliminar(idEncuesta: number, idPregunta: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${idEncuesta}/preguntas/${idPregunta}`);
  }
}
