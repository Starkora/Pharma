import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Compra } from '../models/models';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CompraService {
  private apiUrl = `${environment.apiUrl}/compras`;

  constructor(private http: HttpClient) { }

  obtenerTodas(): Observable<Compra[]> {
    return this.http.get<Compra[]>(this.apiUrl, { withCredentials: true });
  }

  obtenerPorId(id: number): Observable<Compra> {
    return this.http.get<Compra>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  obtenerPorProveedor(proveedorId: number): Observable<Compra[]> {
    return this.http.get<Compra[]>(`${this.apiUrl}/proveedor/${proveedorId}`, { withCredentials: true });
  }

  obtenerPorPeriodo(fechaInicio: string, fechaFin: string): Observable<Compra[]> {
    return this.http.get<Compra[]>(
      `${this.apiUrl}/periodo?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`, { withCredentials: true }
    );
  }

  calcularTotal(fechaInicio: string, fechaFin: string): Observable<number> {
    return this.http.get<number>(
      `${this.apiUrl}/total?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`, { withCredentials: true }
    );
  }

  crear(compra: Compra): Observable<Compra> {
    return this.http.post<Compra>(this.apiUrl, compra, { withCredentials: true });
  }

  cancelar(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/cancelar`, {}, { withCredentials: true });
  }

  recibir(id: number): Observable<Compra> {
    return this.http.put<Compra>(`${this.apiUrl}/${id}/recibir`, {}, { withCredentials: true });
  }
}
