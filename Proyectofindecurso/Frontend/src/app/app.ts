import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class App implements OnInit {

  fechaActual: string = '';
  horaActual: string = '';

  constructor(private cd: ChangeDetectorRef) {}

  ngOnInit() {
    this.actualizarFechaHora();

    setInterval(() => {
      this.actualizarFechaHora();
      this.cd.detectChanges(); // 👈 fuerza actualización en pantalla
    }, 1000);
  }

  actualizarFechaHora() {
    const ahora = new Date();

    this.fechaActual = ahora.toLocaleDateString('es-EC', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });

    this.horaActual = ahora.toLocaleTimeString('es-EC', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }
}
