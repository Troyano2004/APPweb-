import { Component, OnDestroy, OnInit, HostListener } from '@angular/core';
import {
  ActivatedRoute,
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter, Subscription } from 'rxjs';
import {
  getSessionUser,
  getAvailableRoles,
  getActiveRole,
  setActiveRole,
  getRoleHomeRoute,
} from '../../services/session';

type AppRole =
  | 'ADMIN'
  | 'DOCENTE'
  | 'ESTUDIANTE'
  | 'COORDINADOR'
  | 'DIRECTOR'
  | 'TRIBUNAL'
  | 'COMISION_FORMATIVA';

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

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss'],
})
export class ShellComponent implements OnInit, OnDestroy {
  isCollapsed = false;
  isMobileOpen = false;
  openSectionIndex = 0;
  currentTitle = 'Dashboard';
  breadcrumb = 'Inicio / Dashboard';
  userName = 'Usuario';
  userRole = 'Sistema';
  userInitials = 'U';
  isProfileOpen = false;
  availableRoles: string[] = [];
  activeRole = '';

  private readonly subscriptions = new Subscription();

  private readonly ALL_SECTIONS: MenuSection[] = [
    {
      title: 'Dashboard',
      icon: '🏠',
      roles: ['ADMIN', 'DOCENTE', 'ESTUDIANTE', 'COORDINADOR', 'DIRECTOR', 'TRIBUNAL', 'COMISION_FORMATIVA'],
      items: [
        { label: 'Resumen general', path: '/app/dashboard', roles: ['ADMIN', 'COORDINADOR', 'COMISION_FORMATIVA'] },
        { label: 'Mi panel docente', path: '/app/dashboard', roles: ['DOCENTE'] },
        { label: 'Mi panel director', path: '/app/dashboard', roles: ['DIRECTOR'] },
        { label: 'Mi panel tribunal', path: '/app/dashboard', roles: ['TRIBUNAL'] },
        { label: 'Mi panel', path: '/app/dashboard', roles: ['ESTUDIANTE'] },
      ],
    },
    {
      title: 'Catálogos Académicos',
      icon: '🎓',
      roles: ['ADMIN', 'COORDINADOR'],
      items: [
        { label: 'Universidad', path: '/app/catalogos/universidad' },
        { label: 'Facultad', path: '/app/catalogos/facultad' },
        { label: 'Carrera', path: '/app/catalogos/carrera' },
        { label: 'Modalidad Titulación', path: '/app/catalogos/modalidad' },
        { label: 'Período Académico', path: '/app/catalogos/periodo' },
        { label: 'Tipo Trabajo Titulación', path: '/app/catalogos/tipo-trabajo' },
        { label: 'Carrera-Modalidad', path: '/app/catalogos/carrera-modalidad' },
      ],
    },
    {
      title: 'Banco de Temas',
      icon: '📚',
      roles: ['DOCENTE', 'ADMIN', 'COORDINADOR', 'DIRECTOR'],
      items: [
        { label: 'Listado de temas', path: '/app/temas' },
        { label: 'Registrar tema', path: '/app/temas/nuevo', roles: ['DOCENTE', 'ADMIN', 'DIRECTOR'] },
        { label: 'Aprobación temas', path: '/app/temas/aprobacion', roles: ['ADMIN', 'COORDINADOR'] },
      ],
    },
    {
      title: 'Propuesta y Anteproyecto',
      icon: '📝',
      roles: ['ADMIN', 'DOCENTE', 'ESTUDIANTE', 'DIRECTOR'],
      items: [
        { label: 'Propuestas pendientes', path: '/app/propuesta/pendientes', roles: ['ADMIN', 'DOCENTE', 'DIRECTOR'] },
        { label: 'Registrar propuesta', path: '/app/propuesta/nueva', roles: ['ESTUDIANTE', 'ADMIN'] },
        { label: 'Registrar anteproyecto', path: '/app/anteproyecto/nuevo', roles: ['ESTUDIANTE', 'ADMIN'] },
        { label: 'Revisión por director', path: '/app/propuesta/revision', roles: ['DOCENTE', 'ADMIN', 'DIRECTOR'] },
        { label: 'Historial observaciones', path: '/app/tutorias/historial', roles: ['ESTUDIANTE', 'ADMIN'] },
        { label: 'Historial observaciones', path: '/app/propuesta/historial', roles: ['DOCENTE', 'DIRECTOR'] },
      ],
    },
    {
      title: 'Tutorías y Dirección',
      icon: '👨‍🏫',
      roles: ['DOCENTE', 'ADMIN', 'DIRECTOR'],
      items: [
        { label: 'Mis anteproyectos', path: '/app/director/mis-anteproyectos' },
        { label: 'Tutorías', path: '/app/director/tutorias' },
        { label: 'Acta de revisión', path: '/app/director/acta' },
        { label: 'Revisión Final Anteproyecto', path: '/app/dt1/lista' },
        { label: 'Registrar tutoría', path: '/app/tutorias/nueva' },
        { label: 'Actas de tutoría', path: '/app/tutorias/actas' },
        { label: 'Historial', path: '/app/tutorias/historial', roles: ['ADMIN'] },
      ],
    },
    {
      title: 'Proyecto de Titulación',
      icon: '📄',
      roles: ['ADMIN', 'DOCENTE', 'DIRECTOR'],
      items: [
        { label: 'Documento por secciones', path: '/app/proyecto/documento' },
        { label: 'Revisión por secciones', path: '/app/proyecto/revision' },
        { label: 'Correcciones', path: '/app/proyecto/correcciones' },
        { label: 'Estado del proyecto', path: '/app/proyecto/estado' },
      ],
    },
    {
      title: 'Titulación II',
      icon: '🧐',
      roles: ['DOCENTE', 'ESTUDIANTE', 'ADMIN', 'DIRECTOR', 'TRIBUNAL'],
      items: [
        { label: 'Documento de titulación', path: '/app/titulacion2/documento', roles: ['ESTUDIANTE'] },
        { label: 'Documentos pendientes', path: '/app/titulacion2/revision', roles: ['DOCENTE', 'DIRECTOR'] },
        { label: 'Evaluación de sustentación', path: '/app/titulacion2/evaluacion', roles: ['TRIBUNAL'] },
        { label: 'Acta de grado', path: '/app/titulacion2/acta-grado', roles: ['TRIBUNAL', 'ADMIN'] },
        { label: 'Workflow Proceso', path: '/app/titulacion2/workflow', roles: ['ADMIN', 'DOCENTE', 'DIRECTOR'] },
      ],
    },
    {
      title: 'Tribunal',
      icon: '⚖️',
      roles: ['TRIBUNAL', 'ADMIN'],
      items: [
        { label: 'Proyectos asignados', path: '/app/tribunal/proyectos' },
        { label: 'Evaluaciones pendientes', path: '/app/tribunal/evaluaciones' },
        { label: 'Actas de defensa', path: '/app/tribunal/actas' },
        { label: 'Calendario de defensas', path: '/app/tribunal/calendario' },
      ],
    },
    {
      title: 'Documentos',
      icon: '🗂️',
      roles: ['ADMIN', 'DOCENTE', 'ESTUDIANTE', 'DIRECTOR', 'TRIBUNAL'],
      items: [
        { label: 'Habilitantes', path: '/app/documentos/habilitantes' },
        { label: 'Versiones', path: '/app/documentos/versiones' },
        { label: 'Expediente', path: '/app/documentos/expediente' },
      ],
    },
    {
      title: 'Legalización',
      icon: '📜',
      roles: ['ADMIN', 'COORDINADOR', 'COMISION_FORMATIVA'],
      items: [
        { label: 'Validación legal', path: '/app/legalizacion/validacion' },
        { label: 'Checklist', path: '/app/legalizacion/checklist' },
        { label: 'Aprobación final', path: '/app/legalizacion/aprobacion' },
      ],
    },
    {
      title: 'Reportes',
      icon: '📊',
      roles: ['ADMIN', 'DOCENTE', 'COORDINADOR', 'DIRECTOR', 'COMISION_FORMATIVA'],
      items: [
        { label: 'Expediente por estudiante', path: '/app/reportes/expediente' },
        { label: 'Por período', path: '/app/reportes/periodo' },
        { label: 'Actas y constancias', path: '/app/reportes/actas' },
      ],
    },
    {
      title: 'Coordinación',
      icon: '🧭',
      roles: ['COORDINADOR', 'ADMIN'],
      items: [
        { label: 'Seguimiento de proyectos', path: '/app/coordinador/seguimiento' },
        { label: 'Control de directores', path: '/app/coordinador/directores' },
        { label: 'Validación administrativa', path: '/app/coordinador/validacion' },
        { label: 'Control de tutorías', path: '/app/coordinador/tutorias' },
        { label: 'Observaciones administrativas', path: '/app/coordinador/observaciones' },
        { label: 'Workflow Titulación II', path: '/app/titulacion2/workflow' },
        { label: 'Reportes', path: '/app/coordinador/reportes' },
        { label: 'Comisión formativa', path: '/app/coordinador/comision' },
        { label: 'DT1 - Docentes y Tutores', path: '/app/coordinador/dt1-asignacion' },
      ],
    },
    {
      title: 'Comisión Formativa',
      icon: '👥',
      roles: ['COMISION_FORMATIVA', 'ADMIN'],
      items: [
        { label: 'Proyectos en revisión', path: '/app/comision/proyectos' },
        { label: 'Aprobaciones pendientes', path: '/app/comision/aprobaciones' },
        { label: 'Dictámenes', path: '/app/comision/dictamenes' },
        { label: 'Actas de comisión', path: '/app/comision/actas' },
        { label: 'Resoluciones', path: '/app/comision/resoluciones' },
      ],
    },
    {
      title: 'Administración del aplicativo',
      icon: '🛠️',
      roles: ['ADMIN', 'COORDINADOR'],
      items: [
        { label: 'Usuarios', path: '/app/admin/usuarios' },
        { label: 'Roles y permisos', path: '/app/admin/roles' },
        { label: 'Parámetros', path: '/app/admin/parametros' },
      ],
    },
  ];

  menuSections: MenuSection[] = [];

  constructor(private readonly router: Router, private readonly route: ActivatedRoute) {}

  ngOnInit(): void {
    this.loadUserData();
    this.menuSections = this.buildMenuByRole();
    this.openSectionIndex = this.menuSections.length ? 0 : -1;
    this.updateTitles();

    const sub = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.updateTitles());
    this.subscriptions.add(sub);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.profile-dropdown-wrapper')) {
      this.isProfileOpen = false;
    }
  }

  toggleCollapse(): void { this.isCollapsed = !this.isCollapsed; }
  toggleMobile(): void { this.isMobileOpen = !this.isMobileOpen; }
  closeMobile(): void { this.isMobileOpen = false; }
  toggleSection(index: number): void {
    this.openSectionIndex = this.openSectionIndex === index ? -1 : index;
  }

  toggleProfileDropdown(): void {
    this.isProfileOpen = !this.isProfileOpen;
  }

  switchRole(role: string): void {
    if (role === this.activeRole) {
      this.isProfileOpen = false;
      return;
    }

    setActiveRole(role);
    this.activeRole = role;
    this.userRole = this.formatRoleLabel(role);
    this.menuSections = this.buildMenuByRole();
    this.openSectionIndex = this.menuSections.length ? 0 : -1;
    this.isProfileOpen = false;

    const homeRoute = getRoleHomeRoute(role);
    if (homeRoute) {
      this.router.navigate([homeRoute]);
    }
  }

  logout(): void {
    localStorage.removeItem('usuario');
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

  formatRoleLabel(role: string): string {
    const clean = String(role).replace('ROLE_', '').trim();
    const labels: Record<string, string> = {
      ADMIN: 'Administrador',
      DOCENTE: 'Docente',
      ESTUDIANTE: 'Estudiante',
      COORDINADOR: 'Coordinador',
      DIRECTOR: 'Director',
      TRIBUNAL: 'Tribunal',
      COMISION_FORMATIVA: 'Comisión Formativa',
    };
    return labels[clean] ?? clean;
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

    this.activeRole = getActiveRole();
    this.userRole = this.formatRoleLabel(this.activeRole);
    this.availableRoles = getAvailableRoles();
    this.userInitials = this.buildInitials(this.userName);
  }

  private buildInitials(name: string): string {
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  }

  private buildMenuByRole(): MenuSection[] {
    const role = this.getNormalizedRole();
    if (!role) return [];

    return this.ALL_SECTIONS
      .filter(sec => !sec.roles || sec.roles.includes(role))
      .map(sec => ({
        ...sec,
        items: sec.items.filter(it => !it.roles || it.roles.includes(role)),
      }))
      .filter(sec => sec.items.length > 0);
  }

  private getNormalizedRole(): AppRole | null {
    const raw = String(this.activeRole || '').trim().toUpperCase();
    const clean = raw.replace('ROLE_', '');

    if (clean === 'ADMIN') return 'ADMIN';
    if (clean === 'DOCENTE') return 'DOCENTE';
    if (clean === 'ESTUDIANTE') return 'ESTUDIANTE';
    if (clean === 'COORDINADOR') return 'COORDINADOR';
    if (clean === 'DIRECTOR') return 'DIRECTOR';
    if (clean === 'TRIBUNAL') return 'TRIBUNAL';
    if (clean === 'COMISION_FORMATIVA') return 'COMISION_FORMATIVA';
    return null;
  }

  private updateTitles(): void {
    const title = this.getDeepestTitle(this.route) ?? 'Dashboard';
    this.currentTitle = title;
    this.breadcrumb = `Inicio / ${title}`;
  }

  private getDeepestTitle(route: ActivatedRoute): string | undefined {
    let current = route;
    while (current.firstChild) current = current.firstChild;
    return current.snapshot.data['title'] as string | undefined;
  }
}
