import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ComplexivoService, ComplexivoInformeDto } from '../../../services/complexivo.service';
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
  private readonly idEstudiante = getSessionEntityId(getSessionUser(), 'estudiante');

  loading  = signal(false);
  saving   = signal(false);
  error    = signal<string | null>(null);
  ok       = signal<string | null>(null);
  informe  = signal<ComplexivoInformeDto | null>(null);
  activeTab = signal<TabKey>('portada');

  puedeEditar = computed(() => {
    const est = this.informe()?.estado;
    return est === 'BORRADOR' || est == null;
  });

  form = {
    titulo: '',
    planteamientoProblema: '',
    objetivos: '',
    marcoTeorico: '',
    metodologia: '',
    resultados: '',
    conclusiones: '',
    bibliografia: ''
  };

  constructor(private api: ComplexivoService) {}

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    if (!this.idEstudiante) { this.error.set('No se pudo identificar al estudiante.'); return; }
    this.loading.set(true);
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
      error: (e) => { this.error.set(e?.error?.message ?? 'Error al cargar el informe.'); this.loading.set(false); }
    });
  }

  guardar(): void {
    if (!this.idEstudiante || !this.puedeEditar()) return;
    this.saving.set(true); this.ok.set(null); this.error.set(null);
    this.api.guardarInforme(this.idEstudiante, this.form).subscribe({
      next: (d) => { this.informe.set(d); this.ok.set('Informe guardado correctamente.'); this.saving.set(false); },
      error: (e) => { this.error.set(e?.error?.message ?? 'Error al guardar.'); this.saving.set(false); }
    });
  }

  enviar(): void {
    if (!this.idEstudiante) return;
    if (!confirm('¿Deseas enviar el informe práctico a revisión? No podrás editarlo después.')) return;
    this.saving.set(true); this.ok.set(null); this.error.set(null);
    this.api.enviarInforme(this.idEstudiante).subscribe({
      next: (d) => { this.informe.set(d); this.ok.set('Informe enviado a revisión exitosamente.'); this.saving.set(false); },
      error: (e) => { this.error.set(e?.error?.message ?? 'Error al enviar.'); this.saving.set(false); }
    });
  }

  setTab(t: TabKey): void { this.activeTab.set(t); }

  badgeClass(estado: string | null): string {
    const map: Record<string, string> = {
      BORRADOR: 'badge-draft', ENTREGADO: 'badge-pending',
      APROBADO: 'badge-ok', RECHAZADO: 'badge-error'
    };
    return map[estado ?? ''] ?? 'badge-draft';
  }
}
