import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Venta } from '../models/models';
import { environment } from '../../environments/environment';

interface VentaRequestPayload {
  cliente?: { id: number } | null;
  descuento: number;
  metodoPago?: string;
  observaciones?: string;
  subtotal: number;
  impuesto: number;
  total: number;
  detalles: Array<{
    producto: { id: number };
    cantidad: number;
    descuento: number;
    precioUnitario: number;
    subtotal: number;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class VentaService {
  private apiUrl = `${environment.apiUrl}/ventas`;

  constructor(private http: HttpClient) { }

  obtenerTodas(): Observable<Venta[]> {
    return this.http.get<Venta[]>(this.apiUrl, { withCredentials: true });
  }

  obtenerPorId(id: number): Observable<Venta> {
    return this.http.get<Venta>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  obtenerPorCliente(clienteId: number): Observable<Venta[]> {
    return this.http.get<Venta[]>(`${this.apiUrl}/cliente/${clienteId}`, { withCredentials: true });
  }

  obtenerPorPeriodo(fechaInicio: string, fechaFin: string): Observable<Venta[]> {
    return this.http.get<Venta[]>(
      `${this.apiUrl}/periodo?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`, { withCredentials: true }
    );
  }

  calcularTotal(fechaInicio: string, fechaFin: string): Observable<number> {
    return this.http.get<number>(
      `${this.apiUrl}/total?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`, { withCredentials: true }
    );
  }

  crear(venta: Venta): Observable<Venta> {
    const payload: VentaRequestPayload = {
      cliente: venta.cliente?.id ? { id: venta.cliente.id } : null,
      descuento: venta.descuento ?? 0,
      metodoPago: venta.metodoPago,
      observaciones: venta.observaciones,
      subtotal: venta.subtotal,
      impuesto: venta.impuesto,
      total: venta.total,
      detalles: venta.detalles.map(detalle => ({
        producto: { id: detalle.producto.id! },
        cantidad: detalle.cantidad,
        descuento: detalle.descuento ?? 0,
        precioUnitario: detalle.precioUnitario,
        subtotal: detalle.subtotal
      }))
    };

    return this.http.post<Venta>(this.apiUrl, payload, { withCredentials: true });
  }

  cancelar(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/cancelar`, {}, { withCredentials: true });
  }
}
