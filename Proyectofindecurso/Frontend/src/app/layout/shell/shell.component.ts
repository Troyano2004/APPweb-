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
import { filter, Subscription } from 'rxjs';
import { getSessionUser } from '../../services/session';

// ✅ Agregado COORDINADOR al type
type AppRole = 'ADMIN' | 'DOCENTE' | 'ESTUDIANTE' | 'COORDINADOR';

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

  private readonly subscriptions = new Subscription();

  // ✅ MENÚ MAESTRO ACTUALIZADO CON TODO LO DE LA VERSIÓN OFICIAL
  private readonly ALL_SECTIONS: MenuSection[] = [
    {
      title: 'Dashboard',
      icon: '🏠',
      roles: ['ADMIN', 'DOCENTE', 'ESTUDIANTE', 'COORDINADOR'],
      items: [{ label: 'Resumen general', path: '/app/dashboard' }],
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
      roles: ['DOCENTE', 'ADMIN', 'COORDINADOR'],
      items: [
        { label: 'Listado de temas', path: '/app/temas' },
        { label: 'Registrar tema', path: '/app/temas/nuevo' },
        { label: 'Aprobación temas', path: '/app/temas/aprobacion' },
      ],
    },
    {
      title: 'Tutorías y Dirección',
      icon: '👨‍🏫',
      roles: ['DOCENTE', 'ADMIN'],
      items: [
        { label: 'Mis anteproyectos', path: '/app/director/mis-anteproyectos' },
        { label: 'Tutorías', path: '/app/director/tutorias' },
        { label: 'Acta de revisión', path: '/app/director/acta' },
        { label: 'Revisión Final Anteproyecto', path: '/app/dt1/lista' },
      ],
    },
    {
      title: 'Titulación II',
      icon: '🧐',
      roles: ['DOCENTE', 'ESTUDIANTE', 'ADMIN'],
      items: [
        { label: 'Documento de titulación', path: '/app/titulacion2/documento', roles: ['ESTUDIANTE'] },
        { label: 'Documentos pendientes', path: '/app/titulacion2/revision', roles: ['DOCENTE'] },
        { label: 'Workflow Proceso', path: '/app/titulacion2/workflow', roles: ['ADMIN', 'DOCENTE'] },
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
      title: 'Propuesta y Anteproyecto',
      icon: '📝',
      roles: ['ESTUDIANTE'],
      items: [
        { label: 'Registrar propuesta', path: '/app/propuesta/nueva' },
        { label: 'Registrar anteproyecto', path: '/app/anteproyecto/nuevo' },
        { label: 'Historial observaciones', path: '/app/tutorias/historial' },
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

  // ✅ MÉTODOS DE UI
  toggleCollapse() { this.isCollapsed = !this.isCollapsed; }
  toggleMobile() { this.isMobileOpen = !this.isMobileOpen; }
  closeMobile() { this.isMobileOpen = false; }
  toggleSection(index: number) { this.openSectionIndex = this.openSectionIndex === index ? -1 : index; }

  logout(): void {
    localStorage.removeItem('usuario');
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

  private loadUserData(): void {
    const user = getSessionUser();
    if (!user) return;

    // 1. Construir nombre completo (Limpiando nulos y espacios)
    const fullName = [user['nombres'], user['apellidos']]
      .map(v => String(v ?? '').trim()) // String() es más seguro que .toString()
      .filter(v => v.length > 0)
      .join(' ');

    // 2. Fallback al username (Forzando que sea string para evitar el error TS2322)
    const backupName = String(user['username'] || user['usuarioLogin'] || 'Usuario');

    this.userName = fullName || backupName;

    // 3. Formatear el rol
    this.userRole = String(user['rol'] ?? '').replace('ROLE_', '').trim() || 'Sistema';
  }

  // ✅ LÓGICA DE FILTRADO POR ROL
  private buildMenuByRole(): MenuSection[] {
    const role = this.getNormalizedRole();
    if (!role) return [];

    return this.ALL_SECTIONS
      .filter((sec) => !sec.roles || sec.roles.includes(role))
      .map((sec) => ({
        ...sec,
        // Filtra también los items individuales si tienen la propiedad roles
        items: sec.items.filter((it) => !it.roles || it.roles.includes(role)),
      }))
      .filter((sec) => sec.items.length > 0);
  }

  private getNormalizedRole(): AppRole | null {
    const user = getSessionUser();
    const raw = String(user?.['rol'] ?? '').trim().toUpperCase();
    const clean = raw.replace('ROLE_', '');

    if (clean === 'ADMIN') return 'ADMIN';
    if (clean === 'DOCENTE') return 'DOCENTE';
    if (clean === 'ESTUDIANTE') return 'ESTUDIANTE';
    if (clean === 'COORDINADOR') return 'COORDINADOR'; // ✅ Agregado
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
