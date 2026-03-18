import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Comprobante, EmitirComprobanteDTO } from '../models/models';

@Injectable({ providedIn: 'root' })
export class FacturacionService {
  private apiUrl = `${environment.apiUrl}/facturacion`;

  constructor(private http: HttpClient) {}

  listarTodos(): Observable<Comprobante[]> {
    return this.http.get<Comprobante[]>(this.apiUrl, { withCredentials: true });
  }

  buscarPorVenta(ventaId: number): Observable<Comprobante> {
    return this.http.get<Comprobante>(`${this.apiUrl}/venta/${ventaId}`, { withCredentials: true });
  }

  emitir(dto: EmitirComprobanteDTO): Observable<Comprobante> {
    return this.http.post<Comprobante>(`${this.apiUrl}/emitir`, dto, { withCredentials: true });
  }
}
