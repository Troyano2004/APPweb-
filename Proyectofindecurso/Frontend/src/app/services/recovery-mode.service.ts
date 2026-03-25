import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class RecoveryModeService {

  private readonly API_URL      = 'http://localhost:8080/api/recovery';
  private readonly HEALTH_URL   = 'http://localhost:8080/api/recovery/status';

  // Signal reactivo — true cuando la BD está caída
  readonly bdCaida = signal(false);

  private intervaloMonitoreo: any;
  private intervaloEspera: any;

  constructor(private readonly http: HttpClient) {
    this.iniciarMonitoreo();
  }

  private iniciarMonitoreo(): void {
    this.verificar();
    this.intervaloMonitoreo = setInterval(() => this.verificar(), 15_000);
  }

  private verificar(): void {
    // Llama al endpoint de status que devuelve { bdDisponible: true/false }
    this.http.get<{ bdDisponible: boolean }>(`${this.HEALTH_URL}`)
      .subscribe({
        next: (res) => {
          this.bdCaida.set(!res.bdDisponible);
        },
        error: () => {
          // Backend caído del todo → no mostrar recovery
          this.bdCaida.set(false);
        }
      });
  }

  abrirRecovery(): void {
    window.open('http://localhost:8080/recovery.html', '_blank');
    this.esperarRecovery();
  }

  private esperarRecovery(): void {
    this.intervaloEspera = setInterval(() => {
      this.http.get<{ bdDisponible: boolean }>(`${this.HEALTH_URL}`)
        .subscribe({
          next: (res) => {
            if (res.bdDisponible) {
              clearInterval(this.intervaloEspera);
              this.bdCaida.set(false);
              window.location.href = 'http://localhost:4200/login';
            }
          },
          error: () => {}
        });
    }, 5_000);
  }
}
