import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class RecoveryModeService {

  private readonly API = 'http://localhost:8080/api/recovery';

  readonly bdCaida = signal(false);

  private intervaloMonitoreo: any;
  private intervaloEspera: any;

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router
  ) {
    this.iniciarMonitoreo();
  }

  private iniciarMonitoreo(): void {
    this.verificar();
    this.intervaloMonitoreo = setInterval(
      () => this.verificar(),
      15_000
    );
  }

  private verificar(): void {
    this.http.get<{ bdDisponible: boolean }>(`${this.API}/status`)
      .subscribe({
        next:  (res) => this.bdCaida.set(!res.bdDisponible),
        error: ()    => this.bdCaida.set(false)
      });
  }

  abrirRecovery(): void {
    this.router.navigate(['/recovery']);
    this.esperarRecovery();
  }

  private esperarRecovery(): void {
    this.intervaloEspera = setInterval(() => {
      this.http.get<{ bdDisponible: boolean }>(`${this.API}/status`)
        .subscribe({
          next: (res) => {
            if (res.bdDisponible) {
              clearInterval(this.intervaloEspera);
              this.bdCaida.set(false);
              this.router.navigate(['/login']);
            }
          },
          error: () => {}
        });
    }, 5_000);
  }
}
