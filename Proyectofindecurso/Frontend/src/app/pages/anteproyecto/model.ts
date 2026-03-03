export interface PropuestaSnapshot {
  idPropuesta?: number;
  idTema?: number;
  tituloTema?: string;

  titulo?: string;
  temaInvestigacion?: string;
  planteamientoProblema?: string;
  objetivoGeneral?: string;
  objetivosEspecificos?: string;
  metodologia?: string;
  bibliografia?: string;
}

export interface Anteproyecto {
  idAnteproyecto: number | null;
  estado: string;
  idEstudiante: number;
  nombresEstudiante?: string;
  apellidosEstudiante?: string;
  mensaje?: string;

  ultimaVersion?: AnteproyectoVersion;
  propuesta?: PropuestaSnapshot;
}


export interface AnteproyectoVersion {
  idVersion: number;
  fechaCreacion?: string; // ISO string (desde backend)
  comentarioCambio?: string;

  titulo?: string;
  temaInvestigacion?: string;
  planteamientoProblema?: string;
  objetivosGenerales?: string;
  objetivosEspecificos?: string;
  marcoTeorico?: string;
  metodologia?: string;
  resultadosEsperados?: string;
  bibliografia?: string;

}

export type AnteproyectoVersionRequest = Omit<AnteproyectoVersion, 'idVersion' | 'fechaCreacion'>;
