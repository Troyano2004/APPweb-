import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter, Subscription } from 'rxjs';

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

  private readonly subscriptions = new Subscription();

  menuSections: MenuSection[] = [
    {
      title: 'Dashboard',
      icon: '🏠',
      items: [{ label: 'Resumen general', path: '/dashboard' }]
    },
    {
      title: 'Catálogos Académicos',
      icon: '🎓',
      items: [
        { label: 'Universidad', path: '/catalogos/universidad' },
        { label: 'Facultad', path: '/catalogos/facultad' },
        { label: 'Carrera', path: '/catalogos/carrera' },
        { label: 'Modalidad Titulación', path: '/catalogos/modalidad' },
        { label: 'Período Académico', path: '/catalogos/periodo' },
        { label: 'Tipo Trabajo Titulación', path: '/catalogos/tipo-trabajo' },
        { label: 'Carrera-Modalidad', path: '/catalogos/carrera-modalidad' }
      ]
    },
    {
      title: 'Banco de Temas',
      icon: '📚',
      items: [
        { label: 'Listado de temas', path: '/temas' },
        { label: 'Registrar tema', path: '/temas/nuevo' },
        { label: 'Aprobación temas', path: '/temas/aprobacion' }
      ]
    },
    {
      title: 'Propuesta y Anteproyecto',
      icon: '📝',
      items: [
        { label: 'Propuestas pendientes', path: '/propuesta/pendientes' },
        { label: 'Registrar propuesta', path: '/propuesta/nueva' },
        { label: 'Revisión por director', path: '/propuesta/revision' },
        { label: 'Historial observaciones', path: '/propuesta/historial' }
      ]
    },
    {
      title: 'Tutorías',
      icon: '🤝',
      items: [
        { label: 'Registrar tutoría', path: '/tutorias/nueva' },
        { label: 'Actas de tutoría', path: '/tutorias/actas' },
        { label: 'Historial', path: '/tutorias/historial' }
      ]
    },
    {
      title: 'Proyecto de Titulación',
      icon: '📄',
      items: [
        { label: 'Documento por secciones', path: '/proyecto/documento' },
        { label: 'Revisión por secciones', path: '/proyecto/revision' },
        { label: 'Correcciones', path: '/proyecto/correcciones' },
        { label: 'Estado del proyecto', path: '/proyecto/estado' }
      ]
    },
    {
      title: 'Documentos',
      icon: '🗂️',
      items: [
        { label: 'Habilitantes', path: '/documentos/habilitantes' },
        { label: 'Versiones', path: '/documentos/versiones' },
        { label: 'Expediente', path: '/documentos/expediente' }
      ]
    },
    {
      title: 'Legalización',
      icon: '⚖️',
      items: [
        { label: 'Validación legal', path: '/legalizacion/validacion' },
        { label: 'Checklist', path: '/legalizacion/checklist' },
        { label: 'Aprobación final', path: '/legalizacion/aprobacion' }
      ]
    },
    {
      title: 'Reportes',
      icon: '📊',
      items: [
        { label: 'Expediente por estudiante', path: '/reportes/expediente' },
        { label: 'Por período', path: '/reportes/periodo' },
        { label: 'Actas y constancias', path: '/reportes/actas' }
      ]
    },
    {
      title: 'Administración',
      icon: '🛠️',
      items: [
        { label: 'Usuarios', path: '/admin/usuarios' },
        { label: 'Roles y permisos', path: '/admin/roles' },
        { label: 'Parámetros', path: '/admin/parametros' }
      ]
    },
    {
      title: 'Coordinación',
      icon: '🧭',
      items: [
        { label: 'Seguimiento de proyectos', path: '/coordinador/seguimiento' },
        { label: 'Control de directores', path: '/coordinador/directores' },
        { label: 'Validación administrativa', path: '/coordinador/validacion' },
        { label: 'Control de tutorías', path: '/coordinador/tutorias' },
        { label: 'Observaciones administrativas', path: '/coordinador/observaciones' },
        { label: 'Reportes', path: '/coordinador/reportes' },
        { label: 'Comisión formativa', path: '/coordinador/comision' }
      ]
    }
  ];

  constructor(private readonly router: Router, private readonly route: ActivatedRoute) {}

  ngOnInit(): void {
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
