import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Pregunta } from './pregunta.service';

export interface EncuestaPublica {
  idEncuesta: number;
  tituloEncuesta: string;
  objetivoEncuesta: string;
  instruccionesEncuesta: string;
  grupoMeta: string;
  fechaCierre: string;
  totalPreguntas: number;
}

export interface ParticipanteRequest {
  nombre: string;
  email: string;
  fechaNacimiento: string;
}

export interface ParticipanteResponse {
  idEncuesta: number;
  email: string;
  nombre: string;
}

export interface DetalleEnvio {
  idPregunta: number;
  texto: string;
  idOpcion: number | null;
  idOpciones: number[];
  valor: number | null;
  ranking: number[];
  otrosTexto: string;
}

export interface RespuestaEnvio {
  email: string;
  respuestas: DetalleEnvio[];
}

export interface RespuestaConfirmacion {
  numeroRegistro: number;
  fechaRespuesta: string;
}

@Injectable({ providedIn: 'root' })
export class PublicoService {
  private api = 'http://localhost:8080/publico';

  constructor(private http: HttpClient) {}

  cargarEncuesta(token: string): Observable<EncuestaPublica> {
    return this.http.get<EncuestaPublica>(`${this.api}/encuestas/${token}`);
  }

  cargarPreguntas(token: string): Observable<Pregunta[]> {
    return this.http.get<Pregunta[]>(`${this.api}/encuestas/${token}/preguntas`);
  }

  participar(token: string, data: ParticipanteRequest): Observable<ParticipanteResponse> {
    return this.http.post<ParticipanteResponse>(`${this.api}/encuestas/${token}/participar`, data);
  }

  enviarRespuestas(token: string, data: RespuestaEnvio): Observable<RespuestaConfirmacion> {
    return this.http.post<RespuestaConfirmacion>(`${this.api}/encuestas/${token}/responder`, data);
  }
}
