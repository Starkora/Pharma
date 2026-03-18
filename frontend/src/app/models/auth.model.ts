export interface Usuario {
  id?: number;
  username: string;
  password?: string;
  nombre: string;
  apellido: string;
  email: string;
  telefono?: string;
  activo: boolean;
  roles: string[];
  fechaCreacion?: Date;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  success: boolean;
  message: string;
  usuario?: Usuario;
}
