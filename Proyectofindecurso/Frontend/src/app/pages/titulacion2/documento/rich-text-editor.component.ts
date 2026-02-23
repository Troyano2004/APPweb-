import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, forwardRef, Input, Output, ViewChild } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'app-rich-text-editor',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rich-text-editor.component.html',
  styleUrls: ['./rich-text-editor.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RichTextEditorComponent),
      multi: true
    }
  ]
})
export class RichTextEditorComponent implements ControlValueAccessor {
  @Input() placeholder = 'Escribe aquí...';
  @Output() valueChange = new EventEmitter<string>();

  @ViewChild('editor', { static: true }) editorRef!: ElementRef<HTMLDivElement>;
  @ViewChild('imageInput', { static: true }) imageInputRef!: ElementRef<HTMLInputElement>;

  disabled = false;
  private readonly API_URL = 'http://localhost:8080';
  private innerValue = '';

  constructor(private http: HttpClient) {}

  private onChange: (value: string) => void = () => {};
  onTouched: () => void = () => {};

  writeValue(value: string | null): void {
    this.innerValue = value ?? '';
    if (this.editorRef) this.editorRef.nativeElement.innerHTML = this.innerValue;
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  exec(command: string): void {
    if (this.disabled) return;
    this.editorRef.nativeElement.focus();
    document.execCommand(command, false);
    this.onEditorChange();
  }

  insertLink(): void {
    if (this.disabled) return;
    const url = window.prompt('Ingrese la URL del enlace:');
    if (!url) return;
    this.editorRef.nativeElement.focus();
    document.execCommand('createLink', false, url);
    this.onEditorChange();
  }

  insertTable(): void {
    if (this.disabled) return;
    const rows = Number(window.prompt('Número de filas:', '2') || 0);
    const cols = Number(window.prompt('Número de columnas:', '2') || 0);
    if (!rows || !cols || rows < 1 || cols < 1) return;

    const tableWidth = this.normalizeTableSize(window.prompt('Ancho de la tabla (ej: 100%, 600px):', '100%'), '100%');
    const cellWidth = this.normalizeTableSize(window.prompt('Ancho de cada columna (opcional, ej: 180px o 25%):', ''));

    let html = `<table style="width:${tableWidth}; border-collapse: collapse; border: 1px solid #9ca3af; margin: 0.5rem 0; display: block; overflow: auto; resize: horizontal; min-width: 240px; max-width: 100%;">`;
    if (cellWidth) {
      html += '<colgroup>';
      for (let c = 0; c < cols; c++) {
        html += `<col style="width:${cellWidth};">`;
      }
      html += '</colgroup>';
    }

    html += '<tbody>';
    for (let r = 0; r < rows; r++) {
      html += '<tr>';
      for (let c = 0; c < cols; c++) html += '<td style="border: 1px solid #9ca3af; padding: 0.35rem; min-width: 2.5rem;"> </td>';
      html += '</tr>';
    }
    html += '</tbody></table><p></p>';

    this.editorRef.nativeElement.focus();
    document.execCommand('insertHTML', false, html);
    this.onEditorChange();
  }

  private normalizeTableSize(value: string | null, fallback = ''): string {
    const raw = (value ?? '').trim();
    if (!raw) return fallback;

    const sizePattern = /^\d+(\.\d+)?(px|%|rem|em|vw|vh)?$/i;
    if (!sizePattern.test(raw)) return fallback;

    return /^\d+(\.\d+)?$/i.test(raw) ? `${raw}px` : raw;
  }

  triggerImageInput(): void {
    this.imageInputRef.nativeElement.click();
  }

  onImageSelected(event: Event): void {
    if (this.disabled) return;
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    this.http.post<{ url: string }>(`${this.API_URL}/api/uploads/images`, formData).subscribe({
      next: (response) => {
        this.editorRef.nativeElement.focus();
        document.execCommand('insertImage', false, response.url);
        this.onEditorChange();
        input.value = '';
      },
      error: (err) => {
        const message = err?.error?.message ?? 'No se pudo subir la imagen';
        window.alert(message);
        input.value = '';
      }
    });
  }

  clearFormat(): void {
    if (this.disabled) return;
    this.editorRef.nativeElement.focus();
    document.execCommand('removeFormat');
    this.onEditorChange();
  }

  onEditorChange(): void {
    const value = this.editorRef.nativeElement.innerHTML;
    this.innerValue = value;
    this.onChange(value);
    this.valueChange.emit(value);
  }
}
