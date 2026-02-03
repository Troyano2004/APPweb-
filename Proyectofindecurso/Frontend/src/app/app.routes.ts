import { Routes } from '@angular/router';
import { EstudiantesComponent } from './pages/estudiantes/estudiantes';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'estudiantes' },
  { path: 'estudiantes', component: EstudiantesComponent },

  // opcional: cualquier ruta desconocida redirige
  { path: '**', redirectTo: 'estudiantes' }
];
