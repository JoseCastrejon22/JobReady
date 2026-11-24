import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ResultadoSimulacion } from '../../modelos/api-modelos';
import { SimulacionesServicio } from '../../servicios/simulaciones-servicio';
import { CandidatoLayoutComponente } from '../candidato-layout/candidato-layout';
import { RevisionVideoComponente } from '../revision-video/revision-video';

@Component({
  selector: 'app-candidato-resultados',
  imports: [RouterLink, CandidatoLayoutComponente, RevisionVideoComponente],
  templateUrl: './candidato-resultados.html',
  styleUrls: ['../candidato-layout/candidato-content.css', './candidato-resultados.css'],
})
export class CandidatoResultadosComponente {
  private readonly simulacionesServicio = inject(SimulacionesServicio);
  private readonly route = inject(ActivatedRoute);
  private readonly cambios = inject(ChangeDetectorRef);

  protected resultado: ResultadoSimulacion | null = null;
  protected cargando = false;
  protected error = '';

  ngOnInit(): void {
    const idSimulacion = this.route.snapshot.queryParamMap.get('idSimulacion');

    if (idSimulacion != null) {
      this.cargarResultado(Number(idSimulacion));
    }
  }

  cargarResultado(idSimulacion: number): void {
    this.cargando = true;
    this.error = '';

    this.simulacionesServicio.obtenerResultado(idSimulacion).subscribe({
      next: (resultado) => {
        this.resultado = resultado;
        this.cargando = false;
        this.cambios.markForCheck();
      },

      error: () => {
        this.error = 'No se pudo cargar el resultado de la simulación.';
        this.cargando = false;
        this.cambios.markForCheck();
      },
    });
  }
}
