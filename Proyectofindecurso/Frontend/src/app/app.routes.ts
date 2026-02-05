import { Routes } from '@angular/router';
import { EstudiantesComponent } from './pages/estudiantes/estudiantes';
import { Documento } from './pages/titulacion2/documento/documento';

// 🔹 NUEVOS IMPORTS (director)
import { Revision } from './pages/titulacion2/revision/revision';
import { RevisionDetalle } from './pages/titulacion2/revision-detalle/revision-detalle';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'estudiantes' },

  // 👨‍🎓 Estudiante
  { path: 'estudiantes', component: EstudiantesComponent },
  { path: 'titulacion2/documento', component: Documento },

  // 👨‍🏫 Director (REVISIÓN)
  // 👨‍🏫 Director (REVISIÓN)
  { path: 'titulacion2/revision', component: Revision },
  { path: 'titulacion2/revision/:idDocumento', component: RevisionDetalle },



  // ❗ SIEMPRE al final
  { path: '**', redirectTo: 'estudiantes' }
];
