export interface ConfiguracionCorreoDto {
  id?: number;
  proveedor: string;
  usuario: string;
  password?: string;
  activo?: boolean;
  clientId?: string;
  clientSecret?: string;
  autorizado?: boolean;
}
export const PROVEEDORES = [
  { value: 'GMAIL',   label: 'Gmail',   icon: '📧' },
  { value: 'YAHOO',   label: 'Yahoo',   icon: '📨' },
  { value: 'OUTLOOK', label: 'Outlook', icon: '📩' }
];
