import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, forwardRef, Input, Output, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'app-rich-text-editor',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="editor-wrapper" [class.editor-disabled]="disabled">
      <div class="editor-toolbar">
        <button type="button" class="tool-btn" (click)="exec('bold')" [disabled]="disabled"><b>B</b></button>
        <button type="button" class="tool-btn" (click)="exec('italic')" [disabled]="disabled"><i>I</i></button>
        <button type="button" class="tool-btn" (click)="exec('underline')" [disabled]="disabled"><u>U</u></button>
        <button type="button" class="tool-btn" (click)="exec('insertUnorderedList')" [disabled]="disabled">• Lista</button>
        <button type="button" class="tool-btn" (click)="exec('insertOrderedList')" [disabled]="disabled">1. Lista</button>
        <button type="button" class="tool-btn" (click)="insertLink()" [disabled]="disabled">🔗 Enlace</button>
        <button type="button" class="tool-btn" (click)="insertTable()" [disabled]="disabled">▦ Tabla</button>
        <button type="button" class="tool-btn" (click)="triggerImageInput()" [disabled]="disabled">🖼 Imagen</button>
        <button type="button" class="tool-btn" (click)="clearFormat()" [disabled]="disabled">Limpiar</button>
      </div>

      <input #imageInput type="file" accept="image/*" hidden (change)="onImageSelected($event)" />

      <div
        #editor
        class="editor-content"
        [attr.data-placeholder]="placeholder"
        [attr.contenteditable]="!disabled"
        (input)="onEditorChange()"
        (blur)="onTouched(); onEditorChange()"
      ></div>
    </div>
  `,
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
  private innerValue = '';

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

    let html = '<table><tbody>';
    for (let r = 0; r < rows; r++) {
      html += '<tr>';
      for (let c = 0; c < cols; c++) html += '<td> </td>';
      html += '</tr>';
    }
    html += '</tbody></table><p></p>';

    this.editorRef.nativeElement.focus();
    document.execCommand('insertHTML', false, html);
    this.onEditorChange();
  }

  triggerImageInput(): void {
    this.imageInputRef.nativeElement.click();
  }

  onImageSelected(event: Event): void {
    if (this.disabled) return;
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      this.editorRef.nativeElement.focus();
      document.execCommand('insertImage', false, result);
      this.onEditorChange();
      input.value = '';
    };
    reader.readAsDataURL(file);
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
