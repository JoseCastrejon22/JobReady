import { ChangeDetectionStrategy, Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Carrera, PreguntaEntrevista, PreguntaRespondida } from '../../modelos/api-modelos';
import { CarrerasServicio } from '../../servicios/carreras-servicio';
import { PreguntasServicio } from '../../servicios/preguntas-servicio';
import { SimulacionesServicio } from '../../servicios/simulaciones-servicio';
import { CandidatoLayoutComponente } from '../candidato-layout/candidato-layout';

/** Preguntas por simulación: la grabación dura como máximo tres minutos. */
const MAX_PREGUNTAS = 5;
/** Tras mostrar una pregunta, la flecha se habilita pasado este tiempo para evitar avances accidentales. */
const ESPERA_NAVEGACION_MS = 1500;

@Component({
  selector: 'app-entrevista-componente',
  // Angular 22 usa OnPush por defecto; esta vista cambia al llegar datos de la API.
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CandidatoLayoutComponente, FormsModule, RouterLink],
  templateUrl: './entrevista-componente.html',
  styleUrl: './entrevista-componente.css',
})
export class EntrevistaComponente implements OnInit, OnDestroy {
  private readonly carrerasServicio = inject(CarrerasServicio);
  private readonly simulacionesServicio = inject(SimulacionesServicio);
  private readonly preguntasServicio = inject(PreguntasServicio);
  private readonly route = inject(ActivatedRoute);
  @ViewChild('vistaPrevia') private vistaPrevia?: ElementRef<HTMLVideoElement>;
  @ViewChild('confirmarFin') private dialogoConfirmar?: ElementRef<HTMLDialogElement>;
  private flujo?: MediaStream;
  private grabador?: MediaRecorder;
  private fragmentos: Blob[] = [];
  private reloj?: ReturnType<typeof setInterval>;
  private destruido = false;
  protected preparado = false;
  protected grabando = false;
  protected procesando = false;
  protected microfonoActivo = true;
  protected camaraActiva = true;
  protected segundosRestantes = 180;
  protected videoGrabado = '';
  private blobGrabado?: Blob;
  private inicioGrabacion = 0;
  private duracionGrabada = 0;
  protected carreras: Carrera[] = [];
  protected idCarrera: number | null = null;
  /** Carrera con la que se grabó el video actual; es la que se envía junto con sus preguntas. */
  private idCarreraGrabada: number | null = null;

  /** Preguntas activas de la carrera seleccionada (banco del que se eligen las de cada simulación). */
  protected bancoPreguntas: PreguntaEntrevista[] = [];
  protected cargandoPreguntas = false;
  protected errorPreguntas = '';
  private solicitudPreguntas = 0;
  /** Preguntas elegidas para la grabación en curso, en el orden en que se muestran. */
  protected preguntasSimulacion: PreguntaEntrevista[] = [];
  protected indicePregunta = 0;
  /** Milisegundos desde el inicio de la grabación en que se mostró cada pregunta. */
  private marcasMs: number[] = [];
  /** Preguntas mostradas con su tramo del video; se envían con la grabación. */
  protected preguntasRegistradas: PreguntaRespondida[] = [];
  protected navegacionBloqueada = false;
  private esperaNavegacion?: ReturnType<typeof setTimeout>;
  /** La grabación terminó y espera que el candidato confirme el fin de la simulación. */
  protected confirmandoFin = false;
  /** El candidato confirmó el fin de la simulación: ya puede revisar y enviar el video. */
  protected finalizada = false;
  protected enviando = false;
  /** Una grabación enviada ya no puede reenviarse; se desbloquea al grabar otra. */
  protected enviado = false;
  protected mensajeEnvio = '';
  protected errorEnvio = '';
  protected mensaje = 'Comprueba tu cámara y micrófono antes de comenzar.';

  protected get tiempoRestante(): string {
    return this.formatearTiempo(this.segundosRestantes);
  }

  protected get duracionTexto(): string {
    return this.formatearTiempo(this.duracionGrabada);
  }

  protected get preguntaActual(): PreguntaEntrevista | null {
    return this.preguntasSimulacion[this.indicePregunta] ?? null;
  }

  protected get esUltimaPregunta(): boolean {
    return this.indicePregunta >= this.preguntasSimulacion.length - 1;
  }

  /** Cuántas preguntas verá el candidato al iniciar la grabación. */
  protected get totalPreguntasPorMostrar(): number {
    return Math.min(MAX_PREGUNTAS, this.bancoPreguntas.length);
  }

  /** Texto de una pregunta registrada (para la revisión antes de enviar). */
  protected textoPregunta(idPregunta: number): string {
    return this.preguntasSimulacion.find((p) => p.idPregunta === idPregunta)?.textoPregunta ?? '';
  }

  protected formatearSegundos(total: number): string {
    return `${Math.floor(total / 60)}:${(total % 60).toString().padStart(2, '0')}`;
  }

  private formatearTiempo(totalSegundos: number): string {
    const minutos = Math.floor(totalSegundos / 60).toString().padStart(2, '0');
    const segundos = (totalSegundos % 60).toString().padStart(2, '0');
    return `${minutos}:${segundos}`;
  }

  ngOnInit(): void {
    const carrera = this.route.snapshot.queryParamMap.get('carrera');
    this.carrerasServicio.listar().subscribe({
      next: (carreras) => {
        this.carreras = carreras.filter((c) => c.activo !== false);
        const preseleccionada = Number(carrera);
        if (carrera && this.carreras.some((c) => c.idCarrera === preseleccionada)) {
          this.idCarrera = preseleccionada;
          this.cargarPreguntas(preseleccionada);
        }
      },
      error: () => this.errorEnvio = 'No se pudieron cargar las carreras. Comprueba la conexión con el servidor.',
    });
  }

  protected cambiarCarrera(idCarrera: number | null): void {
    this.idCarrera = idCarrera;
    this.cargarPreguntas(idCarrera);
  }

  /** Carga las preguntas activas de la carrera; solo cuenta la respuesta de la última solicitud. */
  private cargarPreguntas(idCarrera: number | null): void {
    const solicitud = ++this.solicitudPreguntas;
    this.bancoPreguntas = [];
    this.errorPreguntas = '';
    if (!idCarrera) {
      this.cargandoPreguntas = false;
      return;
    }
    this.cargandoPreguntas = true;
    this.preguntasServicio.listarPorCarrera(idCarrera).subscribe({
      next: (preguntas) => {
        if (solicitud !== this.solicitudPreguntas) return;
        this.bancoPreguntas = preguntas.filter((p) => p.activo !== false);
        this.cargandoPreguntas = false;
      },
      error: () => {
        if (solicitud !== this.solicitudPreguntas) return;
        this.cargandoPreguntas = false;
        this.errorPreguntas = 'No se pudieron cargar las preguntas de esta carrera. Vuelve a seleccionarla para reintentar.';
      },
    });
  }

  /** Elige al azar hasta MAX_PREGUNTAS preguntas distintas del banco (Fisher-Yates). */
  private elegirPreguntas(): PreguntaEntrevista[] {
    const copia = [...this.bancoPreguntas];
    for (let i = copia.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [copia[i], copia[j]] = [copia[j], copia[i]];
    }
    return copia.slice(0, MAX_PREGUNTAS);
  }

  protected async preparar(): Promise<void> {
    if (this.preparado || this.procesando) return;
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
      this.mensaje = 'Este navegador no permite comprobar la cámara y grabar video.';
      return;
    }
    this.procesando = true;
    try {
      this.flujo = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
      if (this.vistaPrevia) {
        this.vistaPrevia.nativeElement.srcObject = this.flujo;
        await this.vistaPrevia.nativeElement.play();
      }
      this.preparado = true;
      this.microfonoActivo = true;
      this.camaraActiva = true;
      this.mensaje = 'Cámara y micrófono listos. Puedes iniciar una grabación de hasta tres minutos.';
    } catch {
      this.detenerDispositivos();
      this.mensaje = 'No se pudo acceder a cámara y micrófono. Revisa los permisos y vuelve a intentarlo.';
    } finally {
      this.procesando = false;
    }
  }

  protected alternarMicrofono(): void {
    const pista = this.flujo?.getAudioTracks()[0];
    if (!pista) return;
    pista.enabled = !pista.enabled;
    this.microfonoActivo = pista.enabled;
  }

  protected alternarCamara(): void {
    const pista = this.flujo?.getVideoTracks()[0];
    if (!pista) return;
    pista.enabled = !pista.enabled;
    this.camaraActiva = pista.enabled;
  }

  protected iniciarGrabacion(): void {
    if (!this.flujo || this.grabando || !this.preparado) return;
    if (!this.idCarrera) {
      this.mensaje = 'Selecciona la carrera de la entrevista para recibir sus preguntas antes de grabar.';
      return;
    }
    if (this.cargandoPreguntas) {
      this.mensaje = 'Cargando las preguntas de la carrera… inténtalo en un momento.';
      return;
    }
    if (this.videoGrabado) {
      URL.revokeObjectURL(this.videoGrabado);
      this.videoGrabado = '';
    }
    this.blobGrabado = undefined;
    this.confirmandoFin = false;
    this.finalizada = false;
    this.enviado = false;
    this.mensajeEnvio = '';
    this.errorEnvio = '';
    this.fragmentos = [];
    this.segundosRestantes = 180;
    this.preguntasRegistradas = [];
    this.preguntasSimulacion = this.elegirPreguntas();
    this.indicePregunta = 0;
    this.marcasMs = [];
    this.idCarreraGrabada = this.idCarrera;
    const tipo = ['video/webm;codecs=vp9,opus', 'video/webm;codecs=vp8,opus', 'video/webm']
      .find((formato) => MediaRecorder.isTypeSupported(formato));
    try {
      this.grabador = new MediaRecorder(this.flujo, tipo ? { mimeType: tipo } : undefined);
      this.grabador.ondataavailable = (evento) => {
        if (evento.data.size > 0) this.fragmentos.push(evento.data);
      };
      this.grabador.onstop = () => {
        if (this.destruido) return;
        if (this.fragmentos.length) {
          // El tipo se guarda sin parámetros de códec (p. ej. "video/webm"), que es lo que valida el backend.
          const tipo = (this.grabador?.mimeType || 'video/webm').split(';')[0];
          this.blobGrabado = new Blob(this.fragmentos, { type: tipo });
          this.videoGrabado = URL.createObjectURL(this.blobGrabado);
          this.mensaje = 'Grabación detenida. Confirma si deseas finalizar la simulación.';
          this.abrirConfirmacion();
        } else {
          this.mensaje = 'No se pudo guardar la grabación. Vuelve a comprobar la cámara e inténtalo de nuevo.';
        }
      };
      this.grabador.start();
      this.inicioGrabacion = Date.now();
      this.grabando = true;
      if (this.preguntasSimulacion.length) {
        this.marcasMs = [0];
        this.bloquearNavegacion();
        this.mensaje = 'Grabando… responde la pregunta y pulsa la flecha para continuar.';
      } else {
        this.mensaje = 'Grabando respuesta…';
      }
      this.reloj = setInterval(() => {
        this.segundosRestantes -= 1;
        if (this.segundosRestantes <= 0) this.detenerGrabacion();
      }, 1000);
    } catch {
      this.preguntasSimulacion = [];
      this.mensaje = 'No se pudo iniciar la grabación en este navegador.';
    }
  }

  /**
   * Muestra la siguiente pregunta sin tocar el MediaRecorder: la grabación sigue siendo continua.
   * En la última pregunta, la flecha termina la sección de preguntas y detiene la grabación.
   */
  protected siguientePregunta(): void {
    if (!this.grabando || this.navegacionBloqueada || !this.preguntaActual) return;
    if (this.esUltimaPregunta) {
      this.detenerGrabacion();
      return;
    }
    this.marcasMs.push(Date.now() - this.inicioGrabacion);
    this.indicePregunta += 1;
    this.bloquearNavegacion();
  }

  private bloquearNavegacion(): void {
    this.limpiarEsperaNavegacion();
    this.navegacionBloqueada = true;
    this.esperaNavegacion = setTimeout(() => {
      this.navegacionBloqueada = false;
      this.esperaNavegacion = undefined;
    }, ESPERA_NAVEGACION_MS);
  }

  private limpiarEsperaNavegacion(): void {
    if (this.esperaNavegacion) clearTimeout(this.esperaNavegacion);
    this.esperaNavegacion = undefined;
  }

  /**
   * Convierte las marcas de las preguntas mostradas en tramos del video (segundos): cada pregunta
   * va desde que apareció hasta que apareció la siguiente; la última, hasta el final de la grabación.
   */
  private registrarTramos(): void {
    const duracion = this.duracionGrabada;
    const aSegundos = (ms: number) => Math.min(duracion, Math.max(0, Math.round(ms / 1000)));
    let finAnterior = 0;
    this.preguntasRegistradas = this.marcasMs.map((marca, i) => {
      const inicio = Math.max(finAnterior, aSegundos(marca));
      const siguiente = this.marcasMs[i + 1];
      const fin = Math.max(inicio, siguiente === undefined ? duracion : aSegundos(siguiente));
      finAnterior = fin;
      return { idPregunta: this.preguntasSimulacion[i].idPregunta, orden: i + 1, segundoInicio: inicio, segundoFin: fin };
    });
  }

  /**
   * Detiene la grabación y apaga cámara y micrófono en el acto. MediaRecorder entrega los
   * fragmentos ya capturados en su evento "stop", así que apagar las pistas aquí no pierde datos.
   */
  protected detenerGrabacion(): void {
    if (!this.grabando) return;
    this.grabando = false;
    this.limpiarReloj();
    this.limpiarEsperaNavegacion();
    this.navegacionBloqueada = false;
    // La duración y los tramos se calculan en el instante en que termina la grabación.
    this.duracionGrabada = Math.min(180, Math.max(1, Math.round((Date.now() - this.inicioGrabacion) / 1000)));
    this.registrarTramos();
    if (this.grabador && this.grabador.state !== 'inactive') this.grabador.stop();
    this.detenerDispositivos();
    this.mensaje = 'Procesando la grabación…';
  }

  private abrirConfirmacion(): void {
    this.confirmandoFin = true;
    const dialogo = this.dialogoConfirmar?.nativeElement;
    if (dialogo && !dialogo.open) dialogo.showModal();
  }

  private cerrarConfirmacion(): void {
    this.confirmandoFin = false;
    const dialogo = this.dialogoConfirmar?.nativeElement;
    if (dialogo?.open) dialogo.close();
  }

  /** Esc no cierra la confirmación: el candidato debe elegir finalizar o descartar. */
  protected impedirCierre(evento: Event): void {
    evento.preventDefault();
  }

  protected confirmarFinalizacion(): void {
    if (!this.blobGrabado) return;
    this.cerrarConfirmacion();
    this.finalizada = true;
    this.mensaje = 'Simulación finalizada. Revisa tu grabación y pulsa "Enviar video" para que un evaluador la revise.';
  }

  /** Descarta la grabación sin enviarla; el candidato puede volver a comprobar la cámara y grabar otra. */
  protected descartarGrabacion(): void {
    this.cerrarConfirmacion();
    if (this.videoGrabado) URL.revokeObjectURL(this.videoGrabado);
    this.videoGrabado = '';
    this.blobGrabado = undefined;
    this.duracionGrabada = 0;
    this.preguntasRegistradas = [];
    this.preguntasSimulacion = [];
    this.finalizada = false;
    this.segundosRestantes = 180;
    this.mensaje = 'Grabación descartada. Comprueba la cámara y el micrófono para grabar de nuevo.';
  }

  protected descargarGrabacion(): void {
    if (!this.videoGrabado) return;
    const enlace = document.createElement('a');
    enlace.href = this.videoGrabado;
    enlace.download = `jobready-practica-${new Date().toISOString().slice(0, 10)}.webm`;
    enlace.click();
  }

  protected get puedeEnviar(): boolean {
    return this.finalizada && !!this.blobGrabado && !!this.idCarreraGrabada && !this.enviando && !this.enviado && !this.grabando;
  }

  /** La carrera queda fija mientras haya una grabación sin enviar: sus preguntas pertenecen a esa carrera. */
  protected get carreraBloqueada(): boolean {
    return this.enviando || this.grabando || this.confirmandoFin || (!!this.videoGrabado && !this.enviado);
  }

  protected enviarGrabacion(): void {
    if (!this.finalizada || !this.blobGrabado || this.enviando || this.enviado) return;
    if (!this.idCarreraGrabada) {
      this.errorEnvio = 'Selecciona la carrera de la entrevista antes de enviarla.';
      return;
    }
    this.enviando = true;
    this.errorEnvio = '';
    this.mensajeEnvio = '';
    this.simulacionesServicio.enviarEntrevista(this.blobGrabado, this.idCarreraGrabada, this.duracionGrabada,
      this.preguntasRegistradas).subscribe({
      next: () => {
        this.enviando = false;
        this.enviado = true;
        this.mensajeEnvio = 'Entrevista enviada. Un evaluador la revisará y verás el estado en tu historial.';
      },
      error: (respuesta: HttpErrorResponse) => {
        this.enviando = false;
        if (respuesta.status === 409) {
          // El backend ya tiene este mismo video: se considera enviado.
          this.enviado = true;
          this.mensajeEnvio = 'Esta grabación ya había sido enviada. Puedes ver su estado en tu historial.';
          return;
        }
        this.errorEnvio = respuesta.status === 413
          ? 'El video es demasiado grande para enviarse.'
          : respuesta.error?.message || 'No se pudo enviar la entrevista. Inténtalo de nuevo.';
      },
    });
  }

  private limpiarReloj(): void {
    if (this.reloj) clearInterval(this.reloj);
    this.reloj = undefined;
  }

  private detenerDispositivos(): void {
    this.flujo?.getTracks().forEach((pista) => pista.stop());
    this.flujo = undefined;
    this.preparado = false;
    if (this.vistaPrevia) this.vistaPrevia.nativeElement.srcObject = null;
  }

  ngOnDestroy(): void {
    this.destruido = true;
    this.cerrarConfirmacion();
    if (this.grabador?.state === 'recording') this.grabador.stop();
    this.limpiarReloj();
    this.limpiarEsperaNavegacion();
    this.detenerDispositivos();
    if (this.videoGrabado) URL.revokeObjectURL(this.videoGrabado);
  }
}
