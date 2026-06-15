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

export interface DetalleEnvio {
  idPregunta: number;
  texto: string;
  idOpcion: number | null;
  idOpciones: number[];
  valor: number | null;
  ranking: number[];
  otrosTexto: string;
}

export interface RespuestaConfirmacion {
  numeroRegistro: number;
  fechaRespuesta: string;
}

export interface EstadoRespuesta {
  yaRespondido: boolean;
}

@Injectable({ providedIn: 'root' })
export class PublicoService {
  private api = 'http://localhost:8080/publico';
  private responderApi = 'http://localhost:8080/responder';

  constructor(private http: HttpClient) {}

  // Públicos (bienvenida + preguntas)
  cargarEncuesta(token: string): Observable<EncuestaPublica> {
    return this.http.get<EncuestaPublica>(`${this.api}/encuestas/${token}`);
  }

  cargarPreguntas(token: string): Observable<Pregunta[]> {
    return this.http.get<Pregunta[]>(`${this.api}/encuestas/${token}/preguntas`);
  }

  // Autenticados (el JWT lo agrega el interceptor)
  estado(token: string): Observable<EstadoRespuesta> {
    return this.http.get<EstadoRespuesta>(`${this.responderApi}/${token}/estado`);
  }

  enviar(token: string, respuestas: DetalleEnvio[]): Observable<RespuestaConfirmacion> {
    return this.http.post<RespuestaConfirmacion>(`${this.responderApi}/${token}`, { respuestas });
  }
}
