import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { SimulacionesServicio } from '../../servicios/simulaciones-servicio';
import { RespuestaEntrevista, SimulacionDetalle } from '../../modelos/api-modelos';
import { EvaluadorServicio } from '../../servicios/evaluador-servicio';
import { PanelRolLayout } from '../panel-rol-layout/panel-rol-layout';

const MAX_TEXTO = 4000;

/** Rechaza textos que solo contienen espacios. */
function noVacio(control: AbstractControl<string>): ValidationErrors | null {
  return (control.value ?? '').trim() ? null : { required: true };
}

const validadoresPorcentaje = [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)];
const validadoresTexto = [Validators.required, noVacio, Validators.maxLength(MAX_TEXTO)];

/**
 * US05 y US06: el evaluador revisa el video o las respuestas de una práctica y registra la rúbrica:
 * tres porcentajes y dos textos. El puntaje general es el promedio de los porcentajes y no se edita.
 */
@Component({
  selector: 'app-evaluador-evaluar',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    PanelRolLayout, RouterLink, ReactiveFormsModule, DatePipe,
    MatFormFieldModule, MatInputModule, MatButtonModule, MatProgressSpinnerModule,
  ],
  templateUrl: './evaluador-evaluar.html',
  styleUrls: ['../panel-rol-layout/panel-contenido.css', './evaluador-evaluar.css'],
})
export class EvaluadorEvaluar implements OnInit, OnDestroy {
  private readonly simulacionesServicio = inject(SimulacionesServicio);
  private readonly servicio = inject(EvaluadorServicio);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected idSimulacion = 0;
  protected simulacion: SimulacionDetalle | null = null;
  protected respuestas: RespuestaEntrevista[] = [];
  protected cargando = true;
  protected enviando = false;
  protected error = '';

  // Video enviado por el candidato.
  protected urlVideo = '';
  protected tieneVideo = false;
  protected errorVideo = '';

  // Rúbrica (Angular Material + formulario reactivo)
  protected readonly maxTexto = MAX_TEXTO;
  protected readonly formulario = this.fb.group({
    seguridad: this.fb.control<number | null>(null, validadoresPorcentaje),
    claridad: this.fb.control<number | null>(null, validadoresPorcentaje),
    dominio: this.fb.control<number | null>(null, validadoresPorcentaje),
    oportunidades: this.fb.nonNullable.control('', validadoresTexto),
    recomendaciones: this.fb.nonNullable.control('', validadoresTexto),
  });

  ngOnInit(): void {
    this.idSimulacion = Number(this.route.snapshot.paramMap.get('id'));
    forkJoin({
      respuestas: this.servicio.listarRespuestas(this.idSimulacion),
      simulaciones: this.servicio.listarSimulaciones(),
    }).subscribe({
      next: ({ respuestas, simulaciones }) => {
        this.respuestas = respuestas;
        this.simulacion = simulaciones.find((s) => s.idSimulacion === this.idSimulacion) ?? null;
        this.cargando = false;
        this.cargarVideo();
      },
      error: () => { this.error = 'No se pudo cargar la práctica.'; this.cargando = false; },
    });
  }

  private cargarVideo(): void {
    this.simulacionesServicio.listarArchivosDe(this.idSimulacion).subscribe({
      next: (archivos) => {
        const video = archivos.find((a) => a.tipoArchivo === 'video');
        if (!video) return;
        this.tieneVideo = true;
        this.simulacionesServicio.descargarArchivo(video.idArchivo).subscribe({
          next: (blob) => this.urlVideo = URL.createObjectURL(blob),
          error: () => this.errorVideo = 'No se pudo cargar el video del candidato.',
        });
      },
      error: () => this.errorVideo = 'No se pudo consultar el video de la práctica.',
    });
  }

  /** Promedio en segundos de las respuestas; si la práctica es solo un video, su duración (el backend calcula lo mismo). */
  protected get tiempoPromedio(): number {
    const tiempos = this.respuestas.map((r) => r.duracionSegundos ?? 0).filter((t) => t > 0);
    if (!tiempos.length) return this.tieneVideo ? (this.simulacion?.duracionSegundos ?? 0) : 0;
    return Math.round((tiempos.reduce((a, b) => a + b, 0) / tiempos.length) * 10) / 10;
  }

  ngOnDestroy(): void {
    if (this.urlVideo) URL.revokeObjectURL(this.urlVideo);
  }

  /** Puntaje general: promedio de los tres porcentajes redondeado (mismo cálculo que el backend). */
  protected get puntajeGeneral(): number | null {
    const { seguridad, claridad, dominio } = this.formulario.controls;
    if (seguridad.invalid || claridad.invalid || dominio.invalid) return null;
    return Math.round((Number(seguridad.value) + Number(claridad.value) + Number(dominio.value)) / 3);
  }

  protected largo(campo: 'oportunidades' | 'recomendaciones'): number {
    return this.formulario.controls[campo].value.trim().length;
  }

  protected enviar(): void {
    this.error = '';
    // US06 escenario 2: si falta un criterio, no se envía y se resaltan los campos.
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.error = 'Completa todos los campos de la rúbrica: porcentajes enteros entre 0 y 100 y los dos textos.';
      return;
    }
    if (this.enviando) return;
    this.enviando = true;
    const valores = this.formulario.getRawValue();
    this.servicio.registrarEvaluacion({
      idSimulacion: this.idSimulacion,
      seguridadRespuestas: Number(valores.seguridad),
      claridadRespuestas: Number(valores.claridad),
      dominioTema: Number(valores.dominio),
      oportunidadesMejora: valores.oportunidades.trim(),
      recomendaciones: valores.recomendaciones.trim(),
    }).subscribe({
      next: () => void this.router.navigate(['/evaluador/evaluaciones'], { queryParams: { registrada: this.idSimulacion } }),
      error: (respuesta: HttpErrorResponse) => {
        this.enviando = false;
        this.error = respuesta.status === 409
          ? 'Esta práctica ya fue evaluada por otro especialista.'
          : respuesta.status === 403
            ? 'Esta práctica está asignada a otro evaluador.'
            : respuesta.error?.message || 'No se pudo registrar la evaluación. Inténtalo otra vez.';
      },
    });
  }
}
