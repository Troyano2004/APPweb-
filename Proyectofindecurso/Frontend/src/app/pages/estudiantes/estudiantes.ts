import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EstudianteService, Estudiante } from '../../services/estudiante';

@Component({
  selector: 'app-estudiantes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './estudiantes.html',
  styleUrls: ['./estudiantes.scss']
})
export class EstudiantesComponent implements OnInit {

  listaEstudiantes: Estudiante[] = [];
  cargando = true;
  errorMsg = '';

  constructor(
    private estudianteService: EstudianteService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando = true;
    this.errorMsg = '';

    this.estudianteService.getEstudiantes().subscribe({
      next: (res: any) => {
        console.log('✅ RAW API:', res);
        console.log('✅ isArray?', Array.isArray(res));

        const arr =
          Array.isArray(res) ? res :
            (res?.data ?? res?.content ?? res?.result ?? res?.items ?? []);

        this.listaEstudiantes = Array.isArray(arr) ? arr : [];

        console.log('✅ FINAL length:', this.listaEstudiantes.length);

        this.cargando = false;

        // fuerza refresco de vista
        this.cdr.detectChanges();
      },
      error: (e)=>{
        this.errorMsg = `Error al cargar (status: ${e?.status ?? '??'})`;
        this.cargando = false;
      }
    });
  }
}
