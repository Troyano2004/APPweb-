import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AuditoriaService } from '../service';
import { AuditLog, AuditFiltros, traducirAccion } from '../model';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-logs.html',
  styleUrls: ['./audit-logs.scss']
})
export class AuditLogsComponent implements OnInit, OnDestroy {

  logs: AuditLog[] = [];
  totalElements = 0;
  totalPages    = 0;
  loading       = false;
  logSeleccionado: AuditLog | null = null;
  filtros: AuditFiltros = { page: 0, size: 20 };
  entidades: string[] = [];
  acciones:  string[] = [];

  enVivo         = false;
  conectado      = false;
  nuevosNoVistos = 0;
  exportandoPdf   = false;
  exportandoExcel = false;
  private streamSub?: Subscription;

  constructor(private svc: AuditoriaService) {}

  ngOnInit() {
    this.filtros = { page: 0, size: 20 };
    this.cargar();
    this.svc.getEntidades().subscribe(e => this.entidades = e);
    this.svc.getAcciones().subscribe(a => this.acciones  = a);
  }

  ngOnDestroy() {
    this.svc.desconectarStream();
    this.streamSub?.unsubscribe();
  }

  toggleEnVivo() {
    if (this.enVivo) {
      this.enVivo    = false;
      this.conectado = false;
      this.svc.desconectarStream();
      this.streamSub?.unsubscribe();
    } else {
      this.enVivo   = true;
      this.streamSub = this.svc.conectarStream().subscribe({
        next: (log: any) => {
          this.conectado = true;
          this.logs = [log, ...this.logs].slice(0, 20);
          this.totalElements++;
        },
        error: () => { this.conectado = false; }
      });
    }
  }

  // ─── Carga paginada normal
  cargar() {
    this.loading = true;
    this.svc.getLogs(this.filtros).subscribe({
      next: (p: any) => {
        this.logs           = p.content      ?? [];
        this.totalElements  = p.totalElements ?? 0;
        this.totalPages     = p.totalPages    ?? 0;
        this.loading        = false;
        this.nuevosNoVistos = 0;
      },
      error: () => { this.loading = false; }
    });
  }

  aplicarFiltros()  { this.filtros.page = 0; this.cargar(); }
  limpiarFiltros()  { this.filtros = { page: 0, size: 20 }; this.cargar(); }
  paginaAnterior()  { if (this.filtros.page > 0)                   { this.filtros.page--; this.cargar(); } }
  paginaSiguiente() { if (this.filtros.page < this.totalPages - 1) { this.filtros.page++; this.cargar(); } }
  verDetalle(log: AuditLog) { this.logSeleccionado = log; }
  cerrarDetalle()   { this.logSeleccionado = null; }

  exportarPdf() {
    this.exportandoPdf = true;
    this.svc.exportPdf(this.filtros).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'Auditoria_' + new Date().toISOString().slice(0, 10) + '.pdf';
        a.click();
        URL.revokeObjectURL(url);
        this.exportandoPdf = false;
      },
      error: () => {
        alert('Error al exportar PDF');
        this.exportandoPdf = false;
      }
    });
  }

  exportarExcel() {
    this.exportandoExcel = true;
    this.svc.exportExcel(this.filtros).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'Auditoria_' + new Date().toISOString().slice(0, 10) + '.xlsx';
        a.click();
        URL.revokeObjectURL(url);
        this.exportandoExcel = false;
      },
      error: () => {
        alert('Error al exportar Excel');
        this.exportandoExcel = false;
      }
    });
  }

  colorSeveridad(s?: string): string {
    const m: Record<string, string> = {
      CRITICAL: '#c53030', HIGH: '#c05621', MEDIUM: '#2b6cb0', LOW: '#276749'
    };
    return m[s ?? 'LOW'] ?? '#276749';
  }

  traducirAccion = traducirAccion;

  parsearJson(json: string | null): string {
    if (!json) return '—';
    if (typeof json === 'object') return JSON.stringify(json, null, 2);
    try { return JSON.stringify(JSON.parse(json), null, 2); } catch { return json; }
  }
}
