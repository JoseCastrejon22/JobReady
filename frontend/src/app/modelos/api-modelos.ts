export interface AuthRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  jwt: string;
  roles: string[];
}

export interface RegistroCandidato {
  nombreCompleto: string;
  email: string;
  username: string;
  password: string;
  telefono: string;
}

export interface Carrera {
  idCarrera: number;
  nombre: string;
  descripcion?: string;
  activo?: boolean;
}

export interface PreguntaEntrevista {
  idPregunta: number;
  carrera?: Carrera;
  textoPregunta: string;
  categoria?: string;
  nivelDificultad?: string;
  activo: boolean;
  fechaCreacion?: string;
}

export interface PreguntaEntrevistaRequest {
  idCarrera: number;
  textoPregunta: string;
  categoria: string;
  nivelDificultad: string;
  activo: boolean;
}

export interface ResultadoSimulacion {
  idSimulacion: number;
  puntajeGeneral: number;
  nivelComunicacion: string;
  tiempoPromedioRespuesta: number;
  seguridadCalificacion: number;
  resumenFeedback: string;
  nombreEspecialista: string;
}

export interface EntrevistaRevision {
  idSimulacion: number;
  candidato: string;
  carrera: string;
  fechaInicio: string;
  estado: string;
}

export interface ObservacionVideo {
  idObservacion: number;
  segundo: number;
  comentario: string;
}

export interface VideoRevision {
  idArchivo: number;
  url: string;
  observaciones: ObservacionVideo[];
}
