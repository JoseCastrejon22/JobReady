import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of, tap } from 'rxjs';
import { API_BASE_URL } from './api-configuracion';
import { AuthRequest, AuthResponse, RegistroCandidato, UsuarioActual } from '../modelos/api-modelos';

const TOKEN_KEY = 'jobready.jwt';
const ROLES_KEY = 'jobready.roles';
const USUARIO_KEY = 'jobready.usuario';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  /** El token llega en la cabecera de respuesta "Authorization: Bearer <jwt>"; el cuerpo solo trae los roles. */
  ingresar(datos: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE_URL}/authenticate`, datos, { observe: 'response' }).pipe(
      map((respuesta) => {
        const cabecera = respuesta.headers.get('Authorization') ?? '';
        const token = cabecera.startsWith('Bearer ') ? cabecera.substring(7).trim() : '';
        if (!token || !respuesta.body) throw new Error('La respuesta de inicio de sesión no trae el token en la cabecera Authorization.');
        sessionStorage.setItem(TOKEN_KEY, token);
        sessionStorage.setItem(ROLES_KEY, JSON.stringify(respuesta.body.roles));
        sessionStorage.removeItem(USUARIO_KEY);
        return respuesta.body;
      }),
    );
  }

  registrar(datos: RegistroCandidato): Observable<unknown> {
    return this.http.post(`${API_BASE_URL}/registro`, datos);
  }

  /** Envía el enlace de recuperación al correo; la respuesta es la misma exista o no la cuenta. */
  solicitarRecuperacion(email: string): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(`${API_BASE_URL}/recuperar-contrasena`, { email });
  }

  validarEnlaceRecuperacion(token: string): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/recuperar-contrasena/validar`, { token });
  }

  restablecerContrasena(token: string, password: string): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/recuperar-contrasena/restablecer`, { token, password });
  }

  cerrarSesion(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(ROLES_KEY);
    sessionStorage.removeItem(USUARIO_KEY);
  }

  /** Cuenta del candidato conectado; se guarda en la sesión para no consultarla en cada pantalla. */
  obtenerUsuarioActual(): Observable<UsuarioActual> {
    const guardado = sessionStorage.getItem(USUARIO_KEY);
    if (guardado) {
      try {
        return of(JSON.parse(guardado) as UsuarioActual);
      } catch {
        sessionStorage.removeItem(USUARIO_KEY);
      }
    }
    return this.http.get<UsuarioActual>(`${API_BASE_URL}/usuario/actual`).pipe(
      tap((usuario) => sessionStorage.setItem(USUARIO_KEY, JSON.stringify(usuario))),
    );
  }

  estaAutenticado(): boolean {
    return !!sessionStorage.getItem(TOKEN_KEY);
  }

  obtenerRoles(): string[] {
    try {
      return JSON.parse(sessionStorage.getItem(ROLES_KEY) ?? '[]') as string[];
    } catch {
      return [];
    }
  }
}
