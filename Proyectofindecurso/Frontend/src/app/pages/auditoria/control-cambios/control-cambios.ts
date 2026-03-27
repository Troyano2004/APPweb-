import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditoriaService } from '../service';
import { AuditLog } from '../model';

@Component({
  selector: 'app-control-cambios',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './control-cambios.html',
  styleUrl: './control-cambios.scss'
})
export class ControlCambiosComponent implements OnInit {
  cambios: AuditLog[] = [];
  entidades: string[] = [];
  usuarios: string[] = [];
  totalPages = 0;
  loading = false;

  filtros = {
    entidad: '',
    accion: '',
    username: '',
    desde: '',
    hasta: '',
    page: 0,
    size: 15
  };

  constructor(private svc: AuditoriaService) {}

  ngOnInit(): void {
    this.cargar();
    this.svc.getEntidades().subscribe(e => this.entidades = e);
    this.svc.getUsuariosConCambios().subscribe(u => this.usuarios = u);
  }

  cargar(): void {
    this.loading = true;
    this.svc.getCambios(this.filtros).subscribe({
      next: (p: any) => {
        this.cambios = p.content ?? [];
        this.totalPages = p.totalPages ?? 0;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  aplicar(): void { this.filtros.page = 0; this.cargar(); }

  limpiar(): void {
    this.filtros = { entidad: '', accion: '', username: '', desde: '', hasta: '', page: 0, size: 15 };
    this.cargar();
  }

  anterior(): void {
    if (this.filtros.page > 0) { this.filtros.page--; this.cargar(); }
  }

  siguiente(): void {
    if (this.filtros.page < this.totalPages - 1) { this.filtros.page++; this.cargar(); }
  }

  iconAccion(accion: string): string {
    const icons: Record<string, string> = {
      'CREATE': '➕', 'UPDATE': '✏️', 'DELETE': '🗑️',
      'APROBAR': '✅', 'RECHAZAR': '❌', 'VALIDAR': '🔍',
      'DECISION': '⚖️', 'APROBAR_DIRECTOR': '✅', 'DEVOLVER': '↩️'
    };
    return icons[accion] ?? '📝';
  }

  formatearJson(json: string | null | undefined): string {
    if (!json) return '—';
    try { return JSON.stringify(JSON.parse(json), null, 2); }
    catch { return json; }
  }

  claseCard(accion: string): string {
    if (accion === 'CREATE') return 'card-create';
    if (accion === 'UPDATE') return 'card-update';
    if (accion === 'DELETE') return 'card-delete';
    return 'card-otro';
  }
}
