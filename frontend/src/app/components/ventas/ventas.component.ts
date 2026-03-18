import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VentaService } from '../../services/venta.service';
import { ProductoService } from '../../services/producto.service';
import { ClienteService } from '../../services/cliente.service';
import { AppLogger } from '../../services/app-logger.service';
import { Venta, DetalleVenta, Producto, Cliente } from '../../models/models';
import { SolesPipe } from '../../pipes/soles.pipe';
import Swal from 'sweetalert2';

const IGV_RATE = 0.18;

@Component({
  selector: 'app-ventas',
  standalone: true,
  imports: [CommonModule, FormsModule, SolesPipe],
  template: `
    <div class="card">
      <div class="card-header">
        <h2>Gestión de Ventas</h2>
        <button class="btn btn-primary" (click)="abrirNuevo()">
          <i class="fas fa-plus"></i> Nueva Venta
        </button>
      </div>
      
      <!-- Modal Nueva Venta -->
      <div class="modal-overlay" *ngIf="mostrarFormulario" (click)="cerrarAlHacerClick($event)">
        <div class="modal-container modal-lg">
          <div class="modal-header">
            <h3><i class="fas fa-cash-register"></i> Nueva Venta</h3>
            <button class="btn-close-modal" (click)="cancelar()" title="Cerrar">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="modal-body">
            <div class="grid grid-2">
              <div class="form-group">
                <label>Cliente (Opcional)</label>
                <select class="form-control" [(ngModel)]="ventaActual.cliente">
                  <option [ngValue]="null">Venta al público</option>
                  <option *ngFor="let cliente of clientes" [ngValue]="cliente">
                    {{ cliente.nombre }} {{ cliente.apellido }}
                  </option>
                </select>
              </div>
              <div class="form-group">
                <label>Método de Pago</label>
                <select class="form-control" [(ngModel)]="ventaActual.metodoPago">
                  <option value="EFECTIVO">Efectivo</option>
                  <option value="TARJETA">Tarjeta</option>
                  <option value="TRANSFERENCIA">Transferencia</option>
                </select>
              </div>
            </div>

            <h4 class="mt-2">Agregar Productos</h4>
            <div class="grid grid-3">
              <div class="form-group">
                <label>Producto</label>
                <select class="form-control" [(ngModel)]="productoTemp">
                  <option [ngValue]="null">Seleccione...</option>
                  <option *ngFor="let prod of productos" [ngValue]="prod">
                    {{ prod.nombre }} - Stock: {{ prod.stock }}
                  </option>
                </select>
              </div>
              <div class="form-group">
                <label>Cantidad</label>
                <input type="number" class="form-control" [(ngModel)]="cantidadTemp" min="1">
              </div>
              <div class="form-group">
                <label>&nbsp;</label>
                <button type="button" class="btn btn-success" (click)="agregarDetalle()">Agregar</button>
              </div>
            </div>

            <table class="mt-2" *ngIf="ventaActual.detalles.length > 0">
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
                <tr *ngFor="let detalle of ventaActual.detalles; let i = index">
                  <td>{{ detalle.producto.nombre }}</td>
                  <td>{{ detalle.cantidad }}</td>
                  <td>{{ detalle.precioUnitario | soles }}</td>
                  <td>{{ detalle.subtotal | soles }}</td>
                  <td>
                    <button class="btn btn-danger" (click)="eliminarDetalle(i)">X</button>
                  </td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="3" class="text-right"><strong>Subtotal:</strong></td>
                  <td colspan="2">{{ ventaActual.subtotal | soles }}</td>
                </tr>
                <tr>
                  <td colspan="3" class="text-right"><strong>IGV (18%):</strong></td>
                  <td colspan="2">
                    <input type="number" step="0.01" class="form-control" [ngModel]="ventaActual.impuesto" readonly>
                  </td>
                </tr>
                <tr>
                  <td colspan="3" class="text-right"><strong>Descuento:</strong></td>
                  <td colspan="2">
                    <input type="number" step="0.01" class="form-control" [(ngModel)]="ventaActual.descuento" (ngModelChange)="calcularTotal()">
                  </td>
                </tr>
                <tr>
                  <td colspan="3" class="text-right"><strong>TOTAL:</strong></td>
                  <td colspan="2"><strong>{{ ventaActual.total | soles }}</strong></td>
                </tr>
              </tfoot>
            </table>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" (click)="cancelar()">
              <i class="fas fa-times"></i> Cancelar
            </button>
            <button type="button" class="btn btn-success" (click)="guardarVenta()">
              <i class="fas fa-check"></i> Procesar Venta
            </button>
          </div>
        </div>
      </div>
      
      <!-- Lista de Ventas -->
      <div class="mt-2">
        <h3>Historial de Ventas</h3>
        <table>
          <thead>
            <tr>
              <th>Número</th>
              <th>Fecha</th>
              <th>Cliente</th>
              <th>Total</th>
              <th>Método Pago</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let venta of ventas">
              <td>{{ venta.numeroVenta }}</td>
              <td>{{ venta.fecha | date:'short' }}</td>
              <td>{{ venta.cliente ? (venta.cliente.nombre + ' ' + venta.cliente.apellido) : 'Público' }}</td>
              <td>{{ venta.total | soles }}</td>
              <td>{{ venta.metodoPago }}</td>
              <td>
                <span [style.color]="venta.estado === 'COMPLETADA' ? '#2ecc71' : '#e74c3c'">
                  {{ venta.estado }}
                </span>
              </td>
              <td>
                <button class="btn btn-danger" (click)="cancelarVenta(venta.id!)" 
                        [disabled]="venta.estado !== 'COMPLETADA'">
                  Cancelar
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        
        <div *ngIf="ventas.length === 0" class="text-center mt-2">
          <p>No hay ventas registradas</p>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class VentasComponent implements OnInit {
  ventas: Venta[] = [];
  ventaActual: Venta = this.nuevaVenta();
  productos: Producto[] = [];
  clientes: Cliente[] = [];
  mostrarFormulario = false;
  productoTemp: Producto | null = null;
  cantidadTemp = 1;

  constructor(
    private ventaService: VentaService,
    private productoService: ProductoService,
    private clienteService: ClienteService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.cargarVentas();
    this.cargarProductos();
    this.cargarClientes();
  }

  cargarVentas(): void {
    this.ventaService.obtenerTodas().subscribe({
      next: (data) => {
        this.ventas = data;
        this.cdr.detectChanges();
      },
      error: () => AppLogger.error('Error al cargar ventas')
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

  cargarClientes(): void {
    this.clienteService.obtenerActivos().subscribe({
      next: (data) => {
        this.clientes = data;
        this.cdr.detectChanges();
      },
      error: () => AppLogger.error('Error al cargar clientes')
    });
  }

  agregarDetalle(): void {
    if (!this.productoTemp || this.cantidadTemp <= 0) {
      Swal.fire({
        icon: 'warning',
        title: 'Datos incompletos',
        text: 'Seleccione un producto y cantidad válida'
      });
      return;
    }

    if (this.productoTemp.stock < this.cantidadTemp) {
      Swal.fire({
        icon: 'error',
        title: 'Stock insuficiente',
        text: `Solo hay ${this.productoTemp.stock} unidades disponibles`
      });
      return;
    }

    const detalle: DetalleVenta = {
      producto: this.productoTemp,
      cantidad: this.cantidadTemp,
      precioUnitario: this.productoTemp.precioVenta,
      subtotal: this.productoTemp.precioVenta * this.cantidadTemp,
      descuento: 0
    };

    this.ventaActual.detalles.push(detalle);
    this.calcularSubtotal();
    
    this.productoTemp = null;
    this.cantidadTemp = 1;
  }

  eliminarDetalle(index: number): void {
    this.ventaActual.detalles.splice(index, 1);
    this.calcularSubtotal();
  }

  calcularSubtotal(): void {
    this.ventaActual.subtotal = this.ventaActual.detalles.reduce((sum, d) => sum + d.subtotal, 0);
    this.ventaActual.impuesto = this.redondear(this.ventaActual.subtotal * IGV_RATE);
    this.calcularTotal();
  }

  calcularTotal(): void {
    this.ventaActual.total = this.redondear(
      this.ventaActual.subtotal + this.ventaActual.impuesto - this.ventaActual.descuento
    );
  }

  guardarVenta(): void {
    if (this.ventaActual.detalles.length === 0) {
      Swal.fire({
        icon: 'warning',
        title: 'Faltan productos por agregar',
        text: this.productoTemp
          ? 'Seleccione "Agregar" para incluir el producto en la venta antes de procesarla'
          : 'Agregue al menos un producto antes de procesar la venta'
      });
      return;
    }

    this.ventaService.crear(this.ventaActual).subscribe(
      () => {
        Swal.fire({
          icon: 'success',
          title: 'Venta procesada',
          text: 'La venta se processó correctamente',
          timer: 2000,
          showConfirmButton: false
        });
        this.cargarVentas();
        this.cargarProductos();
        this.cancelar();
      },
      (error) => {
        AppLogger.error('Error al procesar venta');
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: error?.error?.message || 'No se pudo procesar la venta'
        });
      }
    );
  }

  cancelarVenta(id: number): void {
    Swal.fire({
      title: '¿Cancelar venta?',
      text: 'Esta acción no se puede deshacer',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Sí, cancelar',
      cancelButtonText: 'No cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.ventaService.cancelar(id).subscribe(
          () => {
            Swal.fire({
              icon: 'success',
              title: 'Venta cancelada',
              text: 'La venta se canceló correctamente',
              timer: 2000,
              showConfirmButton: false
            });
            this.cargarVentas();
            this.cargarProductos();
          },
          () => {
            AppLogger.error('Error al cancelar venta');
            Swal.fire({
              icon: 'error',
              title: 'Error',
              text: 'No se pudo cancelar la venta'
            });
          }
        );
      }
    });
  }

  cancelar(): void {
    this.ventaActual = this.nuevaVenta();
    this.mostrarFormulario = false;
  }

  abrirNuevo(): void {
    this.ventaActual = this.nuevaVenta();
    this.mostrarFormulario = true;
  }

  cerrarAlHacerClick(event: MouseEvent): void {
    if ((event.target as Element).classList.contains('modal-overlay')) {
      this.cancelar();
    }
  }

  private nuevaVenta(): Venta {
    return {
      subtotal: 0,
      impuesto: 0,
      descuento: 0,
      total: 0,
      metodoPago: 'EFECTIVO',
      detalles: []
    };
  }

  private redondear(valor: number): number {
    return Math.round((valor + Number.EPSILON) * 100) / 100;
  }
}
