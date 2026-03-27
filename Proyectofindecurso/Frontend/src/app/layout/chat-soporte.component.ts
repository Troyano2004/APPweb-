import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-chat-soporte',
  standalone: true,
  imports: [CommonModule],
  template: `<!-- Botpress chat widget -->`,
})
export class ChatSoporteComponent implements OnInit, OnDestroy {

  ngOnInit(): void {
    this.cargarBotpress();
  }

  ngOnDestroy(): void {
    document.getElementById('bp-script-inject')?.remove();
    document.getElementById('bp-script-config')?.remove();
  }

  private cargarBotpress(): void {
    if (document.getElementById('bp-script-inject')) return;

    const s1 = document.createElement('script');
    s1.id  = 'bp-script-inject';
    s1.src = 'https://cdn.botpress.cloud/webchat/v3.6/inject.js';

    s1.onload = () => {
      const s2 = document.createElement('script');
      s2.id    = 'bp-script-config';
      s2.src   = 'https://files.bpcontent.cloud/2026/03/27/02/20260327023535-ZTI7QQ00.js';
      s2.defer = true;
      document.body.appendChild(s2);
    };

    document.body.appendChild(s1);
  }
}
