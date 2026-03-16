export interface PeriodoSelect { idPeriodo: number; descripcion: string; fechaInicio: string; fechaFin: string; activo: boolean; }
export interface EstudianteSelect { idEstudiante: number; usuario: { nombres: string; apellidos: string; cedula: string; }; carrera?: { nombre: string }; }
