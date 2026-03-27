import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { RecoveryFabComponent } from './shared/recovery-fab/recovery-fab.component';


@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RecoveryFabComponent],
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class App {}
