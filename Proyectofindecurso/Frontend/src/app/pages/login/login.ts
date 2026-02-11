import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { finalize, timeout } from 'rxjs/operators';
import { AuthService } from '../../services/auth';

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
    // ✅ limpia error antes de intentar
    this.errorMessage = '';

    if (!this.usuarioLogin || !this.password) {
      this.errorMessage = 'Complete todos los campos';
      return;
    }

    this.isLoading = true;

    this.authService.login(this.usuarioLogin, this.password)
      .pipe(
        timeout(8000), // ✅ si no responde en 8s, cae al error
        finalize(() => this.isLoading = false) // ✅ apaga loading SIEMPRE
      )
      .subscribe({
        next: (resp: any) => {
          localStorage.setItem('usuario', JSON.stringify(resp));

          const rol = String(resp?.rol || '').trim().toUpperCase();

          if (rol === 'ROLE_ADMIN') {
            this.router.navigate(['/admin/usuarios']);
            return;
          }
          if (rol === 'ROLE_DOCENTE') {
            this.router.navigate(['/docente']);
            return;
          }
          if (rol === 'ROLE_ESTUDIANTE') {
            this.router.navigate(['/estudiante']);
            return;
          }

          this.errorMessage = 'Rol no reconocido: ' + (resp?.rol ?? '');
        },
        error: (err) => {
          // ✅ Si fue timeout
          if (err?.name === 'TimeoutError') {
            this.errorMessage = 'El servidor tardó demasiado. Intente nuevamente.';
            return;
          }

          // ✅ Mensaje del backend si existe
          this.errorMessage = err?.error?.message || 'Usuario o contraseña incorrectos';
        }
      });
  }
}
