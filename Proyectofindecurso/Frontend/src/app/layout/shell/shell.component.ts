import { Component, OnDestroy, OnInit } from '@angular/core';
import {
  ActivatedRoute,
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { filter, Subscription } from 'rxjs';
import { getSessionUser, getUserRoles } from '../../services/session';

type AppRole =
  | 'ADMIN'
  | 'DOCENTE'
  | 'DOCENTE_TITULADO'
  | 'ESTUDIANTE'
  | 'COORDINADOR'
  | 'SECRETARIO'
  | 'ABOGADO'
  | 'DIRECTOR_ADMINISTRATIVO'
  | 'GESTOR_USUARIOS'
  | 'ROL_REPORT';

interface MenuItem {
  label: string;
  path: string;
  roles?: AppRole[];
}

interface MenuSection {
  title: string;
  icon: string;
  roles?: AppRole[];
  items: MenuItem[];
}

interface SearchResult {
  label: string;
  section: string;
  icon: string;
  path: string;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss'],
})
export class ShellComponent implements OnInit, OnDestroy {
  isCollapsed      = false;
  isMobileOpen     = false;
  openSectionIndex = 0;
  currentTitle     = 'Dashboard';
  breadcrumb       = 'Inicio / Dashboard';
  userName         = 'Usuario';
  userRole         = 'Sistema';
  isDarkMode       = false;

  // ── Búsqueda ──────────────────────────────────────────
  searchQuery   = '';
  searchFocused = false;
  searchResults: SearchResult[] = [];

  private readonly subscriptions = new Subscription();

  private readonly ALL_SECTIONS: MenuSection[] = [
    {
      title: 'Dashboard',
      icon: '🏠',
      roles: ['ADMIN','DOCENTE','DOCENTE_TITULADO','ESTUDIANTE','COORDINADOR','SECRETARIO','ABOGADO','DIRECTOR_ADMINISTRATIVO'],
      items: [
        { label: 'Resumen general',  path: '/app/dashboard', roles: ['ADMIN','COORDINADOR','DIRECTOR_ADMINISTRATIVO'] },
        { label: 'Mi panel docente', path: '/app/dashboard', roles: ['DOCENTE','DOCENTE_TITULADO'] },
        { label: 'Mi panel',         path: '/app/dashboard', roles: ['ESTUDIANTE','SECRETARIO','ABOGADO'] },
      ],
    },
    {
      title: 'Catálogos Académicos',
      icon: '🎓',
      roles: ['ADMIN', 'COORDINADOR'],
      items: [
        { label: 'Universidad',             path: '/app/catalogos/universidad' },
        { label: 'Facultad',                path: '/app/catalogos/facultad' },
        { label: 'Carrera',                 path: '/app/catalogos/carrera' },
        { label: 'Modalidad Titulación',    path: '/app/catalogos/modalidad' },
        { label: 'Período Académico',       path: '/app/catalogos/periodo' },
        { label: 'Tipo Trabajo Titulación', path: '/app/catalogos/tipo-trabajo' },
        { label: 'Carrera-Modalidad',       path: '/app/catalogos/carrera-modalidad' },
      ],
    },
    {
      title: 'Banco de Temas',
      icon: '📚',
      roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ADMIN', 'COORDINADOR'],
      items: [
        { label: 'Listado de temas', path: '/app/temas' },
        { label: 'Registrar tema',   path: '/app/temas/nuevo' },
        { label: 'Aprobación temas', path: '/app/temas/aprobacion' },
      ],
    },
    {
      title: 'Propuesta y Anteproyecto',
      icon: '📝',
      roles: ['ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'ESTUDIANTE'],
      items: [{ label: 'Reporte de propuestas', path: '/app/propuesta/reporte', roles: ['ADMIN','COORDINADOR','DOCENTE','DOCENTE_TITULADO'] },
        { label: 'Propuestas pendientes',   path: '/app/propuesta/pendientes',  roles: ['ADMIN','DOCENTE','DOCENTE_TITULADO'] },
        { label: 'Registrar propuesta',     path: '/app/propuesta/nueva',       roles: ['ESTUDIANTE','ADMIN'] },
        { label: 'Registrar anteproyecto',  path: '/app/anteproyecto/nuevo',    roles: ['ESTUDIANTE','ADMIN'] },
        { label: 'Revisión por director',   path: '/app/propuesta/revision',    roles: ['DOCENTE','DOCENTE_TITULADO','ADMIN'] },
        { label: 'Historial observaciones', path: '/app/tutorias/historial',    roles: ['ESTUDIANTE','ADMIN'] },
        { label: 'Historial observaciones', path: '/app/propuesta/historial',   roles: ['DOCENTE','DOCENTE_TITULADO'] },
      ],
    },
    {
      title: 'Tutorías y Dirección',
      icon: '👨‍🏫',
      roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ADMIN'],
      items: [
        { label: 'Mis anteproyectos',           path: '/app/director/mis-anteproyectos' },
        { label: 'Tutorías',                    path: '/app/director/tutorias' },
        { label: 'Acta de revisión',            path: '/app/director/acta' },
        { label: 'Revisión Final Anteproyecto', path: '/app/dt1/lista' },
        { label: 'Registrar tutoría',           path: '/app/tutorias/nueva' },
        { label: 'Actas de tutoría',            path: '/app/tutorias/actas' },
        { label: 'Historial',                   path: '/app/tutorias/historial', roles: ['ADMIN'] },
      ],
    },
    {
      title: 'Proyecto de Titulación',
      icon: '📄',
      roles: ['ADMIN', 'DOCENTE', 'DOCENTE_TITULADO'],
      items: [
        { label: 'Documento por secciones', path: '/app/proyecto/documento' },
        { label: 'Revisión por secciones',  path: '/app/proyecto/revision' },
        { label: 'Correcciones',            path: '/app/proyecto/correcciones' },
        { label: 'Estado del proyecto',     path: '/app/proyecto/estado' },
      ],
    },
    {
      title: 'Titulación II',
      icon: '🧐',
      roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ESTUDIANTE', 'ADMIN'],
      items: [
        { label: 'Documento de titulación', path: '/app/titulacion2/documento',    roles: ['ESTUDIANTE'] },
        { label: 'Documentos pendientes',   path: '/app/titulacion2/revisar',      roles: ['DOCENTE','DOCENTE_TITULADO'] },
        { label: 'Workflow Proceso',        path: '/app/titulacion2/workflow',     roles: ['ADMIN','DOCENTE','DOCENTE_TITULADO'] },
        { label: 'Seguimiento DT2',         path: '/app/director/seguimiento-dt2', roles: ['DOCENTE','DOCENTE_TITULADO','ADMIN'] },
        { label: 'Antiplagio COMPILATIO',   path: '/app/director/antiplagio-dt2',  roles: ['DOCENTE','DOCENTE_TITULADO','ADMIN'] },
        { label: 'Predefensa',              path: '/app/titulacion2/predefensa',   roles: ['DOCENTE','DOCENTE_TITULADO','ADMIN'] },
        { label: 'Sustentación Final',      path: '/app/titulacion2/sustentacion', roles: ['DOCENTE','DOCENTE_TITULADO','ADMIN'] },
      ],
    },
    {
      title: 'Documentos',
      icon: '🗂️',
      roles: ['ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'ESTUDIANTE', 'SECRETARIO'],
      items: [
        { label: 'Habilitantes', path: '/app/documentos/habilitantes' },
        { label: 'Versiones',    path: '/app/documentos/versiones' },
        { label: 'Expediente',   path: '/app/documentos/expediente' },
      ],
    },
    {
      title: 'Legalización',
      icon: '⚖️',
      roles: ['ADMIN', 'COORDINADOR', 'ABOGADO', 'SECRETARIO'],
      items: [
        { label: 'Validación legal', path: '/app/legalizacion/validacion' },
        { label: 'Checklist',        path: '/app/legalizacion/checklist' },
        { label: 'Aprobación final', path: '/app/legalizacion/aprobacion' },
      ],
    },
    {
      title: 'Reportes',
      icon: '📊',
      roles: ['ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'COORDINADOR', 'DIRECTOR_ADMINISTRATIVO', 'ROL_REPORT'],
      items: [
        { label: 'Expediente por estudiante', path: '/app/reportes/expediente' },
        { label: 'Por período',               path: '/app/reportes/periodo' },
        { label: 'Actas y constancias',       path: '/app/reportes/actas' },
      ],
    },
    {
      title: 'Coordinación',
      icon: '🧭',
      roles: ['COORDINADOR', 'ADMIN'],
      items: [
        { label: 'Seguimiento de proyectos',      path: '/app/coordinador/seguimiento' },
        { label: 'Control de directores',         path: '/app/coordinador/directores' },
        { label: 'Validación administrativa',     path: '/app/coordinador/validacion' },
        { label: 'Control de tutorías',           path: '/app/coordinador/tutorias' },
        { label: 'Observaciones administrativas', path: '/app/coordinador/observaciones' },
        { label: 'DT2 - Configuración Inicial',   path: '/app/coordinador/configuracion-dt2' },
        { label: 'DT2 - Predefensa',              path: '/app/titulacion2/predefensa' },
        { label: 'DT2 - Sustentación Final',      path: '/app/titulacion2/sustentacion' },
        { label: 'Workflow Titulación II',        path: '/app/titulacion2/workflow' },
        { label: 'Reportes',                      path: '/app/coordinador/reportes' },
        { label: 'Comisión formativa',            path: '/app/coordinador/comision' },
        { label: 'DT1 - Docentes y Tutores',      path: '/app/coordinador/dt1-asignacion' },
      ],
    },
    {
      title: 'Dirección Administrativa',
      icon: '🏛️',
      roles: ['DIRECTOR_ADMINISTRATIVO', 'ADMIN'],
      items: [
        { label: 'Gestión institucional',  path: '/app/director-admin/gestion' },
        { label: 'Aprobación de reportes', path: '/app/director-admin/reportes' },
      ],
    },
    {
      title: 'Secretaría',
      icon: '📋',
      roles: ['SECRETARIO', 'ADMIN'],
      items: [
        { label: 'Registro de actas',  path: '/app/secretaria/actas' },
        { label: 'Gestión documental', path: '/app/secretaria/documentos' },
      ],
    },
    {
      title: 'Asesoría Legal',
      icon: '⚖️',
      roles: ['ABOGADO', 'ADMIN'],
      items: [
        { label: 'Legalización',        path: '/app/legalizacion/validacion' },
        { label: 'Validación jurídica', path: '/app/legal/validacion' },
      ],
    },
    {
      title: 'Administración del aplicativo',
      icon: '🛠️',
      roles: ['ADMIN', 'GESTOR_USUARIOS'],
      items: [
        { label: 'Usuarios',                path: '/app/admin/usuarios' },
        { label: 'Roles y permisos',        path: '/app/admin/roles' },
        { label: 'Parámetros',              path: '/app/admin/parametros' },
        { label: 'Gestión de solicitudes',  path: '/app/admin/gestion-solicitudes' },
        { label: 'Configuración de correo', path: '/app/admin/configuracion-correo' },
        { label: '🗄️ Respaldos',     path: '/app/admin/backup' },
      ],
    },
  ];

  menuSections: MenuSection[] = [];

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.loadUserData();
    this.loadTheme();
    this.menuSections = this.buildMenuByRoles();
    this.openSectionIndex = this.menuSections.length ? 0 : -1;
    this.updateTitles();

    const sub = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.updateTitles());
    this.subscriptions.add(sub);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  // ── Sidebar ───────────────────────────────────────────
  toggleCollapse(): void { this.isCollapsed = !this.isCollapsed; }
  toggleMobile():   void { this.isMobileOpen = !this.isMobileOpen; }
  closeMobile():    void { this.isMobileOpen = false; }

  toggleSection(index: number): void {
    this.openSectionIndex = this.openSectionIndex === index ? -1 : index;
  }

  // ── Tema ──────────────────────────────────────────────
  toggleTheme(): void {
    this.isDarkMode = !this.isDarkMode;
    document.body.classList.toggle('dark-mode', this.isDarkMode);
    localStorage.setItem('theme', this.isDarkMode ? 'dark' : 'light');
  }

  // ── Sesión ────────────────────────────────────────────
  logout(): void {
    localStorage.removeItem('usuario');
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

  // ── Búsqueda ──────────────────────────────────────────

  updateSearchResults(): void {
    const q = this.searchQuery.trim().toLowerCase();
    if (!q) { this.searchResults = []; return; }

    const seen    = new Set<string>();
    const results: SearchResult[] = [];

    for (const section of this.ALL_SECTIONS) {
      for (const item of section.items) {
        if (seen.has(item.path)) continue;
        if (item.label.toLowerCase().includes(q) || section.title.toLowerCase().includes(q)) {
          seen.add(item.path);
          results.push({ label: item.label, section: section.title, icon: section.icon, path: item.path });
        }
        if (results.length >= 7) break;
      }
      if (results.length >= 7) break;
    }
    this.searchResults = results;
  }

  navigateToFirst(): void {
    if (this.searchResults.length > 0) {
      this.router.navigate([this.searchResults[0].path]);
      this.clearSearch();
    }
  }

  onResultClick(): void { this.clearSearch(); }

  clearSearch(): void {
    this.searchQuery   = '';
    this.searchResults = [];
    this.searchFocused = false;
  }

  onSearchBlur(): void {
    setTimeout(() => { this.searchFocused = false; this.searchResults = []; }, 160);
  }

  // ── Privados ──────────────────────────────────────────

  private loadTheme(): void {
    const saved = localStorage.getItem('theme');
    if (saved === 'dark') {
      this.isDarkMode = true;
      document.body.classList.add('dark-mode');
    }
  }

  private loadUserData(): void {
    const user = getSessionUser();
    if (!user) return;

    const fullName = [user['nombres'], user['apellidos']]
      .map(v => String(v ?? '').trim())
      .filter(v => v.length > 0)
      .join(' ');

    const backupName = String(user['username'] || user['usuarioLogin'] || 'Usuario');
    this.userName = fullName || backupName;

    const roles = getUserRoles();
    if (roles.length > 0) {
      this.userRole = roles.map(r => r.replace('ROLE_', '')).join(' | ');
    } else {
      this.userRole = String(user['rol'] ?? '').replace('ROLE_', '').trim() || 'Sistema';
    }
  }

  private buildMenuByRoles(): MenuSection[] {
    const userRoles = this.getNormalizedRoles();
    if (userRoles.length === 0) return [];

    return this.ALL_SECTIONS
      .filter(sec => !sec.roles || sec.roles.some(r => userRoles.includes(r)))
      .map(sec => ({
        ...sec,
        items: sec.items.filter(it => !it.roles || it.roles.some(r => userRoles.includes(r))),
      }))
      .filter(sec => sec.items.length > 0);
  }

  private getNormalizedRoles(): AppRole[] {
    const validRoles: AppRole[] = [
      'ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'ESTUDIANTE',
      'COORDINADOR', 'SECRETARIO', 'ABOGADO',
      'DIRECTOR_ADMINISTRATIVO', 'GESTOR_USUARIOS', 'ROL_REPORT'
    ];
    return getUserRoles()
      .map(r => r.replace('ROLE_', '') as AppRole)
      .filter(r => validRoles.includes(r));
  }

  private updateTitles(): void {
    const title = this.getDeepestTitle(this.route) ?? 'Dashboard';
    this.currentTitle = title;
    this.breadcrumb   = `Inicio / ${title}`;
  }

  private getDeepestTitle(route: ActivatedRoute): string | undefined {
    let current = route;
    while (current.firstChild) current = current.firstChild;
    return current.snapshot.data['title'] as string | undefined;
  }
}
