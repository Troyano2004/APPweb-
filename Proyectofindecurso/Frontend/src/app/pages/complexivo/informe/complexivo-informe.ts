
import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ComplexivoService,
  ComplexivoInformeDto
} from '../../../services/complexivo.service';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

type TabKey = 'portada' | 'marco' | 'resultados' | 'conclusiones';

@Component({
  selector: 'app-complexivo-informe',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './complexivo-informe.html',
  styleUrl: './complexivo-informe.scss'
})
export class ComplexivoInformeComponent implements OnInit {

  private readonly idEstudiante =
    getSessionEntityId(getSessionUser(), 'estudiante');

  loading   = signal(false);
  saving    = signal(false);
  error     = signal<string | null>(null);
  ok        = signal<string | null>(null);
  mensajeInfo = signal<string | null>(null);
  informe   = signal<ComplexivoInformeDto | null>(null);
  activeTab = signal<TabKey>('portada');

  puedeEditar = computed(() => {
    const est = this.informe()?.estado;
    return est === 'BORRADOR' || est === 'RECHAZADO' || est == null;
  });

  form = {
    titulo:                '',
    planteamientoProblema: '',
    objetivos:             '',
    marcoTeorico:          '',
    metodologia:           '',
    resultados:            '',
    conclusiones:          '',
    bibliografia:          ''
  };

  get progreso(): number {
    const campos = [
      this.form.planteamientoProblema,
      this.form.objetivos,
      this.form.marcoTeorico,
      this.form.metodologia,
      this.form.resultados,
      this.form.conclusiones,
    ];
    const llenos = campos.filter(c => c && c.trim().length > 10).length;
    return Math.round((llenos / campos.length) * 100);
  }

  constructor(private api: ComplexivoService) {}

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    if (!this.idEstudiante) {
      this.error.set('No se pudo identificar al estudiante.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);
    this.mensajeInfo.set(null);

    this.api.getInforme(this.idEstudiante).subscribe({
      next: (d) => {
        this.informe.set(d);
        this.form = {
          titulo:                d.titulo ?? '',
          planteamientoProblema: d.planteamientoProblema ?? '',
          objetivos:             d.objetivos ?? '',
          marcoTeorico:          d.marcoTeorico ?? '',
          metodologia:           d.metodologia ?? '',
          resultados:            d.resultados ?? '',
          conclusiones:          d.conclusiones ?? '',
          bibliografia:          d.bibliografia ?? ''
        };
        this.loading.set(false);
      },
      error: (e) => {
        const msg: string = e?.error?.message ?? e?.message ?? '';

        if (msg.includes('NO_TIENES_REGISTRO') || e?.status === 404) {
          this.api.iniciarComplexivo(this.idEstudiante!).subscribe({
            next: () => {
              this.api.getInforme(this.idEstudiante!).subscribe({
                next: (d) => {
                  this.informe.set(d);
                  this.form = {
                    titulo:                d.titulo ?? '',
                    planteamientoProblema: d.planteamientoProblema ?? '',
                    objetivos:             d.objetivos ?? '',
                    marcoTeorico:          d.marcoTeorico ?? '',
                    metodologia:           d.metodologia ?? '',
                    resultados:            d.resultados ?? '',
                    conclusiones:          d.conclusiones ?? '',
                    bibliografia:          d.bibliografia ?? ''
                  };
                  this.loading.set(false);
                },
                error: (e2) => {
                  this.error.set(e2?.error?.message ?? 'Error al cargar.');
                  this.loading.set(false);
                }
              });
            },
            error: () => {
              this.error.set(
                '⏳ Tu propuesta aún no ha sido aprobada por el docente. ' +
                'Una vez aprobada, podrás acceder al informe práctico.'
              );
              this.loading.set(false);
            }
          });
        } else {
          this.error.set(msg || 'Error al cargar el informe.');
          this.loading.set(false);
        }
      }
    });
  }

  guardar(): void {
    if (!this.idEstudiante || !this.puedeEditar()) return;
    this.saving.set(true);
    this.ok.set(null);
    this.error.set(null);
    this.mensajeInfo.set(null);

    this.api.guardarInforme(this.idEstudiante, this.form).subscribe({
      next: (d) => {
        this.informe.set(d);
        this.ok.set('💾 Informe guardado correctamente.');
        this.saving.set(false);
      },
      error: (e) => {
        this.error.set(e?.error?.message ?? 'Error al guardar.');
        this.saving.set(false);
      }
    });
  }

  enviar(): void {
    if (!this.idEstudiante) return;
    if (!confirm(
      '¿Deseas enviar el informe práctico a revisión?\n\n' +
      'Una vez enviado no podrás editarlo hasta que el docente lo revise.'
    )) return;

    this.saving.set(true);
    this.ok.set(null);
    this.error.set(null);
    this.mensajeInfo.set(null);

    const enviarDirecto = () => {
      this.api.enviarInforme(this.idEstudiante!).subscribe({
        next: (d) => {
          this.informe.set(d);
          this.ok.set('📬 Informe enviado a revisión. Tu docente será notificado.');
          this.saving.set(false);
        },
        error: (e) => {
          this.error.set(e?.error?.message ?? 'Error al enviar.');
          this.saving.set(false);
        }
      });
    };

    if (this.informe()?.estado === 'RECHAZADO') {
      this.api.guardarInforme(this.idEstudiante!, this.form).subscribe({
        next: () => enviarDirecto(),
        error: (e) => {
          this.error.set(e?.error?.message ?? 'Error al preparar el envío.');
          this.saving.set(false);
        }
      });
    } else {
      enviarDirecto();
    }
  }

  setTab(t: TabKey): void { this.activeTab.set(t); }

  // ── Recargar — descarta cambios locales y recarga desde servidor ──
  recargar(): void {
    if (!this.idEstudiante) return;
    if (confirm('¿Descartar los cambios no guardados y recargar desde el servidor?')) {
      this.mensajeInfo.set(null);
      this.cargar();
    }
  }

  // ── Copiar propuesta — llena campos con la propuesta aprobada ────
  copiarDesdePropuesta(): void {
    const inf = this.informe();
    if (!inf) return;

    const tieneDatos = this.form.planteamientoProblema.trim().length > 0
      || this.form.objetivos.trim().length > 0;

    if (tieneDatos) {
      if (!confirm('Ya tienes contenido en el informe. ¿Deseas sobreescribir con los datos de la propuesta?')) {
        return;
      }
    }

    this.form.titulo = inf.titulo ?? '';
    this.mensajeInfo.set('✅ Se copió el título desde la propuesta aprobada. Desarrolla el resto de las secciones.');
    this.ok.set(null);
    this.error.set(null);
  }

  badgeClass(estado: string | null): string {
    const map: Record<string, string> = {
      BORRADOR:  'badge-draft',
      ENTREGADO: 'badge-pending',
      APROBADO:  'badge-ok',
      RECHAZADO: 'badge-error'
    };
    return map[estado ?? ''] ?? 'badge-draft';
  }
}
