import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditoriaService } from '../service';
import { AuditLog, AuditFiltros, ENTIDADES_SISTEMA, ACCIONES_SISTEMA } from '../model';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-logs.html',
  styleUrls: ['./audit-logs.scss']
})
export class AuditLogsComponent implements OnInit {
  logs: AuditLog[] = [];
  totalElements = 0; totalPages = 0; loading = false;
  logSeleccionado: AuditLog | null = null;
  filtros: AuditFiltros = { page: 0, size: 20 };
  entidades = ENTIDADES_SISTEMA; acciones = ACCIONES_SISTEMA;

  constructor(private svc: AuditoriaService) {}
  ngOnInit() {
    this.filtros = { page: 0, size: 20 };
    this.cargar();
  }

  cargar() {
    this.loading = true;
    this.svc.getLogs(this.filtros).subscribe({
      next: (p: any) => {
        this.logs = p.content ?? [];
        this.totalElements = p.totalElements ?? 0;
        this.totalPages = p.totalPages ?? 0;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error cargando logs:', err);
        this.loading = false;
      }
    });
  }
  aplicarFiltros() { this.filtros.page = 0; this.cargar(); }
  limpiarFiltros() {
    this.filtros = { page: 0, size: 20 };
    this.cargar();
  }
  paginaAnterior() { if (this.filtros.page > 0) { this.filtros.page--; this.cargar(); } }
  paginaSiguiente() { if (this.filtros.page < this.totalPages - 1) { this.filtros.page++; this.cargar(); } }
  verDetalle(log: AuditLog) { this.logSeleccionado = log; }
  cerrarDetalle() { this.logSeleccionado = null; }
  exportar() {
    this.svc.exportCsv(this.filtros).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href = url; a.download = 'auditoria.csv'; a.click(); URL.revokeObjectURL(url);
    });
  }
  colorSeveridad(s?: string): string {
    const m: Record<string,string> = { CRITICAL:'#c53030', HIGH:'#c05621', MEDIUM:'#2b6cb0', LOW:'#276749' };
    return m[s ?? 'LOW'] ?? '#276749';
  }
  parsearJson(json: string | null): string {
    if (!json) return '—';
    try { return JSON.stringify(JSON.parse(json), null, 2); } catch { return json; }
  }
}