import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../servicios/auth-service';

@Component({
  selector: 'app-recuperacion-password-componente',
  // Angular 22 usa OnPush por defecto; esta vista cambia al responder la API.
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, RouterLink],
  templateUrl: './recuperacion-password-componente.html',
  styleUrl: '../registro-componente/registro-componente.css',
})
export class RecuperacionPasswordComponente {
  private readonly auth = inject(AuthService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly token = this.ruta.snapshot.fragment ?? '';

  protected readonly restablecer = this.ruta.snapshot.data['restablecer'] === true;
  protected enlaceInvalido = this.restablecer && !/^[A-Za-z0-9_-]{43}$/.test(this.token);
  protected email = '';
  protected password = '';
  protected confirmacion = '';
  protected cargando = false;
  protected error = '';
  protected mensaje = '';

  enviar(): void {
    if (this.cargando || this.enlaceInvalido) return;
    this.error = '';
    this.mensaje = '';
    if (this.restablecer && this.password !== this.confirmacion) {
      this.error = 'Las contraseñas deben coincidir.';
      return;
    }
    if (this.restablecer && (!this.password.trim() || new TextEncoder().encode(this.password).length > 72)) {
      this.error = 'Ingresa al menos 8 caracteres y hasta 72 bytes.';
      return;
    }

    const solicitud = this.restablecer
      ? this.auth.restablecerPassword(this.token, this.password)
      : this.auth.recuperarPassword(this.email.trim());
    this.cargando = true;
    solicitud.pipe(finalize(() => this.cargando = false)).subscribe({
      next: () => {
        this.password = '';
        this.confirmacion = '';
        this.mensaje = this.restablecer
          ? 'Tu contraseña se actualizó. Ya puedes iniciar sesión.'
          : 'Enviamos el enlace a tu correo. Es válido durante 15 minutos.';
      },
      error: (respuesta: HttpErrorResponse) => {
        if (!this.restablecer && respuesta.status === 404) {
          this.error = 'No existe ninguna cuenta asociada a este correo electrónico';
        } else if (!this.restablecer && respuesta.status === 400) {
          this.error = 'Ingresa un correo electrónico válido.';
        } else if (this.restablecer && respuesta.status === 400) {
          this.enlaceInvalido = true;
        } else if (this.restablecer && respuesta.status === 422) {
          this.error = 'Ingresa al menos 8 caracteres y hasta 72 bytes.';
        } else {
          this.error = this.restablecer
            ? 'No se pudo actualizar tu contraseña. Intenta nuevamente.'
            : 'No se pudo enviar el correo de recuperación. Intenta nuevamente.';
        }
      },
    });
  }
}
