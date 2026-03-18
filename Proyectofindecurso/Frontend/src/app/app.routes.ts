import { Routes } from '@angular/router';
import { DocumentosHabilitantesComponent } from './pages/habilitantes/habilitantes';
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
import { TitulacionWorkflowComponent } from './pages/titulacion2/workflow/workflow';
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
import { AsignacionDt1 } from './pages/coordinador/asignacion-dt1/asignacion-dt1';
import { authGuard, loginGuard } from './guards/auth.guard';

// Catálogos
import { UniversidadComponent } from './pages/catalogos/universidad/universidad.component';
import { FacultadComponent } from './pages/catalogos/facultad/facultad.component';
import { CarreraComponent } from './pages/catalogos/carrera/carrera.component';
import { ModalidadCatalogoComponent } from './pages/catalogos/modalidad/modalidad-catalogo.component';
import { PeriodoComponent } from './pages/catalogos/periodo/periodo.component';
import { TipoTrabajoComponent } from './pages/catalogos/tipo-trabajo/tipo-trabajo.component';
import { CarreraModalidadComponent } from './pages/catalogos/carrera-modalidad/carrera-modalidad.component';

// Otros
import { Historialtutorias } from './pages/historialtutorias/historialtutorias';
import { Dt1EnviadosComponent } from './pages/dt1/dt1lista/dt1lista';
import { Dt1RevisionComponent } from './pages/dt1/dt1revision/dt1revision';
import { AnteproyectoComponent } from './pages/anteproyecto/anteproyecto';
import { Actadirector } from './pages/director/actadirector/actadirector';
import { Tutoriasdirector } from './pages/director/tutoriasdirector/tutoriasdirector';
import { DirectorMisAnteproyectosComponent } from './pages/director/directoranteproyectos/directoranteproyectos';
import { RegistroEstudianteComponent } from './pages/registro-estudiante/registro-estudiante';
import { GestionSolicitudesComponent } from './pages/gestion-solicitudes/gestion-solicitudes';
import { ConfiguracionCorreoComponent } from './pages/configuracion-correo/configuracion-correo';
import { AuditLogsComponent }      from './pages/auditoria/audit-logs/audit-logs';
import { AuditConfigComponent }    from './pages/auditoria/audit-config/audit-config';
import { AuditDashboardComponent } from './pages/auditoria/audit-dashboard/audit-dashboard';
import { ExpedienteComponent } from './pages/reportes/expediente/expediente.component';
import { ReportePeriodoComponent } from './pages/reportes/periodo/periodo.component';
import { ActasComponent }      from './pages/reportes/actas/actas.component';
import { ParametrosComponent } from './pages/parametros/parametros.component';

// ✅ Módulos Titulación II (DT2)
import { ConfiguracionDt2Component } from './pages/coordinador/configuracion-dt2/configuracion-dt2';
import { SeguimientoDt2Component } from './pages/director/seguimiento-dt2/seguimiento-dt2';
import { AntiplagioDt2Component } from './pages/director/antiplagio-dt2/antiplagio-dt2';
import { PredefensaDt2Component } from './pages/titulacion2/predefensa/predefensa-dt2';
import { SustentacionDt2Component } from './pages/titulacion2/sustentacion/sustentacion-dt2';

import { ReportePropuestasComponent } from './pages/propuesta/reporte-propuestas/reporte-propuestas.component';


import { BackupJobsComponent } from './pages/backup/backup-jobs/backup-jobs.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginComponent, canActivate: [loginGuard], data: { title: 'Login' } },
  { path: 'registro-estudiante', component: RegistroEstudianteComponent, data: { title: 'Registro Estudiante' } },
  {
    path: 'app',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: DashboardComponent, data: { title: 'Dashboard' } },

      // Estudiante / Anteproyecto
      { path: 'anteproyecto/nuevo', component: AnteproyectoComponent, data: { title: 'Registrar anteproyecto' } },
      { path: 'tutorias/historial', component: Historialtutorias, data: { title: 'Historial de Tutorías' } },

      // Director / DT1
      { path: 'director/mis-anteproyectos', component: DirectorMisAnteproyectosComponent, data: { title: 'Mis anteproyectos' } },
      { path: 'director/tutorias',          component: Tutoriasdirector,                  data: { title: 'Tutorías (Director)' } },
      { path: 'director/acta',              component: Actadirector,                      data: { title: 'Acta de revisión (Director)' } },
      { path: 'dt1/lista',                  component: Dt1EnviadosComponent,              data: { title: 'DT1 - Lista' } },
      { path: 'dt1/revision',               component: Dt1RevisionComponent,              data: { title: 'DT1 - Revisión' } },

      // ── Titulación II — DT2 ──────────────────────────────────────────────────
      { path: 'coordinador/configuracion-dt2', component: ConfiguracionDt2Component, data: { title: 'DT2 - Configuración Inicial' } },
      { path: 'director/seguimiento-dt2',      component: SeguimientoDt2Component,   data: { title: 'DT2 - Seguimiento de Avances' } },
      { path: 'director/antiplagio-dt2',       component: AntiplagioDt2Component,    data: { title: 'DT2 - Antiplagio COMPILATIO' } },
      { path: 'titulacion2/predefensa',         component: PredefensaDt2Component,    data: { title: 'DT2 - Predefensa' } },
      { path: 'titulacion2/sustentacion',       component: SustentacionDt2Component,  data: { title: 'DT2 - Sustentación Final' } },

      // ── Titulación II — Documento / Revisión ─────────────────────────────────
      { path: 'titulacion2/documento',                component: Documento,                    data: { title: 'Documento de titulación' } },
      { path: 'titulacion2/revisar',                  component: Revision,                     data: { title: 'Revisión de director' } },
      { path: 'titulacion2/revisar/:idDocumento',     component: RevisionDetalle,              data: { title: 'Detalle de revisión' } },
      { path: 'titulacion2/workflow',                 component: TitulacionWorkflowComponent,  data: { title: 'Workflow Titulación II' } },
      // Redirecciones de compatibilidad (rutas antiguas → nuevas)
      { path: 'titulacion2/revision',                 pathMatch: 'full', redirectTo: 'titulacion2/revisar' },
      { path: 'titulacion2/revision/:idDocumento',    pathMatch: 'full', redirectTo: 'titulacion2/revisar/:idDocumento' },

      // ── Estudiantes ──────────────────────────────────────────────────────────
      { path: 'estudiantes', component: EstudiantesComponent, data: { title: 'Estudiantes' } },

      // ── Catálogos Académicos ─────────────────────────────────────────────────
      { path: 'catalogos/universidad',      component: UniversidadComponent,         data: { title: 'Universidad' } },
      { path: 'catalogos/facultad',         component: FacultadComponent,            data: { title: 'Facultad' } },
      { path: 'catalogos/carrera',          component: CarreraComponent,             data: { title: 'Carrera' } },
      { path: 'catalogos/modalidad',        component: ModalidadCatalogoComponent,   data: { title: 'Modalidad Titulación' } },
      { path: 'catalogos/periodo',          component: PeriodoComponent,             data: { title: 'Período Académico' } },
      { path: 'catalogos/tipo-trabajo',     component: TipoTrabajoComponent,         data: { title: 'Tipo Trabajo Titulación' } },
      { path: 'catalogos/carrera-modalidad', component: CarreraModalidadComponent,  data: { title: 'Carrera-Modalidad' } },

      // ── Banco de Temas ───────────────────────────────────────────────────────
      { path: 'temas',           component: BancoTemasComponent,     data: { title: 'Banco de temas' } },
      { path: 'temas/nuevo',     component: BancoTemasComponent,     data: { title: 'Registrar tema' } },
      { path: 'temas/aprobacion', component: AprobacionTemasComponent, data: { title: 'Aprobación de propuestas' } },

      // ── Propuesta y Anteproyecto ─────────────────────────────────────────────
      // ── Propuesta y Anteproyecto ─────────────────────────────────────────────
      { path: 'propuesta/pendientes', component: PropuestasPendientesComponent, data: { title: 'Propuestas pendientes' } },
      { path: 'propuesta/nueva',      component: PropuestaNuevaComponent,       data: { title: 'Registrar propuesta' } },
      { path: 'propuesta/reporte',    component: ReportePropuestasComponent,    data: { title: 'Reporte de propuestas' } },
      { path: 'propuesta/revision',   component: PlaceholderPageComponent,      data: { title: 'Revisión por director' } },
      { path: 'propuesta/historial',  component: PlaceholderPageComponent,      data: { title: 'Historial observaciones' } },

      // Tutorías
      { path: 'tutorias/nueva', component: PlaceholderPageComponent, data: { title: 'Registrar tutoría' } },
      { path: 'tutorias/actas', component: PlaceholderPageComponent, data: { title: 'Actas de tutoría' } },

      // Proyecto
      { path: 'proyecto/documento', component: DocumentoSeccionesComponent, data: { title: 'Documento por secciones' } },
      { path: 'proyecto/revision', component: PlaceholderPageComponent, data: { title: 'Revisión por secciones' } },
      { path: 'proyecto/correcciones', component: PlaceholderPageComponent, data: { title: 'Correcciones' } },
      { path: 'proyecto/estado', component: PlaceholderPageComponent, data: { title: 'Estado del proyecto' } },
      // ── Proyecto de Titulación ───────────────────────────────────────────────
      { path: 'proyecto/documento',    component: DocumentoSeccionesComponent, data: { title: 'Documento por secciones' } },
      { path: 'proyecto/revision',     component: PlaceholderPageComponent,    data: { title: 'Revisión por secciones' } },
      { path: 'proyecto/correcciones', component: PlaceholderPageComponent,    data: { title: 'Correcciones' } },
      { path: 'proyecto/estado',       component: PlaceholderPageComponent,    data: { title: 'Estado del proyecto' } },

      // ── Documentos ───────────────────────────────────────────────────────────
      // ✅ documentos/habilitantes usa el componente real (preservado del doc3)
      { path: 'documentos/habilitantes', component: DocumentosHabilitantesComponent, data: { title: 'Habilitantes' } },
      { path: 'documentos/versiones',    component: PlaceholderPageComponent,        data: { title: 'Versiones' } },
      { path: 'documentos/expediente',   component: PlaceholderPageComponent,        data: { title: 'Expediente' } },

      // ── Legalización ─────────────────────────────────────────────────────────
      { path: 'legalizacion/validacion', component: PlaceholderPageComponent, data: { title: 'Validación legal' } },
      { path: 'legalizacion/checklist',  component: PlaceholderPageComponent, data: { title: 'Checklist' } },
      { path: 'legalizacion/aprobacion', component: PlaceholderPageComponent, data: { title: 'Aprobación final' } },

      // ── Reportes ─────────────────────────────────────────────────────────────
      { path: 'reportes/expediente',  component: ExpedienteComponent, data: { title: 'Expediente por estudiante' } },
      { path: 'reportes/periodo',     component: ReportePeriodoComponent, data: { title: 'Por periodo' } },
      { path: 'reportes/actas',       component: ActasComponent,      data: { title: 'Actas y constancias' } },

      // Administración
      { path: 'admin/usuarios', component: AdminUsuariosComponent, data: { title: 'Usuarios' } },
      { path: 'admin/roles', component: RolesComponent, data: { title: 'Roles del aplicativo' } },
      { path: 'admin/parametros', component: ParametrosComponent, data: { title: 'Parametros' } },
      { path: 'admin/gestion-solicitudes', component: GestionSolicitudesComponent, data: { title: 'Gestión de Solicitudes' } },
      { path: 'admin/configuracion-correo', component: ConfiguracionCorreoComponent, data: { title: 'Configuración de Correo' } },
      // ── Backup ───────────────────────────────────────────────────────────────────
      { path: 'admin/backup', component: BackupJobsComponent, data: { title: 'Respaldos de Base de Datos' } },
      { path: 'admin/auditoria/dashboard', component: AuditDashboardComponent, data: { title: 'Dashboard Auditoría' } },
      { path: 'admin/auditoria/logs',      component: AuditLogsComponent,      data: { title: 'Logs de Auditoría' } },
      { path: 'admin/auditoria/config',    component: AuditConfigComponent,     data: { title: 'Configuración Auditoría' } },

      // ── Coordinación ─────────────────────────────────────────────────────────
      { path: 'coordinador/seguimiento',   component: SeguimientoProyectosComponent, data: { title: 'Seguimiento de proyectos' } },
      { path: 'coordinador/directores',    component: DirectoresComponent,           data: { title: 'Control de directores' } },
      { path: 'coordinador/validacion',    component: ValidacionComponent,           data: { title: 'Validación administrativa' } },
      { path: 'coordinador/tutorias',      component: TutoriasControlComponent,      data: { title: 'Control de tutorías' } },
      { path: 'coordinador/observaciones', component: ObservacionesAdminComponent,   data: { title: 'Observaciones administrativas' } },
      { path: 'coordinador/reportes',      component: ReportesCoordinacionComponent, data: { title: 'Reportes de coordinación' } },
      { path: 'coordinador/comision',      component: ComisionFormativaComponent,    data: { title: 'Comisión formativa' } },
      { path: 'coordinador/proyecto',      component: VisualizarProyectoComponent,   data: { title: 'Visualización de proyecto' } },
      { path: 'coordinador/dt1-asignacion', component: AsignacionDt1,               data: { title: 'DT1 - Asignación Docentes y Tutores' } },

      // ── Rutas de secciones pendientes (roles nuevos del shell) ────────────────
      { path: 'legal/validacion',           component: PlaceholderPageComponent, data: { title: 'Validación jurídica' } },
      { path: 'secretaria/actas',           component: PlaceholderPageComponent, data: { title: 'Registro de actas' } },
      { path: 'secretaria/documentos',      component: PlaceholderPageComponent, data: { title: 'Gestión documental' } },
      { path: 'director-admin/gestion',     component: PlaceholderPageComponent, data: { title: 'Gestión institucional' } },
      { path: 'director-admin/reportes',    component: PlaceholderPageComponent, data: { title: 'Aprobación de reportes' } },

      { path: '**', redirectTo: 'dashboard' }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
