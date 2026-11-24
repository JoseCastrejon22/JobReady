import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { VideoRevision } from '../../modelos/api-modelos';
import { SimulacionesServicio } from '../../servicios/simulaciones-servicio';

@Component({
  selector: 'app-revision-video',
  imports: [FormsModule],
  templateUrl: './revision-video.html',
  styleUrl: './revision-video.css',
})
export class RevisionVideoComponente implements OnInit {
  private readonly simulaciones = inject(SimulacionesServicio);
  private readonly destruccion = inject(DestroyRef);
  readonly idSimulacion = input.required<number>();
  readonly puedeComentar = input(false);
  protected readonly videos = signal<VideoRevision[]>([]);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal<number | null>(null);
  protected readonly error = signal('');
  protected readonly aviso = signal('');

  ngOnInit(): void {
    this.simulaciones.obtenerRevisionVideo(this.idSimulacion()).pipe(
      takeUntilDestroyed(this.destruccion),
      finalize(() => this.cargando.set(false)),
    ).subscribe({
      next: ({ videos }) => this.videos.set(videos),
      error: () => this.error.set('No se pudo cargar la revisión de video.'),
    });
  }

  protected guardar(video: VideoRevision, reproductor: HTMLVideoElement, campo: HTMLTextAreaElement): void {
    const comentario = campo.value.trim();
    if (this.guardando() !== null || !comentario) return;
    if (reproductor.readyState < 1 || reproductor.seeking) {
      this.error.set('Espera a que el video cargue en el momento elegido.');
      return;
    }

    reproductor.pause();
    const segundo = Math.round(reproductor.currentTime * 1000) / 1000;
    this.error.set('');
    this.aviso.set('');
    this.guardando.set(video.idArchivo);
    this.simulaciones.guardarObservacion(this.idSimulacion(), video.idArchivo, segundo, comentario).pipe(
      takeUntilDestroyed(this.destruccion),
      finalize(() => this.guardando.set(null)),
    ).subscribe({
      next: (nota) => {
        this.videos.update((videos) => videos.map((item) => item.idArchivo === video.idArchivo
          ? { ...item, observaciones: [...item.observaciones, nota].sort((a, b) => a.segundo - b.segundo || a.idObservacion - b.idObservacion) }
          : item));
        campo.value = '';
        this.aviso.set(`Observación guardada en ${this.formatearTiempo(nota.segundo)}.`);
      },
      error: () => this.error.set('No se pudo guardar la observación. Tu comentario sigue en el formulario.'),
    });
  }

  protected reproducirDesde(reproductor: HTMLVideoElement, segundo: number): void {
    reproductor.currentTime = segundo;
    void reproductor.play().catch(() => this.error.set('No se pudo reproducir el video.'));
  }

  protected formatearTiempo(segundo: number): string {
    const total = Math.round(segundo * 1000);
    const minutos = Math.floor(total / 60000);
    const segundos = Math.floor(total / 1000) % 60;
    const milisegundos = total % 1000;
    const marca = `${String(minutos).padStart(2, '0')}:${String(segundos).padStart(2, '0')}`;
    return milisegundos ? `${marca}.${String(milisegundos).padStart(3, '0')}` : marca;
  }
}
