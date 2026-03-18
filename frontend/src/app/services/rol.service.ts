import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Rol {
  id?: number;
  nombre: string;
  descripcion?: string;
}

@Injectable({ providedIn: 'root' })
export class RolService {
  private apiUrl = `${environment.apiUrl}/roles`;

  constructor(private http: HttpClient) {}

  obtenerTodos(): Observable<Rol[]> {
    return this.http.get<Rol[]>(this.apiUrl, { withCredentials: true });
  }

  crear(rol: Rol): Observable<Rol> {
    return this.http.post<Rol>(this.apiUrl, rol, { withCredentials: true });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }
}
