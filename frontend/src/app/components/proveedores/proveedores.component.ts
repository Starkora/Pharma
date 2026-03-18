import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProveedorService } from '../../services/proveedor.service';
import { AppLogger } from '../../services/app-logger.service';
import { Proveedor } from '../../models/models';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-proveedores',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card">
      <div class="card-header">
        <h2><i class="fas fa-building"></i> Gestión de Proveedores</h2>
        <button class="btn btn-primary" (click)="mostrarFormulario()">
          <i class="fas fa-plus"></i> Nuevo Proveedor
        </button>
      </div>
      
      <!-- Modal Formulario -->
      <div class="modal-overlay" *ngIf="mostrandoFormulario" (click)="cerrarAlHacerClick($event)">
        <div class="modal-container modal-lg">
          <div class="modal-header">
            <h3><i class="fas fa-edit"></i> {{ proveedorSeleccionado.id ? 'Editar' : 'Nuevo' }} Proveedor</h3>
            <button class="btn-close-modal" (click)="cancelar()" title="Cerrar">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="modal-body">
            <form (ngSubmit)="guardar()" #proveedorForm="ngForm">
              <div class="grid grid-3">
                <div class="form-group">
                  <label for="nombre">Nombre *</label>
                  <input type="text" id="nombre" class="form-control"
                    [(ngModel)]="proveedorSeleccionado.nombre" name="nombre" required
                    placeholder="Nombre del proveedor">
                </div>
                <div class="form-group">
                  <label for="ruc">RUC</label>
                  <input type="text" id="ruc" class="form-control"
                    [(ngModel)]="proveedorSeleccionado.ruc" name="ruc" maxlength="11"
                    placeholder="12345678901">
                </div>
                <div class="form-group">
                  <label for="telefono">Teléfono</label>
                  <input type="tel" id="telefono" class="form-control"
                    [(ngModel)]="proveedorSeleccionado.telefono" name="telefono"
                    placeholder="999 999 999">
                </div>
              </div>
              <div class="grid grid-2">
                <div class="form-group">
                  <label for="email">Email</label>
                  <input type="email" id="email" class="form-control"
                    [(ngModel)]="proveedorSeleccionado.email" name="email"
                    placeholder="proveedor@ejemplo.com">
                </div>
                <div class="form-group">
                  <label for="personaContacto">Persona de Contacto</label>
                  <input type="text" id="personaContacto" class="form-control"
                    [(ngModel)]="proveedorSeleccionado.personaContacto" name="personaContacto"
                    placeholder="Nombre del contacto">
                </div>
              </div>
              <div class="grid grid-2">
                <div class="form-group">
                  <label for="direccion">Dirección</label>
                  <input type="text" id="direccion" class="form-control"
                    [(ngModel)]="proveedorSeleccionado.direccion" name="direccion"
                    placeholder="Dirección completa">
                </div>
                <div class="form-group">
                  <label for="activo">Estado</label>
                  <select id="activo" class="form-control"
                    [(ngModel)]="proveedorSeleccionado.activo" name="activo">
                    <option [value]="true">Activo</option>
                    <option [value]="false">Inactivo</option>
                  </select>
                </div>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" (click)="cancelar()">
              <i class="fas fa-times"></i> Cancelar
            </button>
            <button type="button" class="btn btn-success" [disabled]="!proveedorForm.valid" (click)="guardar()">
              <i class="fas fa-save"></i> Guardar
            </button>
          </div>
        </div>
      </div>
      
      <!-- Lista de Proveedores -->
      <div class="table-container mt-2">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre</th>
              <th>RUC</th>
              <th>Teléfono</th>
              <th>Email</th>
              <th>Persona de Contacto</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let proveedor of proveedores">
              <td>{{ proveedor.id }}</td>
              <td>{{ proveedor.nombre }}</td>
              <td>{{ proveedor.ruc || '-' }}</td>
              <td>{{ proveedor.telefono || '-' }}</td>
              <td>{{ proveedor.email || '-' }}</td>
              <td>{{ proveedor.personaContacto || '-' }}</td>
              <td>
                <span class="badge" [class.badge-success]="proveedor.activo" [class.badge-danger]="!proveedor.activo">
                  {{ proveedor.activo ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
              <td>
                <button class="btn btn-sm btn-primary" (click)="editar(proveedor)">
                  <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" (click)="eliminar(proveedor.id!)">
                  <i class="fas fa-trash"></i>
                </button>
              </td>
            </tr>
            <tr *ngIf="proveedores.length === 0">
              <td colspan="8" style="text-align: center;">No hay proveedores registrados</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class ProveedoresComponent implements OnInit {
  proveedores: Proveedor[] = [];
  proveedorSeleccionado: Proveedor = this.nuevoProveedor();
  mostrandoFormulario = false;

  constructor(
    private readonly proveedorService: ProveedorService,
    private readonly cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.cargarProveedores();
  }

  cargarProveedores(): void {
    this.proveedorService.obtenerTodos().subscribe({
      next: (data) => {
        this.proveedores = data;
        this.cdr.detectChanges();  // Fuerza la detección de cambios
      },
      error: (err) => {
        AppLogger.error('Error al cargar proveedores');
      }
    });
  }

  mostrarFormulario(): void {
    this.proveedorSeleccionado = this.nuevoProveedor();
    this.mostrandoFormulario = true;
  }

  editar(proveedor: Proveedor): void {
    this.proveedorSeleccionado = { ...proveedor };
    this.mostrandoFormulario = true;
  }

  guardar(): void {
    if (this.proveedorSeleccionado.id) {
      this.proveedorService.actualizar(this.proveedorSeleccionado.id, this.proveedorSeleccionado)
        .subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Proveedor actualizado',
              timer: 1500,
              showConfirmButton: false
            });
            this.cargarProveedores();
            this.cdr.detectChanges();
            this.cancelar();
          },
          error: () => AppLogger.error('Error al actualizar proveedor')
        });
    } else {
      this.proveedorService.crear(this.proveedorSeleccionado).subscribe({
        next: () => {
          Swal.fire({
            icon: 'success',
            title: 'Proveedor creado',
            timer: 1500,
            showConfirmButton: false
          });
          this.cargarProveedores();
          this.cdr.detectChanges();
          this.cancelar();
        },
        error: () => AppLogger.error('Error al crear proveedor')
      });
    }
  }

  eliminar(id: number): void {
    Swal.fire({
      title: '¿Eliminar proveedor?',
      text: 'Esta acción no se puede deshacer',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.proveedorService.eliminar(id).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Proveedor eliminado',
              timer: 1500,
              showConfirmButton: false
            });
            this.cargarProveedores();
            this.cdr.detectChanges();
          },
          error: () => AppLogger.error('Error al eliminar proveedor')
        });
      }
    });
  }

  cancelar(): void {
    this.proveedorSeleccionado = this.nuevoProveedor();
    this.mostrandoFormulario = false;
  }

  cerrarAlHacerClick(event: MouseEvent): void {
    if ((event.target as Element).classList.contains('modal-overlay')) {
      this.cancelar();
    }
  }

  private nuevoProveedor(): Proveedor {
    return {
      nombre: '',
      ruc: '',
      direccion: '',
      telefono: '',
      email: '',
      personaContacto: '',
      activo: true
    };
  }
}
