import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Producto } from '../models/models';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProductoService {
  private readonly apiUrl = `${environment.apiUrl}/productos`;

  constructor(private readonly http: HttpClient) { }

  obtenerTodos(): Observable<Producto[]> {
    return this.http.get<Producto[]>(this.apiUrl, { withCredentials: true });
  }

  obtenerActivos(): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${this.apiUrl}/activos`, { withCredentials: true });
  }

  obtenerPorId(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  buscarPorNombre(nombre: string): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${this.apiUrl}/buscar?nombre=${nombre}`, { withCredentials: true });
  }

  obtenerBajoStock(): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${this.apiUrl}/bajo-stock`, { withCredentials: true });
  }

  crear(producto: Producto): Observable<Producto> {
    return this.http.post<Producto>(this.apiUrl, producto, { withCredentials: true });
  }

  crearConImagen(producto: Producto, archivo: File): Observable<Producto> {
    const formData = new FormData();
    formData.append('producto', new Blob([JSON.stringify(producto)], { type: 'application/json' }));
    formData.append('imagen', archivo);
    return this.http.post<Producto>(`${this.apiUrl}/con-imagen`, formData, { withCredentials: true });
  }

  actualizar(id: number, producto: Producto): Observable<Producto> {
    return this.http.put<Producto>(`${this.apiUrl}/${id}`, producto, { withCredentials: true });
  }

  actualizarConImagen(id: number, producto: Producto, archivo?: File): Observable<Producto> {
    const formData = new FormData();
    formData.append('producto', new Blob([JSON.stringify(producto)], { type: 'application/json' }));
    if (archivo) {
      formData.append('imagen', archivo);
    }
    return this.http.put<Producto>(`${this.apiUrl}/${id}/con-imagen`, formData, { withCredentials: true });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  actualizarStock(id: number, cantidad: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/stock?cantidad=${cantidad}`, {});
  }
}
