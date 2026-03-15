import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SesionesActivasService } from './service';
import { SesionActivaDto } from './model';


@Component({
  selector: 'app-sesiones-activas',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sesiones-activas.html',
  styleUrls: ['./sesiones-activas.scss']
})
export class SesionesActivas implements OnInit{

  mensaje = ""
  error = ""
  sesiones: SesionActivaDto[] = []
  exito = ""
  cargando = false;


  constructor( private cdr: ChangeDetectorRef, private service: SesionesActivasService) {
  }

  ngOnInit() {
    this.cargar()
  }


  cargar ():void {
    this.cargando = true;
    this.cdr.detectChanges();
    this.service.listarActivas().subscribe({
      next: data => {
        this.sesiones = data;
        this.cargando = false;
        if (!this.sesiones.length) this.mensaje = 'No hay sesiones activas.';
        else this.mensaje = '';
        this.cdr.detectChanges()


      },
      error: () =>{
        this.error =  'No se pudo cargar las sesiones.';
        this.cargando = false;
        this.cdr.detectChanges()
      }
    })

  }
  cerrarSesion(sesion:SesionActivaDto){
    if (!confirm(`¿Cerrar sesión de ${sesion.nombres} ${sesion.apellidos}?`)) return;
    this.cargando = true;
    this.cdr.detectChanges()
    this.service.cerrarSesion(sesion.id).subscribe({
      next: ()=>{
        this.cargando=false;
        this.cdr.detectChanges();
        this.exito = `Sesión de ${sesion.nombres} cerrada correctamente.`;
        setTimeout(() => this.exito = '', 4000);
        this.cargar();
      },
      error: () =>{
        this.error = 'No se pudo cerrar la sesión.';
        setTimeout(() => this.error = '', 4000);

      }
    })


  }

}
