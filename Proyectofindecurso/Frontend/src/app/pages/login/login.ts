import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { finalize, timeout } from 'rxjs/operators';
import { AuthService } from '../../services/auth';
import { getRoleHomeRoute, setSessionUser } from '../../services/session';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent {

  usuarioLogin = '';
  password = '';
  showPassword = false;
  isLoading = false;
  errorMessage = '';

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  login() {

    this.errorMessage = '';

    if (!this.usuarioLogin || !this.password) {
      this.errorMessage = 'Complete todos los campos';
      return;
    }

    this.isLoading = true;

    this.authService.login(this.usuarioLogin, this.password)
      .pipe(
        timeout(8000),
        finalize(() => this.isLoading = false)
      )
      .subscribe({
        next: (resp: any) => {
          // Determinar rol activo inicial (el rol principal)
          const rolActivo = resp?.rol ?? '';

          // Guardar sesión incluyendo todos los roles disponibles y el rol activo
          setSessionUser({
            ...(resp || {}),
            rol: rolActivo,
            rolActivo: rolActivo,
            roles: Array.isArray(resp?.roles) ? resp.roles : (rolActivo ? [rolActivo] : []),
          });

          const homeRoute = getRoleHomeRoute(rolActivo);
          if (!homeRoute) {
            this.errorMessage = 'Rol no reconocido: ' + (rolActivo ?? '');
            return;
          }

          this.router.navigate([homeRoute]);
        },
        error: (err) => {
          if (err?.name === 'TimeoutError') {
            this.errorMessage = 'El servidor tardó demasiado. Intente nuevamente.';
            return;
          }

          this.errorMessage = err?.error?.message || 'Usuario o contraseña incorrectos';
        }
      });
  }
}
