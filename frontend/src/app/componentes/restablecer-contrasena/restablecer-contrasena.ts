import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../servicios/auth-service';

type Estado = 'validando' | 'invalido' | 'formulario' | 'listo';

/** Pantalla a la que lleva el enlace del correo: define la nueva contraseña. */
@Component({
  selector: 'app-restablecer-contrasena',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, RouterLink],
  templateUrl: './restablecer-contrasena.html',
  styleUrls: ['../ingresar-componente/ingresar-componente.css', '../recuperar-contrasena/recuperar-contrasena.css'],
})
export class RestablecerContrasena implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected estado: Estado = 'validando';
  protected password = '';
  protected confirmacion = '';
  protected mostrarPassword = false;
  protected cargando = false;
  protected error = '';
  private token = '';

  ngOnInit(): void {
    this.token = this.ruta.snapshot.queryParamMap.get('token') ?? '';
    // El token no debe quedar en la barra de direcciones ni en el historial del navegador.
    void this.router.navigate([], { queryParams: {}, replaceUrl: true });
    if (!this.token) {
      this.estado = 'invalido';
      return;
    }
    this.auth.validarEnlaceRecuperacion(this.token).subscribe({
      next: () => { this.estado = 'formulario'; },
      error: (e: HttpErrorResponse) => {
        this.estado = e.status === 400 ? 'invalido' : 'formulario';
        if (e.status !== 400) this.error = 'No se pudo comprobar el enlace; si falla al guardar, solicita uno nuevo.';
      },
    });
  }

  guardar(): void {
    if (this.cargando) return;
    this.error = '';
    if (this.password.length < 8 || this.password.length > 72) {
      this.error = 'La contraseña debe tener entre 8 y 72 caracteres.';
      return;
    }
    if (this.password !== this.confirmacion) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }
    this.cargando = true;
    this.auth.restablecerContrasena(this.token, this.password).subscribe({
      next: () => { this.estado = 'listo'; this.cargando = false; this.password = ''; this.confirmacion = ''; },
      error: (e: HttpErrorResponse) => {
        this.cargando = false;
        if (e.status === 400) this.estado = 'invalido';
        else if (e.status === 422) this.error = 'La contraseña debe tener entre 8 y 72 caracteres.';
        else this.error = 'No fue posible guardar la contraseña. Inténtalo de nuevo en un momento.';
      },
    });
  }
}
