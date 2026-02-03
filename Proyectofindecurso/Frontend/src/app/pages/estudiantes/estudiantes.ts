import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // Necesario para *ngFor y *ngIf
import { EstudianteService } from '../../services/estudiante'; // Verifica esta ruta

@Component({
  selector: 'app-estudiantes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './estudiantes.html', // Verifica el nombre del archivo
  styleUrl: './estudiantes.scss'     // Verifica el nombre del archivo
})
export class EstudiantesComponent implements OnInit {

  listaEstudiantes: any[] = [];
  cargando: boolean = true; // Para mostrar mensaje de carga

  constructor(private estudianteService: EstudianteService) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos() {
    this.cargando = true;
    this.estudianteService.getEstudiantes().subscribe({
      next: (data) => {
        this.listaEstudiantes = data;
        this.cargando = false;
        console.log('Datos cargados exitosamente:', data);
      },
      error: (e) => {
        console.error('Error al conectar con la API:', e);
        this.cargando = false;
        // Tip: Si sale error CORS, revisa tu Backend
      }
    });
  }
}
