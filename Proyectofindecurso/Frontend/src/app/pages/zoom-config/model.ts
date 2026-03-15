export interface ZoomConfigDto {
  id?: number;
  accountId: string;
  clientId: string;
  clientSecret: string;
  configurado: boolean;
}

export interface ZoomConfigRequest {
  accountId: string;
  clientId: string;
  clientSecret: string;
}
