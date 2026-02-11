import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter, Subscription } from 'rxjs';
import { getSessionUser } from '../../services/session';

interface MenuItem {
  label: string;
  path: string;
}

interface MenuSection {
  title: string;
  icon: string;
  items: MenuItem[];
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss']
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

  menuSections: MenuSection[] = [
    {
      title: 'Dashboard',
      icon: '🏠',
      items: [{ label: 'Resumen general', path: '/app/dashboard' }]
    },
    {
      title: 'Catálogos Académicos',
      icon: '🎓',
      items: [
        { label: 'Universidad', path: '/app/catalogos/universidad' },
        { label: 'Facultad', path: '/app/catalogos/facultad' },
        { label: 'Carrera', path: '/app/catalogos/carrera' },
        { label: 'Modalidad Titulación', path: '/app/catalogos/modalidad' },
        { label: 'Período Académico', path: '/app/catalogos/periodo' },
        { label: 'Tipo Trabajo Titulación', path: '/app/catalogos/tipo-trabajo' },
        { label: 'Carrera-Modalidad', path: '/app/catalogos/carrera-modalidad' }
      ]
    },
    {
      title: 'Banco de Temas',
      icon: '📚',
      items: [
        { label: 'Listado de temas', path: '/app/temas' },
        { label: 'Registrar tema', path: '/app/temas/nuevo' },
        { label: 'Aprobación temas', path: '/app/temas/aprobacion' }
      ]
    },
    {
      title: 'Propuesta y Anteproyecto',
      icon: '📝',
      items: [
        { label: 'Propuestas pendientes', path: '/app/propuesta/pendientes' },
        { label: 'Registrar propuesta', path: '/app/propuesta/nueva' },
        { label: 'Revisión por director', path: '/app/propuesta/revision' },
        { label: 'Historial observaciones', path: '/app/propuesta/historial' }
      ]
    },
    {
      title: 'Tutorías',
      icon: '🤝',
      items: [
        { label: 'Registrar tutoría', path: '/app/tutorias/nueva' },
        { label: 'Actas de tutoría', path: '/app/tutorias/actas' },
        { label: 'Historial', path: '/app/tutorias/historial' }
      ]
    },
    {
      title: 'Proyecto de Titulación',
      icon: '📄',
      items: [
        { label: 'Documento por secciones', path: '/app/proyecto/documento' },
        { label: 'Revisión por secciones', path: '/app/proyecto/revision' },
        { label: 'Correcciones', path: '/app/proyecto/correcciones' },
        { label: 'Estado del proyecto', path: '/app/proyecto/estado' }
      ]
    },
    {
      title: 'Documentos',
      icon: '🗂️',
      items: [
        { label: 'Habilitantes', path: '/app/documentos/habilitantes' },
        { label: 'Versiones', path: '/app/documentos/versiones' },
        { label: 'Expediente', path: '/app/documentos/expediente' }
      ]
    },
    {
      title: 'Legalización',
      icon: '⚖️',
      items: [
        { label: 'Validación legal', path: '/app/legalizacion/validacion' },
        { label: 'Checklist', path: '/app/legalizacion/checklist' },
        { label: 'Aprobación final', path: '/app/legalizacion/aprobacion' }
      ]
    },
    {
      title: 'Reportes',
      icon: '📊',
      items: [
        { label: 'Expediente por estudiante', path: '/app/reportes/expediente' },
        { label: 'Por período', path: '/app/reportes/periodo' },
        { label: 'Actas y constancias', path: '/app/reportes/actas' }
      ]
    },
    {
      title: 'Administración del aplicativo',
      icon: '🛠️',
      items: [
        { label: 'Usuarios', path: '/app/admin/usuarios' },
        { label: 'Roles y permisos', path: '/app/admin/roles' },
        { label: 'Parámetros', path: '/app/admin/parametros' }
      ]
    },
    {
      title: 'Coordinación',
      icon: '🧭',
      items: [
        { label: 'Seguimiento de proyectos', path: '/app/coordinador/seguimiento' },
        { label: 'Control de directores', path: '/app/coordinador/directores' },
        { label: 'Validación administrativa', path: '/app/coordinador/validacion' },
        { label: 'Control de tutorías', path: '/app/coordinador/tutorias' },
        { label: 'Observaciones administrativas', path: '/app/coordinador/observaciones' },
        { label: 'Reportes', path: '/app/coordinador/reportes' },
        { label: 'Comisión formativa', path: '/app/coordinador/comision' }
      ]
    }
  ];

  constructor(private readonly router: Router, private readonly route: ActivatedRoute) {}

  ngOnInit(): void {
    this.loadUserData();
    this.updateTitles();
    const sub = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.updateTitles());
    this.subscriptions.add(sub);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  toggleMobile(): void {
    this.isMobileOpen = !this.isMobileOpen;
  }

  closeMobile(): void {
    this.isMobileOpen = false;
  }

  toggleSection(index: number): void {
    this.openSectionIndex = this.openSectionIndex === index ? -1 : index;
  }


  logout(): void {
    localStorage.removeItem('usuario');
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

  private loadUserData(): void {
    const user = getSessionUser();
    if (!user) {
      return;
    }

    const names = [user['nombres'], user['apellidos']]
      .map((value) => (value ?? '').toString().trim())
      .filter((value) => value.length > 0);

    const username = (user['username'] ?? user['usuarioLogin'] ?? '').toString().trim();
    this.userName = names.length ? names.join(' ') : (username || 'Usuario');

    const role = (user['rol'] ?? '').toString().replace('ROLE_', '').trim();
    this.userRole = role || 'Sistema';
  }

  private updateTitles(): void {
    const title = this.getDeepestTitle(this.route) ?? 'Dashboard';
    this.currentTitle = title;
    this.breadcrumb = `Inicio / ${title}`;
  }

  private getDeepestTitle(route: ActivatedRoute): string | undefined {
    let current = route;
    while (current.firstChild) {
      current = current.firstChild;
    }
    return current.snapshot.data['title'] as string | undefined;
  }
}
