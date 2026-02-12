import { Routes } from '@angular/router';
import { ShellComponent } from './layout/shell/shell.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { PropuestasPendientesComponent } from './pages/propuestas-pendientes/propuestas-pendientes.component';
import { DocumentoSeccionesComponent } from './pages/documento-secciones/documento-secciones.component';
import { PlaceholderPageComponent } from './pages/dashboard/placeholder/placeholder.component';
import { BancoTemasComponent } from './pages/temas/banco-temas/banco-temas.component';
import { AprobacionTemasComponent } from './pages/temas/aprobacion-temas/aprobacion-temas.component';
import { PropuestaNuevaComponent } from './pages/propuesta/propuesta-nueva/propuesta-nueva.component';
import { EstudiantesComponent } from './pages/estudiantes/estudiantes';
import { Documento } from './pages/titulacion2/documento/documento';
import { Revision } from './pages/titulacion2/revision/revision';
import { RevisionDetalle } from './pages/titulacion2/revision-detalle/revision-detalle';
import { SeguimientoProyectosComponent } from './pages/coordinador/seguimiento-proyectos/seguimiento-proyectos.component';
import { DirectoresComponent } from './pages/coordinador/directores/directores.component';
import { ValidacionComponent } from './pages/coordinador/validacion/validacion.component';
import { TutoriasControlComponent } from './pages/coordinador/tutorias-control/tutorias-control.component';
import { ObservacionesAdminComponent } from './pages/coordinador/observaciones-admin/observaciones-admin.component';
import { ReportesCoordinacionComponent } from './pages/coordinador/reportes/reportes.component';
import { ComisionFormativaComponent } from './pages/coordinador/comision-formativa/comision-formativa.component';
import { VisualizarProyectoComponent } from './pages/coordinador/visualizar-proyecto/visualizar-proyecto.component';
import { LoginComponent } from './pages/login/login';
import { AdminUsuariosComponent } from './pages/admin-usuario/admin-usuarios';
import { RolesComponent } from './pages/roles/roles';
import { authGuard, loginGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginComponent, canActivate: [loginGuard], data: { title: 'Login' } },
  {
    path: 'app',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: DashboardComponent, data: { title: 'Dashboard' } },
      { path: 'estudiantes', component: EstudiantesComponent, data: { title: 'Estudiantes' } },
      {
        path: 'titulacion2/documento',
        component: Documento,
        data: { title: 'Documento de titulación' }
      },
      { path: 'titulacion2/revision', component: Revision, data: { title: 'Revisión de director' } },
      {
        path: 'titulacion2/revision/:idDocumento',
        component: RevisionDetalle,
        data: { title: 'Detalle de revisión' }
      },

      { path: 'catalogos/universidad', component: PlaceholderPageComponent, data: { title: 'Universidad' } },
      { path: 'catalogos/facultad', component: PlaceholderPageComponent, data: { title: 'Facultad' } },
      { path: 'catalogos/carrera', component: PlaceholderPageComponent, data: { title: 'Carrera' } },
      { path: 'catalogos/modalidad', component: PlaceholderPageComponent, data: { title: 'Modalidad Titulación' } },
      { path: 'catalogos/periodo', component: PlaceholderPageComponent, data: { title: 'Período Académico' } },
      {
        path: 'catalogos/tipo-trabajo',
        component: PlaceholderPageComponent,
        data: { title: 'Tipo Trabajo Titulación' }
      },
      {
        path: 'catalogos/carrera-modalidad',
        component: PlaceholderPageComponent,
        data: { title: 'Carrera-Modalidad' }
      },

      { path: 'temas', component: BancoTemasComponent, data: { title: 'Banco de temas' } },
      { path: 'temas/nuevo', component: BancoTemasComponent, data: { title: 'Registrar tema' } },
      { path: 'temas/aprobacion', component: AprobacionTemasComponent, data: { title: 'Aprobación de propuestas' } },

      {
        path: 'propuesta/pendientes',
        component: PropuestasPendientesComponent,
        data: { title: 'Propuestas pendientes' }
      },
      { path: 'propuesta/nueva', component: PropuestaNuevaComponent, data: { title: 'Registrar propuesta' } },
      { path: 'propuesta/revision', component: PlaceholderPageComponent, data: { title: 'Revisión por director' } },
      { path: 'propuesta/historial', component: PlaceholderPageComponent, data: { title: 'Historial observaciones' } },

      { path: 'tutorias/nueva', component: PlaceholderPageComponent, data: { title: 'Registrar tutoría' } },
      { path: 'tutorias/actas', component: PlaceholderPageComponent, data: { title: 'Actas de tutoría' } },
      { path: 'tutorias/historial', component: PlaceholderPageComponent, data: { title: 'Historial' } },

      {
        path: 'proyecto/documento',
        component: DocumentoSeccionesComponent,
        data: { title: 'Documento por secciones' }
      },
      { path: 'proyecto/revision', component: PlaceholderPageComponent, data: { title: 'Revisión por secciones' } },
      { path: 'proyecto/correcciones', component: PlaceholderPageComponent, data: { title: 'Correcciones' } },
      { path: 'proyecto/estado', component: PlaceholderPageComponent, data: { title: 'Estado del proyecto' } },

      { path: 'documentos/habilitantes', component: PlaceholderPageComponent, data: { title: 'Habilitantes' } },
      { path: 'documentos/versiones', component: PlaceholderPageComponent, data: { title: 'Versiones' } },
      { path: 'documentos/expediente', component: PlaceholderPageComponent, data: { title: 'Expediente' } },

      { path: 'legalizacion/validacion', component: PlaceholderPageComponent, data: { title: 'Validación legal' } },
      { path: 'legalizacion/checklist', component: PlaceholderPageComponent, data: { title: 'Checklist' } },
      { path: 'legalizacion/aprobacion', component: PlaceholderPageComponent, data: { title: 'Aprobación final' } },

      {
        path: 'reportes/expediente',
        component: PlaceholderPageComponent,
        data: { title: 'Expediente por estudiante' }
      },
      { path: 'reportes/periodo', component: PlaceholderPageComponent, data: { title: 'Reportes por período' } },
      { path: 'reportes/actas', component: PlaceholderPageComponent, data: { title: 'Actas y constancias' } },

      { path: 'admin/usuarios', component: AdminUsuariosComponent, data: { title: 'Usuarios' } },
      { path: 'admin/roles', component: RolesComponent, data: { title: 'Roles del aplicativo' } },
      { path: 'admin/parametros', component: PlaceholderPageComponent, data: { title: 'Parámetros' } },

      {
        path: 'coordinador/seguimiento',
        component: SeguimientoProyectosComponent,
        data: { title: 'Seguimiento de proyectos' }
      },
      {
        path: 'coordinador/directores',
        component: DirectoresComponent,
        data: { title: 'Control de directores' }
      },
      {
        path: 'coordinador/validacion',
        component: ValidacionComponent,
        data: { title: 'Validación administrativa' }
      },
      {
        path: 'coordinador/tutorias',
        component: TutoriasControlComponent,
        data: { title: 'Control de tutorías' }
      },
      {
        path: 'coordinador/observaciones',
        component: ObservacionesAdminComponent,
        data: { title: 'Observaciones administrativas' }
      },
      {
        path: 'coordinador/reportes',
        component: ReportesCoordinacionComponent,
        data: { title: 'Reportes de coordinación' }
      },
      {
        path: 'coordinador/comision',
        component: ComisionFormativaComponent,
        data: { title: 'Comisión formativa' }
      },
      {
        path: 'coordinador/proyecto',
        component: VisualizarProyectoComponent,
        data: { title: 'Visualización de proyecto' }
      },

      { path: '**', redirectTo: 'dashboard' }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
