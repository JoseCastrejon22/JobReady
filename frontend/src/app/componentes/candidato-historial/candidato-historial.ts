import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { EntrevistaRevision } from '../../modelos/api-modelos';
import { SimulacionesServicio } from '../../servicios/simulaciones-servicio';
import { CandidatoLayoutComponente } from '../candidato-layout/candidato-layout';

@Component({
  selector: 'app-candidato-historial',
  imports: [RouterLink, CandidatoLayoutComponente, DatePipe],
  templateUrl: './candidato-historial.html',
  styleUrls: ['../candidato-layout/candidato-content.css', './candidato-historial.css'],
})
export class CandidatoHistorialComponente {
  private readonly simulaciones = inject(SimulacionesServicio);
  private readonly destruccion = inject(DestroyRef);
  protected readonly entrevistas = signal<EntrevistaRevision[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal('');

  ngOnInit(): void {
    this.simulaciones.listarEntrevistasParaRevision().pipe(
      takeUntilDestroyed(this.destruccion),
      finalize(() => this.cargando.set(false)),
    ).subscribe({
      next: (entrevistas) => this.entrevistas.set(entrevistas),
      error: () => this.error.set('No se pudo cargar el historial de prácticas.'),
    });
  }
}
