import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClienteService } from '../../services/cliente.service';
import { AppLogger } from '../../services/app-logger.service';
import { Cliente } from '../../models/models';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card">
      <div class="card-header">
        <h2>Gestión de Clientes</h2>
        <button class="btn btn-primary" (click)="abrirNuevo()">
          <i class="fas fa-plus"></i> Nuevo Cliente
        </button>
      </div>
      
      <!-- Modal Formulario -->
      <div class="modal-overlay" *ngIf="mostrarFormulario" (click)="cerrarAlHacerClick($event)">
        <div class="modal-container">
          <div class="modal-header">
            <h3><i class="fas fa-user"></i> {{ clienteSeleccionado.id ? 'Editar' : 'Nuevo' }} Cliente</h3>
            <button class="btn-close-modal" (click)="cancelar()" title="Cerrar">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="modal-body">
            <form (ngSubmit)="guardar()" #clienteForm="ngForm">
              <div class="grid grid-2">
                <div class="form-group">
                  <label>Nombre *</label>
                  <input type="text" class="form-control" [(ngModel)]="clienteSeleccionado.nombre" name="nombre" required>
                </div>
                <div class="form-group">
                  <label>Apellido *</label>
                  <input type="text" class="form-control" [(ngModel)]="clienteSeleccionado.apellido" name="apellido" required>
                </div>
                <div class="form-group">
                  <label>DNI</label>
                  <input type="text" class="form-control" [(ngModel)]="clienteSeleccionado.dni" name="dni">
                </div>
                <div class="form-group">
                  <label>Teléfono</label>
                  <input type="text" class="form-control" [(ngModel)]="clienteSeleccionado.telefono" name="telefono">
                </div>
                <div class="form-group">
                  <label>Email</label>
                  <input type="email" class="form-control" [(ngModel)]="clienteSeleccionado.email" name="email">
                </div>
                <div class="form-group">
                  <label>Fecha de Nacimiento</label>
                  <input type="date" class="form-control" [(ngModel)]="clienteSeleccionado.fechaNacimiento" name="fechaNacimiento">
                </div>
                <div class="form-group" style="grid-column: span 2;">
                  <label>Dirección</label>
                  <textarea class="form-control" [(ngModel)]="clienteSeleccionado.direccion" name="direccion" rows="2"></textarea>
                </div>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" (click)="cancelar()">Cancelar</button>
            <button type="button" class="btn btn-success" [disabled]="!clienteForm.valid" (click)="guardar()">
              <i class="fas fa-save"></i> Guardar
            </button>
          </div>
        </div>
      </div>
      
      <!-- Búsqueda -->
      <div class="mt-2">
        <input type="text" 
               class="form-control" 
               placeholder="Buscar cliente..." 
               [(ngModel)]="terminoBusqueda"
               (ngModelChange)="buscar()">
      </div>
      
      <!-- Tabla -->
      <div class="mt-2">
        <table>
          <thead>
            <tr>
              <th>Nombre</th>
              <th>DNI</th>
              <th>Teléfono</th>
              <th>Email</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let cliente of clientes">
              <td>{{ cliente.nombre }} {{ cliente.apellido }}</td>
              <td>{{ cliente.dni || 'N/A' }}</td>
              <td>{{ cliente.telefono || 'N/A' }}</td>
              <td>{{ cliente.email || 'N/A' }}</td>
              <td>
                <span [style.color]="cliente.activo ? '#2ecc71' : '#e74c3c'">
                  {{ cliente.activo ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
              <td>
                <button class="btn btn-sm btn-warning" (click)="editar(cliente)" title="Editar">
                  <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" (click)="eliminar(cliente.id!)" title="Eliminar">
                  <i class="fas fa-trash"></i>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        
        <div *ngIf="clientes.length === 0" class="text-center mt-2">
          <p>No hay clientes registrados</p>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class ClientesComponent implements OnInit {
  clientes: Cliente[] = [];
  clienteSeleccionado: Cliente = this.nuevoCliente();
  mostrarFormulario = false;
  terminoBusqueda = '';

  constructor(
    private readonly clienteService: ClienteService,
    private readonly cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.cargarClientes();
  }

  cargarClientes(): void {
    this.clienteService.obtenerTodos().subscribe({
      next: (data) => {
        this.clientes = data;
        this.cdr.detectChanges();
      },
      error: () => AppLogger.error('Error al cargar clientes')
    });
  }

  buscar(): void {
    if (this.terminoBusqueda.trim()) {
      this.clienteService.buscar(this.terminoBusqueda).subscribe({
        next: (data) => {
          this.clientes = data;
          this.cdr.detectChanges();
        },
        error: () => AppLogger.error('Error al buscar clientes')
      });
    } else {
      this.cargarClientes();
    }
  }

  guardar(): void {
    if (this.clienteSeleccionado.id) {
      this.clienteService.actualizar(this.clienteSeleccionado.id, this.clienteSeleccionado).subscribe({
        next: () => {
          Swal.fire({
            icon: 'success',
            title: 'Cliente actualizado',
            text: 'El cliente se actualizó correctamente',
            timer: 2000,
            showConfirmButton: false
          });
          this.cargarClientes();
          this.cdr.detectChanges();
          this.cancelar();
        },
        error: () => AppLogger.error('Error al actualizar cliente')
      });
    } else {
      this.clienteService.crear(this.clienteSeleccionado).subscribe({
        next: () => {
          Swal.fire({
            icon: 'success',
            title: 'Cliente creado',
            text: 'El cliente se creó correctamente',
            timer: 2000,
            showConfirmButton: false
          });
          this.cargarClientes();
          this.cdr.detectChanges();
          this.cancelar();
        },
        error: () => AppLogger.error('Error al crear cliente')
      });
    }
  }

  editar(cliente: Cliente): void {
    this.clienteSeleccionado = { ...cliente };
    this.mostrarFormulario = true;
  }

  eliminar(id: number): void {
    Swal.fire({
      title: '¿Eliminar cliente?',
      text: 'Esta acción no se puede deshacer',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.clienteService.eliminar(id).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Eliminado',
              text: 'Cliente eliminado correctamente',
              timer: 2000,
              showConfirmButton: false
            });
            this.cargarClientes();
            this.cdr.detectChanges();
          },
          error: (error) => {
            AppLogger.error('Error al eliminar cliente');
            Swal.fire({
              icon: 'error',
              title: 'Error',
              text: 'No se pudo eliminar el cliente'
            });
          }
        });
      }
    });
  }

  cancelar(): void {
    this.clienteSeleccionado = this.nuevoCliente();
    this.mostrarFormulario = false;
  }

  abrirNuevo(): void {
    this.clienteSeleccionado = this.nuevoCliente();
    this.mostrarFormulario = true;
  }

  cerrarAlHacerClick(event: MouseEvent): void {
    if ((event.target as Element).classList.contains('modal-overlay')) {
      this.cancelar();
    }
  }

  private nuevoCliente(): Cliente {
    return {
      nombre: '',
      apellido: '',
      activo: true
    };
  }
}
