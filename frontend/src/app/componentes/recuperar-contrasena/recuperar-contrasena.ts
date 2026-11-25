import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../servicios/auth-service';

const PATRON_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** El usuario escribe su correo y recibe allí el enlace para restablecer la contraseña. */
@Component({
  selector: 'app-recuperar-contrasena',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, RouterLink],
  templateUrl: './recuperar-contrasena.html',
  styleUrls: ['../ingresar-componente/ingresar-componente.css', './recuperar-contrasena.css'],
})
export class RecuperarContrasena {
  private readonly auth = inject(AuthService);
  protected email = '';
  protected cargando = false;
  protected enviado = false;
  protected error = '';

  enviar(): void {
    const email = this.email.trim();
    if (this.cargando) return;
    if (!PATRON_CORREO.test(email)) {
      this.error = 'Ingresa un correo electrónico válido.';
      return;
    }
    this.cargando = true;
    this.error = '';
    this.auth.solicitarRecuperacion(email).subscribe({
      next: () => { this.enviado = true; this.cargando = false; },
      error: (e: HttpErrorResponse) => {
        this.error = e.status === 400 ? 'Ingresa un correo electrónico válido.'
          : 'No fue posible enviar la solicitud. Comprueba que el servidor esté disponible e inténtalo de nuevo.';
        this.cargando = false;
      },
    });
  }

  volverAEnviar(): void {
    this.enviado = false;
  }
}
