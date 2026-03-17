import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditoriaService } from '../service';
import { AuditConfig, ENTIDADES_SISTEMA, ACCIONES_SISTEMA, SEVERIDADES } from '../model';

@Component({ selector:'app-audit-config', standalone:true, imports:[CommonModule,FormsModule], templateUrl:'./audit-config.html', styleUrls:['./audit-config.scss'] })
export class AuditConfigComponent implements OnInit {
  configs: AuditConfig[] = []; formulario: AuditConfig = this.nuevo(); modoEdicion=false; idEditando?: number; mostrarFormulario=false; nuevoDestinatario='';
  entidades=ENTIDADES_SISTEMA; acciones=ACCIONES_SISTEMA; severidades=SEVERIDADES;
  constructor(private svc: AuditoriaService) {}
  ngOnInit() { this.cargar(); }
  cargar() { this.svc.getConfigs().subscribe(c => this.configs = c); }
  nuevo(): AuditConfig { return {id:0,entidad:'',accion:'CREATE',activo:true,notificarEmail:false,destinatarios:[],severidad:'LOW',descripcion:''}; }
  abrirNuevo() { this.formulario=this.nuevo(); this.modoEdicion=false; this.idEditando=undefined; this.mostrarFormulario=true; }
  abrirEditar(c: AuditConfig) { this.formulario={...c,destinatarios:[...(c.destinatarios??[])]}; this.modoEdicion=true; this.idEditando=c.id; this.mostrarFormulario=true; }
  cerrar() { this.mostrarFormulario=false; }
  guardar() {
    const obs = this.modoEdicion && this.idEditando
      ? this.svc.updateConfig(this.idEditando, this.formulario)
      : this.svc.createConfig(this.formulario);
    obs.subscribe({
      next: () => {
        this.cargar();
        this.cerrar();
      },
      error: (err) => {
        console.error('Error guardando config:', err);
        alert('Error al guardar. Revisa la consola.');
      }
    });
  }
  toggle(c: AuditConfig) { this.svc.toggleConfig(c.id).subscribe(() => this.cargar()); }
  eliminar(c: AuditConfig) { if(confirm('¿Eliminar '+c.entidad+'/'+c.accion+'?')) this.svc.deleteConfig(c.id).subscribe(()=>this.cargar()); }
  agregarDest() { const e=this.nuevoDestinatario.trim(); if(e&&!this.formulario.destinatarios!.includes(e)) this.formulario.destinatarios!.push(e); this.nuevoDestinatario=''; }
  quitarDest(i: number) { this.formulario.destinatarios!.splice(i,1); }
  colorSev(s: string) { return ({CRITICAL:'#c53030',HIGH:'#c05621',MEDIUM:'#2b6cb0',LOW:'#276749'} as any)[s]??'#276749'; }
}