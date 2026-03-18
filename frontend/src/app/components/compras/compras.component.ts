import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CompraService } from '../../services/compra.service';
import { ProductoService } from '../../services/producto.service';
import { ProveedorService } from '../../services/proveedor.service';
import { AppLogger } from '../../services/app-logger.service';
import { Compra, DetalleCompra, Producto, Proveedor } from '../../models/models';
import { SolesPipe } from '../../pipes/soles.pipe';
import { of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-compras',
  standalone: true,
  imports: [CommonModule, FormsModule, SolesPipe],
  template: `
    <div class="card">
      <div class="card-header">
        <h2><i class="fas fa-shopping-cart"></i> Gestión de Compras</h2>
        <button class="btn btn-primary" (click)="abrirNuevo()">
          <i class="fas fa-plus"></i> Nueva Compra
        </button>
      </div>
      
      <!-- Modal Nueva Compra -->
      <div class="modal-overlay" *ngIf="mostrarFormulario" (click)="cerrarAlHacerClick($event)">
        <div class="modal-container modal-lg">
          <div class="modal-header">
            <h3><i class="fas fa-file-invoice"></i> Nueva Compra</h3>
            <button class="btn-close-modal" (click)="cancelarModal()" title="Cerrar">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="modal-body">
            <div class="grid grid-2">
              <div class="form-group">
                <label>Proveedor *</label>
                <select class="form-control" [(ngModel)]="compraActual.proveedor" required>
                  <option [ngValue]="null">Seleccione un proveedor...</option>
                  <option *ngFor="let prov of proveedores" [ngValue]="prov">
                    {{ prov.nombre }} {{ prov.ruc ? '- RUC: ' + prov.ruc : '' }}
                  </option>
                </select>
              </div>
              <div class="form-group">
                <label>Estado</label>
                <select class="form-control" [(ngModel)]="compraActual.estado">
                  <option value="PENDIENTE">Pendiente</option>
                  <option value="RECIBIDA">Recibida</option>
                </select>
              </div>
            </div>

            <div class="form-group">
              <label>Observaciones</label>
              <textarea class="form-control" [(ngModel)]="compraActual.observaciones" rows="2"></textarea>
            </div>

            <h4 class="mt-2"><i class="fas fa-box"></i> Agregar Productos</h4>
            <div class="grid grid-3">
              <div class="form-group">
                <label>Producto</label>
                <select class="form-control" [(ngModel)]="productoTemp">
                  <option [ngValue]="null">Seleccione...</option>
                  <option *ngFor="let prod of productos" [ngValue]="prod">
                    {{ prod.nombre }} - Stock actual: {{ prod.stock }}
                  </option>
                </select>
              </div>
              <div class="form-group">
                <label>Cantidad</label>
                <input type="number" class="form-control" [(ngModel)]="cantidadTemp" min="1" placeholder="0">
              </div>
              <div class="form-group">
                <label>Precio Compra</label>
                <div style="display: flex; gap: 5px;">
                  <input type="number" class="form-control" [(ngModel)]="precioTemp" min="0" step="0.01" placeholder="0.00">
                  <button type="button" class="btn btn-success" (click)="agregarDetalle()">
                    <i class="fas fa-plus"></i>
                  </button>
                </div>
              </div>
            </div>

            <div *ngIf="compraActual.detalles.length > 0" class="table-container mt-2">
              <table class="table">
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Cantidad</th>
                    <th>Precio Unit.</th>
                    <th>Subtotal</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let detalle of compraActual.detalles; let i = index">
                    <td>{{ detalle.producto.nombre }}</td>
                    <td>{{ detalle.cantidad }}</td>
                    <td>{{ detalle.precioUnitario | soles }}</td>
                    <td>{{ detalle.subtotal | soles }}</td>
                    <td>
                      <button class="btn btn-sm btn-danger" (click)="eliminarDetalle(i)">
                        <i class="fas fa-trash"></i>
                      </button>
                    </td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr>
                    <td colspan="3" class="text-right"><strong>Subtotal:</strong></td>
                    <td colspan="2"><strong>{{ compraActual.subtotal | soles }}</strong></td>
                  </tr>
                  <tr>
                    <td colspan="3" class="text-right"><strong>IGV (18%):</strong></td>
                    <td colspan="2"><strong>{{ compraActual.impuesto | soles }}</strong></td>
                  </tr>
                  <tr class="total-row">
                    <td colspan="3" class="text-right"><strong>TOTAL:</strong></td>
                    <td colspan="2"><strong>{{ compraActual.total | soles }}</strong></td>
                  </tr>
                </tfoot>
              </table>
            </div>

            <div *ngIf="compraActual.detalles.length === 0" class="alert alert-info mt-2">
              <i class="fas fa-info-circle"></i> Agregue productos a la compra
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" (click)="cancelarModal()">
              <i class="fas fa-times"></i> Cancelar
            </button>
            <button type="button" class="btn btn-success" (click)="procesarCompra()" [disabled]="compraActual.detalles.length === 0">
              <i class="fas fa-check"></i> Procesar Compra
            </button>
          </div>
        </div>
      </div>
      
      <!-- Lista de Compras Recientes -->
      <div class="card mt-2">
        <h3><i class="fas fa-history"></i> Compras Recientes</h3>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Nº Compra</th>
                <th>Fecha</th>
                <th>Proveedor</th>
                <th>Total</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let compra of compras">
                <td>{{ compra.numeroCompra }}</td>
                <td>{{ compra.fecha | date:'dd/MM/yyyy HH:mm' }}</td>
                <td>{{ compra.proveedor?.nombre || 'Sin proveedor' }}</td>
                <td>{{ compra.total | soles }}</td>
                <td>
                  <span class="badge" 
                    [class.badge-warning]="compra.estado === 'PENDIENTE'"
                    [class.badge-success]="compra.estado === 'RECIBIDA'"
                    [class.badge-danger]="compra.estado === 'CANCELADA'">
                    {{ compra.estado }}
                  </span>
                </td>
                <td>
                  <button class="btn btn-sm btn-info" (click)="verDetalle(compra)" title="Ver detalle">
                    <i class="fas fa-eye"></i>
                  </button>
                  <button *ngIf="compra.estado === 'PENDIENTE'"
                    class="btn btn-sm btn-success"
                    (click)="recibirCompraExistente(compra.id!)"
                    title="Marcar como recibida">
                    <i class="fas fa-check"></i>
                  </button>
                  <button *ngIf="compra.estado === 'PENDIENTE'" 
                    class="btn btn-sm btn-danger" 
                    (click)="cancelarCompraExistente(compra.id!)"
                    title="Cancelar compra">
                    <i class="fas fa-ban"></i>
                  </button>
                </td>
              </tr>
              <tr *ngIf="compras.length === 0">
                <td colspan="6" style="text-align: center;">No hay compras registradas</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .total-row {
      background-color: #f8f9fa;
      font-size: 1.2em;
    }
    
    .alert-info {
      background-color: #d1ecf1;
      border: 1px solid #bee5eb;
      color: #0c5460;
      padding: 12px;
      border-radius: 5px;
    }
  `]
})
export class ComprasComponent implements OnInit {
  compras: Compra[] = [];
  proveedores: Proveedor[] = [];
  productos: Producto[] = [];
  mostrarFormulario = false;
  
  compraActual: Compra = this.nuevaCompra();
  productoTemp: Producto | null = null;
  cantidadTemp = 1;
  precioTemp = 0;

  constructor(
    private compraService: CompraService,
    private proveedorService: ProveedorService,
    private productoService: ProductoService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.cargarCompras();
    this.cargarProveedores();
    this.cargarProductos();
  }

  cargarCompras(): void {
    this.compraService.obtenerTodas().pipe(
      map((data) => this.normalizarCompras(data)),
      catchError(() => {
        // Fallback frontend: si falla /compras, intentar por periodo actual.
        const hoy = new Date();
        const anio = hoy.getFullYear();
        const mes = hoy.getMonth() + 1;
        const inicio = `${anio}-${String(mes).padStart(2, '0')}-01T00:00:00`;
        const ultimoDia = new Date(anio, mes, 0).getDate();
        const fin = `${anio}-${String(mes).padStart(2, '0')}-${String(ultimoDia).padStart(2, '0')}T23:59:59`;

        return this.compraService.obtenerPorPeriodo(inicio, fin).pipe(
          map((data) => this.normalizarCompras(data)),
          catchError(() => of([] as Compra[]))
        );
      })
    ).subscribe({
      next: (data) => {
        this.compras = data;
        this.cdr.detectChanges();
      },
      error: () => AppLogger.error('Error al cargar compras')
    });
  }

  private normalizarCompras(data: Compra[] | null | undefined): Compra[] {
    if (!Array.isArray(data)) {
      return [];
    }

    return data
      .filter((c) => !!c)
      .map((c) => ({
        ...c,
        proveedor: c.proveedor || ({ nombre: 'Sin proveedor' } as Proveedor),
        detalles: Array.isArray(c.detalles) ? c.detalles : []
      }));
  }

  cargarProveedores(): void {
    this.proveedorService.obtenerActivos().subscribe({
      next: (data) => {
        this.proveedores = data;
        this.cdr.detectChanges();
      },
      error: () => AppLogger.error('Error al cargar proveedores')
    });
  }

  cargarProductos(): void {
    this.productoService.obtenerActivos().subscribe({
      next: (data) => {
        this.productos = data;
        this.cdr.detectChanges();
      },
      error: () => AppLogger.error('Error al cargar productos')
    });
  }

  agregarDetalle(): void {
    if (!this.productoTemp) {
      Swal.fire({
        icon: 'warning',
        title: 'Seleccione un producto',
        timer: 2000,
        showConfirmButton: false
      });
      return;
    }

    if (this.cantidadTemp <= 0) {
      Swal.fire({
        icon: 'warning',
        title: 'Cantidad inválida',
        text: 'La cantidad debe ser mayor a 0',
        timer: 2000,
        showConfirmButton: false
      });
      return;
    }

    if (this.precioTemp <= 0) {
      Swal.fire({
        icon: 'warning',
        title: 'Precio inválido',
        text: 'El precio debe ser mayor a 0',
        timer: 2000,
        showConfirmButton: false
      });
      return;
    }

    const subtotal = this.cantidadTemp * this.precioTemp;
    
    const detalle: DetalleCompra = {
      producto: this.productoTemp,
      cantidad: this.cantidadTemp,
      precioUnitario: this.precioTemp,
      subtotal: subtotal
    };

    this.compraActual.detalles.push(detalle);
    this.calcularTotales();
    
    // Limpiar campos
    this.productoTemp = null;
    this.cantidadTemp = 1;
    this.precioTemp = 0;
  }

  eliminarDetalle(index: number): void {
    this.compraActual.detalles.splice(index, 1);
    this.calcularTotales();
  }

  calcularTotales(): void {
    this.compraActual.subtotal = this.compraActual.detalles.reduce(
      (sum, det) => sum + det.subtotal, 
      0
    );
    this.compraActual.impuesto = this.compraActual.subtotal * 0.18;
    this.compraActual.total = this.compraActual.subtotal + this.compraActual.impuesto;
  }

  procesarCompra(): void {
    if (!this.compraActual.proveedor) {
      Swal.fire({
        icon: 'warning',
        title: 'Seleccione un proveedor',
        timer: 2000,
        showConfirmButton: false
      });
      return;
    }

    if (this.compraActual.detalles.length === 0) {
      Swal.fire({
        icon: 'warning',
        title: 'Agregue productos',
        text: 'Debe agregar al menos un producto',
        timer: 2000,
        showConfirmButton: false
      });
      return;
    }

    this.compraService.crear(this.compraActual).subscribe({
      next: (compra) => {
        Swal.fire({
          icon: 'success',
          title: 'Compra procesada',
          text: `Compra Nº ${compra.numeroCompra} registrada correctamente`,
          timer: 2000,
          showConfirmButton: false
        });
        this.cargarCompras();
        this.cargarProductos();
        this.compraActual = this.nuevaCompra();
        this.mostrarFormulario = false;
      },
      error: (err) => {
        Swal.fire({
          icon: 'error',
          title: 'Error al procesar compra',
          text: err.error?.message || 'Error desconocido'
        });
      }
    });
  }

  cancelarCompra(): void {
    Swal.fire({
      title: '¿Cancelar compra?',
      text: 'Se perderán todos los datos ingresados',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Sí, cancelar',
      cancelButtonText: 'No'
    }).then((result) => {
      if (result.isConfirmed) {
        this.cancelarModal();
      }
    });
  }

  cancelarModal(): void {
    this.compraActual = this.nuevaCompra();
    this.productoTemp = null;
    this.cantidadTemp = 1;
    this.precioTemp = 0;
    this.mostrarFormulario = false;
  }

  abrirNuevo(): void {
    this.compraActual = this.nuevaCompra();
    this.productoTemp = null;
    this.cantidadTemp = 1;
    this.precioTemp = 0;
    this.mostrarFormulario = true;
  }

  cerrarAlHacerClick(event: MouseEvent): void {
    if ((event.target as Element).classList.contains('modal-overlay')) {
      this.cancelarModal();
    }
  }

  cancelarCompraExistente(id: number): void {
    Swal.fire({
      title: '¿Cancelar esta compra?',
      text: 'Esta acción no se puede deshacer',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Sí, cancelar',
      cancelButtonText: 'No'
    }).then((result) => {
      if (result.isConfirmed) {
        this.compraService.cancelar(id).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Compra cancelada',
              timer: 1500,
              showConfirmButton: false
            });
            this.cargarCompras();
            this.cargarProductos();
          },
          error: () => AppLogger.error('Error al cancelar compra')
        });
      }
    });
  }

  recibirCompraExistente(id: number): void {
    Swal.fire({
      title: '¿Marcar compra como recibida?',
      text: 'Esta acción incrementará el stock de los productos de la compra.',
      icon: 'question',
      showCancelButton: true,
      confirmButtonColor: '#28a745',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, recibir',
      cancelButtonText: 'No'
    }).then((result) => {
      if (result.isConfirmed) {
        this.compraService.recibir(id).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Compra recibida',
              text: 'La compra se marcó como recibida y el stock fue actualizado.',
              timer: 1800,
              showConfirmButton: false
            });
            this.cargarCompras();
            this.cargarProductos();
          },
          error: (err) => {
            Swal.fire({
              icon: 'error',
              title: 'No se pudo recibir la compra',
              text: err?.error?.message || 'Ocurrió un error al marcar la compra como recibida.'
            });
          }
        });
      }
    });
  }

  verDetalle(compra: Compra): void {
    const detallesTexto = (compra.detalles || [])
      .map(d => `${d.producto?.nombre || 'N/A'} | Cant: ${d.cantidad} | Precio: S/ ${d.precioUnitario.toFixed(2)} | Subtotal: S/ ${d.subtotal.toFixed(2)}`)
      .join('\n');

    const bloques = [
      `Proveedor: ${compra.proveedor?.nombre || 'N/A'}`,
      `Fecha: ${compra.fecha ? new Date(compra.fecha).toLocaleString('es-PE') : 'N/A'}`,
      `Estado: ${compra.estado || 'N/A'}`,
      compra.observaciones ? `Observaciones: ${compra.observaciones}` : '',
      '',
      'DETALLES',
      detallesTexto || 'Sin detalles',
      '',
      `Subtotal: S/ ${compra.subtotal.toFixed(2)}`,
      `IGV (18%): S/ ${compra.impuesto.toFixed(2)}`,
      `TOTAL: S/ ${compra.total.toFixed(2)}`
    ].filter(Boolean);

    Swal.fire({
      title: `Compra Nº ${compra.numeroCompra}`,
      text: bloques.join('\n'),
      width: 700,
      confirmButtonText: 'Cerrar'
    });
  }

  private nuevaCompra(): Compra {
    return {
      proveedor: null as any,
      subtotal: 0,
      impuesto: 0,
      total: 0,
      estado: 'PENDIENTE',
      observaciones: '',
      detalles: []
    };
  }
}
