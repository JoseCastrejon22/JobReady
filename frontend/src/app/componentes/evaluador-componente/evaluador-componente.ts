import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { EntrevistaRevision } from '../../modelos/api-modelos';
import { SimulacionesServicio } from '../../servicios/simulaciones-servicio';
import { PanelRolLayout } from '../panel-rol-layout/panel-rol-layout';
import { RevisionVideoComponente } from '../revision-video/revision-video';

@Component({
  selector: 'app-evaluador-componente',
  imports: [PanelRolLayout, RouterLink, DatePipe, RevisionVideoComponente],
  templateUrl: './evaluador-componente.html',
  styleUrl: './evaluador-componente.css',
})
export class EvaluadorComponente {
  private readonly simulaciones = inject(SimulacionesServicio);
  private readonly destruccion = inject(DestroyRef);
  protected readonly entrevistas = signal<EntrevistaRevision[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal('');
  protected seleccion: number | null = null;

  ngOnInit(): void {
    this.simulaciones.listarEntrevistasParaRevision().pipe(
      takeUntilDestroyed(this.destruccion),
      finalize(() => this.cargando.set(false)),
    ).subscribe({
      next: (entrevistas) => this.entrevistas.set(entrevistas),
      error: () => this.error.set('No se pudieron cargar las entrevistas asignadas.'),
    });
  }
}
