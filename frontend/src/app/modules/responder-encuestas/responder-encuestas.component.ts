import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PublicoService, EncuestaDisponible } from '../../core/services/publico.service';
import { SearchBarComponent } from '../../shared/search-bar/search-bar.component';
import { coincide } from '../../core/utils/search.util';

@Component({
  selector: 'app-responder-encuestas',
  standalone: true,
  imports: [CommonModule, SearchBarComponent],
  templateUrl: './responder-encuestas.component.html',
  styleUrl: './responder-encuestas.component.css'
})
export class ResponderEncuestasComponent implements OnInit {
  cargando = true;
  error = '';
  encuestas: EncuestaDisponible[] = [];
  busqueda = '';

  constructor(
    private publicoService: PublicoService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.publicoService.disponibles().subscribe({
      next: (data) => { this.encuestas = data; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.error = 'No se pudieron cargar las encuestas disponibles.'; this.cargando = false; this.cdr.detectChanges(); }
    });
  }

  get filtradas(): EncuestaDisponible[] {
    return this.encuestas.filter(e =>
      coincide(this.busqueda, e.tituloEncuesta, e.objetivoEncuesta, e.grupoMeta)
    );
  }

  responder(e: EncuestaDisponible): void {
    this.router.navigate(['/responder', e.tokenPublico]);
  }

  etiquetaBoton(e: EncuestaDisponible): string {
    if (e.estadoRespuesta === 2) return 'Ver encuesta';
    if (e.estadoRespuesta === 1) return 'Continuar';
    return 'Responder';
  }
}
