import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CategoriaService } from '../../services/categoria.service';
import { AppLogger } from '../../services/app-logger.service';
import { Categoria } from '../../models/models';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-categorias',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card">
      <div class="card-header">
        <h2><i class="fas fa-list"></i> Gestión de Categorías</h2>
        <button class="btn btn-primary" (click)="mostrarFormulario()">
          <i class="fas fa-plus"></i> Nueva Categoría
        </button>
      </div>
      
      <!-- Modal Formulario -->
      <div class="modal-overlay" *ngIf="mostrandoFormulario" (click)="cerrarAlHacerClick($event)">
        <div class="modal-container">
          <div class="modal-header">
            <h3><i class="fas fa-edit"></i> {{ categoriaSeleccionada.id ? 'Editar' : 'Nueva' }} Categoría</h3>
            <button class="btn-close-modal" (click)="cancelar()" title="Cerrar">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="modal-body">
            <form (ngSubmit)="guardar()" #categoriaForm="ngForm">
              <div class="grid grid-2">
                <div class="form-group">
                  <label for="nombre">Nombre *</label>
                  <input
                    type="text"
                    id="nombre"
                    class="form-control"
                    [(ngModel)]="categoriaSeleccionada.nombre"
                    name="nombre"
                    required
                    placeholder="Ej: Medicamentos">
                </div>
                <div class="form-group">
                  <label for="activo">Estado</label>
                  <select
                    id="activo"
                    class="form-control"
                    [(ngModel)]="categoriaSeleccionada.activo"
                    name="activo">
                    <option [value]="true">Activo</option>
                    <option [value]="false">Inactivo</option>
                  </select>
                </div>
              </div>
              <div class="form-group">
                <label for="descripcion">Descripción</label>
                <textarea
                  id="descripcion"
                  class="form-control"
                  [(ngModel)]="categoriaSeleccionada.descripcion"
                  name="descripcion"
                  rows="3"
                  placeholder="Descripción de la categoría"></textarea>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" (click)="cancelar()">
              <i class="fas fa-times"></i> Cancelar
            </button>
            <button type="button" class="btn btn-success" [disabled]="!categoriaForm.valid" (click)="guardar()">
              <i class="fas fa-save"></i> Guardar
            </button>
          </div>
        </div>
      </div>
      
      <!-- Lista de Categorías -->
      <div class="table-container mt-2">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre</th>
              <th>Descripción</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let categoria of categorias">
              <td>{{ categoria.id }}</td>
              <td>{{ categoria.nombre }}</td>
              <td>{{ categoria.descripcion || '-' }}</td>
              <td>
                <span class="badge" [class.badge-success]="categoria.activo" [class.badge-danger]="!categoria.activo">
                  {{ categoria.activo ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
              <td>
                <button class="btn btn-sm btn-primary" (click)="editar(categoria)">
                  <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" (click)="eliminar(categoria.id!)">
                  <i class="fas fa-trash"></i>
                </button>
              </td>
            </tr>
            <tr *ngIf="categorias.length === 0">
              <td colspan="5" style="text-align: center;">No hay categorías registradas</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class CategoriasComponent implements OnInit {
  categorias: Categoria[] = [];
  categoriaSeleccionada: Categoria = this.nuevaCategoria();
  mostrandoFormulario = false;

  constructor(
    private readonly categoriaService: CategoriaService,
    private readonly cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.cargarCategorias();
  }

  cargarCategorias(): void {
    this.categoriaService.obtenerTodas().subscribe({
      next: (data) => {
        this.categorias = data;
        this.cdr.detectChanges();
      },
      error: () => AppLogger.error('Error al cargar categorías')
    });
  }

  mostrarFormulario(): void {
    this.categoriaSeleccionada = this.nuevaCategoria();
    this.mostrandoFormulario = true;
  }

  editar(categoria: Categoria): void {
    this.categoriaSeleccionada = { ...categoria };
    this.mostrandoFormulario = true;
  }

  guardar(): void {
    if (this.categoriaSeleccionada.id) {
      this.categoriaService.actualizar(this.categoriaSeleccionada.id, this.categoriaSeleccionada)
        .subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Categoría actualizada',
              timer: 1500,
              showConfirmButton: false
            });
            this.cargarCategorias();
            this.cdr.detectChanges();
            this.cancelar();
          },
          error: () => AppLogger.error('Error al actualizar categoría')
        });
    } else {
      this.categoriaService.crear(this.categoriaSeleccionada).subscribe({
        next: () => {
          Swal.fire({
            icon: 'success',
            title: 'Categoría creada',
            timer: 1500,
            showConfirmButton: false
          });
          this.cargarCategorias();
          this.cdr.detectChanges();
          this.cancelar();
        },
        error: () => AppLogger.error('Error al crear categoría')
      });
    }
  }

  eliminar(id: number): void {
    Swal.fire({
      title: '¿Eliminar categoría?',
      text: 'Esta acción no se puede deshacer',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.categoriaService.eliminar(id).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Categoría eliminada',
              timer: 1500,
              showConfirmButton: false
            });
            this.cargarCategorias();
            this.cdr.detectChanges();
          },
          error: () => AppLogger.error('Error al eliminar categoría')
        });
      }
    });
  }

  cancelar(): void {
    this.categoriaSeleccionada = this.nuevaCategoria();
    this.mostrandoFormulario = false;
  }

  cerrarAlHacerClick(event: MouseEvent): void {
    if ((event.target as Element).classList.contains('modal-overlay')) {
      this.cancelar();
    }
  }

  private nuevaCategoria(): Categoria {
    return {
      nombre: '',
      descripcion: '',
      activo: true
    };
  }
}
