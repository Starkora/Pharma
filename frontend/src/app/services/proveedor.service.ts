import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Proveedor } from '../models/models';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProveedorService {
  private apiUrl = `${environment.apiUrl}/proveedores`;

  constructor(private http: HttpClient) { }

  obtenerTodos(): Observable<Proveedor[]> {
    return this.http.get<Proveedor[]>(this.apiUrl, { withCredentials: true });
  }

  obtenerActivos(): Observable<Proveedor[]> {
    return this.http.get<Proveedor[]>(`${this.apiUrl}/activos`, { withCredentials: true });
  }

  obtenerPorId(id: number): Observable<Proveedor> {
    return this.http.get<Proveedor>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  buscarPorNombre(nombre: string): Observable<Proveedor[]> {
    return this.http.get<Proveedor[]>(`${this.apiUrl}/buscar?nombre=${nombre}`, { withCredentials: true });
  }

  crear(proveedor: Proveedor): Observable<Proveedor> {
    return this.http.post<Proveedor>(this.apiUrl, proveedor, { withCredentials: true });
  }

  actualizar(id: number, proveedor: Proveedor): Observable<Proveedor> {
    return this.http.put<Proveedor>(`${this.apiUrl}/${id}`, proveedor, { withCredentials: true });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }
}
