import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuditoriaService } from '../service';
import { AuditStats } from '../model';

@Component({ selector:'app-audit-dashboard', standalone:true, imports:[CommonModule,RouterModule], templateUrl:'./audit-dashboard.html', styleUrls:['./audit-dashboard.scss'] })
export class AuditDashboardComponent implements OnInit, OnDestroy {
  stats: AuditStats | null = null;
  private streamSub?: Subscription;

  constructor(private svc: AuditoriaService) {}

  ngOnInit() {
    this.cargarStats();
    this.streamSub = this.svc.conectarStream().subscribe(() => {
      this.cargarStats();
    });
  }

  ngOnDestroy() {
    this.svc.desconectarStream();
    this.streamSub?.unsubscribe();
  }

  cargarStats() {
    this.svc.getStats().subscribe({
      next: s => this.stats = s,
      error: err => console.error('Error cargando stats de auditoría:', err)
    });
  }

  maxTotal(arr: {total:number}[]): number { return arr?.length ? Math.max(...arr.map(x=>x.total)) : 1; }
}