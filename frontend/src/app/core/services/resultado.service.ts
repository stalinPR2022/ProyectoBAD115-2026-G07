import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ConteoOpcion {
  etiqueta: string;
  cantidad: number;
  porcentaje: number;
}

export interface ResultadoPregunta {
  idPregunta: number;
  descripcionPregunta: string;
  tipoPregunta: string;
  tipoPreguntaCerrada: string | null;
  esMixta: boolean;
  totalRespuestas: number;
  graficoSugerido: 'pastel' | 'barras' | 'linea' | 'texto';
  opciones: ConteoOpcion[];
  respuestasTexto: string[];
}

export interface Resultados {
  idEncuesta: number;
  tituloEncuesta: string;
  estadoEncuesta: number;
  estadoNombre: string;
  totalRespuestas: number;
  opcionMasSeleccionada: string | null;
  preguntas: ResultadoPregunta[];
}

@Injectable({ providedIn: 'root' })
export class ResultadoService {
  private api = 'http://localhost:8080/encuestas';

  constructor(private http: HttpClient) {}

  obtener(idEncuesta: number): Observable<Resultados> {
    return this.http.get<Resultados>(`${this.api}/${idEncuesta}/resultados`);
  }

  descargar(idEncuesta: number, formato: 'excel' | 'pdf' | 'word'): Observable<Blob> {
    return this.http.get(`${this.api}/${idEncuesta}/reporte/${formato}`, { responseType: 'blob' });
  }
}
