import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoService } from '../../services/producto.service';
import { CategoriaService } from '../../services/categoria.service';
import { ProveedorService } from '../../services/proveedor.service';
import { AppLogger } from '../../services/app-logger.service';
import { Producto, Categoria, Proveedor } from '../../models/models';
import { SolesPipe } from '../../pipes/soles.pipe';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [CommonModule, FormsModule, SolesPipe],
  template: `
    <div class="card">
      <div class="card-header">
        <h2>Gestión de Productos</h2>
        <button class="btn btn-primary" (click)="abrirNuevo()">
          <i class="fas fa-plus"></i> Nuevo Producto
        </button>
      </div>
      
      <!-- Modal Formulario -->
      <div class="modal-overlay" *ngIf="mostrarFormulario" (click)="cerrarAlHacerClick($event)">
        <div class="modal-container modal-lg">
          <div class="modal-header">
            <h3><i class="fas fa-box"></i> {{ productoSeleccionado.id ? 'Editar' : 'Nuevo' }} Producto</h3>
            <button class="btn-close-modal" (click)="cancelar()" title="Cerrar">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="modal-body">
            <form (ngSubmit)="guardar()" #productoForm="ngForm">
              <div class="grid grid-2">
                <div class="form-group">
                  <label>Código *</label>
                  <input type="text" class="form-control" [(ngModel)]="productoSeleccionado.codigo" name="codigo" required>
                </div>
                <div class="form-group">
                  <label>Nombre *</label>
                  <input type="text" class="form-control" [(ngModel)]="productoSeleccionado.nombre" name="nombre" required>
                </div>
                <div class="form-group">
                  <label>Categoría</label>
                  <select class="form-control" [(ngModel)]="productoSeleccionado.categoria" [compareWith]="compararPorId" name="categoria">
                    <option [ngValue]="null">Seleccione...</option>
                    <option *ngFor="let cat of categorias" [ngValue]="cat">{{ cat.nombre }}</option>
                  </select>
                </div>
                <div class="form-group">
                  <label>Proveedor</label>
                  <select class="form-control" [(ngModel)]="productoSeleccionado.proveedor" [compareWith]="compararPorId" name="proveedor">
                    <option [ngValue]="null">Seleccione...</option>
                    <option *ngFor="let prov of proveedores" [ngValue]="prov">{{ prov.nombre }}</option>
                  </select>
                </div>
                <div class="form-group">
                  <label>Precio Compra *</label>
                  <input type="number" step="0.01" class="form-control" [(ngModel)]="productoSeleccionado.precioCompra" name="precioCompra" required>
                </div>
                <div class="form-group">
                  <label>Precio Venta *</label>
                  <input type="number" step="0.01" class="form-control" [(ngModel)]="productoSeleccionado.precioVenta" name="precioVenta" required>
                </div>
                <div class="form-group">
                  <label>Stock *</label>
                  <input type="number" class="form-control" [(ngModel)]="productoSeleccionado.stock" name="stock" required>
                </div>
                <div class="form-group">
                  <label>Stock Mínimo *</label>
                  <input type="number" class="form-control" [(ngModel)]="productoSeleccionado.stockMinimo" name="stockMinimo" required>
                </div>
                <div class="form-group">
                  <label>Unidad de Medida</label>
                  <input type="text" class="form-control" [(ngModel)]="productoSeleccionado.unidadMedida" name="unidadMedida">
                </div>
                <div class="form-group">
                  <label>
                    <input type="checkbox" [(ngModel)]="productoSeleccionado.requiereReceta" name="requiereReceta">
                    Requiere Receta
                  </label>
                </div>
                <div class="form-group" style="grid-column: span 2;">
                  <label>Descripción</label>
                  <textarea class="form-control" [(ngModel)]="productoSeleccionado.descripcion" name="descripcion" rows="3"></textarea>
                </div>
                <div class="form-group" style="grid-column: span 2;">
                  <label>Imagen del Producto</label>
                  <div class="image-upload-container">
                    <input type="file" 
                           #imagenInput
                           class="form-control" 
                           accept="image/*" 
                           (change)="onImagenSeleccionada($event)"
                           name="imagen">
                    <div *ngIf="imagenPreview" class="image-preview" style="margin-top: 10px;">
                      <img [src]="imagenPreview" [alt]="productoSeleccionado.nombre" style="max-width: 200px; max-height: 200px; border-radius: 5px;">
                    </div>
                    <div *ngIf="!imagenPreview && productoSeleccionado.imagenUrl" class="image-preview" style="margin-top: 10px;">
                      <img [src]="productoSeleccionado.imagenUrl" [alt]="productoSeleccionado.nombre" style="max-width: 200px; max-height: 200px; border-radius: 5px;">
                    </div>
                  </div>
                </div>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" (click)="cancelar()">Cancelar</button>
            <button type="button" class="btn btn-success" [disabled]="!productoForm.valid" (click)="guardar()">
              <i class="fas fa-save"></i> Guardar
            </button>
          </div>
        </div>
      </div>
      
      <!-- Búsqueda -->
      <div class="mt-2">
        <input type="text" 
               class="form-control" 
               placeholder="Buscar producto..." 
               [(ngModel)]="terminoBusqueda"
               (ngModelChange)="buscar()">
      </div>
      
      <!-- Tabla -->
      <div class="mt-2">
        <table>
          <thead>
            <tr>
              <th>Código</th>
              <th>Nombre</th>
              <th>Categoría</th>
              <th>Precio Venta</th>
              <th>Stock</th>
              <th>Stock Mín.</th>
              <th>Imagen</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let producto of productos" 
                [style.background-color]="producto.stock <= producto.stockMinimo ? '#ffebee' : ''">
              <td>{{ producto.codigo }}</td>
              <td>{{ producto.nombre }}</td>
              <td>{{ producto.categoria?.nombre || 'N/A' }}</td>
              <td>{{ producto.precioVenta | soles }}</td>
              <td>{{ producto.stock }}</td>
              <td>{{ producto.stockMinimo }}</td>
              <td>
                <div *ngIf="producto.imagenUrl; else sinImagen" style="display:flex; align-items:center; gap:8px;">
                  <img
                    [src]="producto.imagenUrl"
                    [alt]="producto.nombre"
                    style="width:42px; height:42px; object-fit:cover; border-radius:6px; border:1px solid #dcdcdc;"
                  >
                  <button class="btn btn-info" style="padding:4px 8px; font-size:12px;" (click)="verImagen(producto)">Ver imagen</button>
                </div>
                <ng-template #sinImagen>
                  <span style="color:#8a8a8a; font-size:13px;">Sin imagen</span>
                </ng-template>
              </td>
              <td>
                <span [style.color]="producto.activo ? '#2ecc71' : '#e74c3c'">
                  {{ producto.activo ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
              <td>
                <button class="btn btn-sm btn-warning" (click)="editar(producto)" title="Editar">
                  <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" (click)="eliminar(producto.id!)" title="Eliminar">
                  <i class="fas fa-trash"></i>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        
        <div *ngIf="productos.length === 0" class="text-center mt-2">
          <p>No hay productos registrados</p>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class ProductosComponent implements OnInit {
  productos: Producto[] = [];
  categorias: Categoria[] = [];
  proveedores: Proveedor[] = [];
  productoSeleccionado: Producto = this.nuevoProducto();
  mostrarFormulario = false;
  terminoBusqueda = '';
  imagenSeleccionada: File | null = null;
  imagenPreview: string | null = null;

  constructor(
    private readonly productoService: ProductoService,
    private readonly categoriaService: CategoriaService,
    private readonly proveedorService: ProveedorService,
    private readonly cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.cargarProductos();
    this.cargarCategorias();
    this.cargarProveedores();
  }

  cargarProductos(): void {
    this.productoService.obtenerTodos().subscribe({
      next: (data) => {
        this.productos = data;
        this.cdr.detectChanges();
      },
      error: () => AppLogger.error('Error al cargar productos')
    });
  }

  cargarCategorias(): void {
    this.categoriaService.obtenerActivas().subscribe({
      next: (data) => {
        this.categorias = data;
        this.cdr.detectChanges();
      },
      error: () => AppLogger.error('Error al cargar categorías')
    });
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

  buscar(): void {
    if (this.terminoBusqueda.trim()) {
      this.productoService.buscarPorNombre(this.terminoBusqueda).subscribe({
        next: (data) => {
          this.productos = data;
          this.cdr.detectChanges();
        },
        error: () => AppLogger.error('Error al buscar productos')
      });
    } else {
      this.cargarProductos();
    }
  }

  guardar(): void {
    if (this.productoSeleccionado.id) {
      // Actualizar existente
      if (this.imagenSeleccionada) {
        this.productoService.actualizarConImagen(this.productoSeleccionado.id, this.productoSeleccionado, this.imagenSeleccionada).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Producto actualizado',
              text: 'El producto se actualizó correctamente',
              timer: 2000,
              showConfirmButton: false
            });
            this.cargarProductos();
            this.cdr.detectChanges();
            this.cancelar();
          },
          error: () => AppLogger.error('Error al actualizar producto')
        });
      } else {
        this.productoService.actualizar(this.productoSeleccionado.id, this.productoSeleccionado).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Producto actualizado',
              text: 'El producto se actualizó correctamente',
              timer: 2000,
              showConfirmButton: false
            });
            this.cargarProductos();
            this.cdr.detectChanges();
            this.cancelar();
          },
          error: () => AppLogger.error('Error al actualizar producto')
        });
      }
    } else if (this.imagenSeleccionada) {
      // Crear nuevo con imagen
      this.productoService.crearConImagen(this.productoSeleccionado, this.imagenSeleccionada).subscribe({
        next: () => {
          Swal.fire({
            icon: 'success',
            title: 'Producto creado',
            text: 'El producto se creó correctamente',
            timer: 2000,
            showConfirmButton: false
          });
          this.cargarProductos();
          this.cdr.detectChanges();
          this.cancelar();
        },
        error: () => AppLogger.error('Error al crear producto')
      });
    } else {
      // Crear nuevo sin imagen
      this.productoService.crear(this.productoSeleccionado).subscribe({
        next: () => {
          Swal.fire({
            icon: 'success',
            title: 'Producto creado',
            text: 'El producto se creó correctamente',
            timer: 2000,
            showConfirmButton: false
          });
          this.cargarProductos();
          this.cdr.detectChanges();
          this.cancelar();
        },
        error: () => AppLogger.error('Error al crear producto')
      });
    }
  }

  editar(producto: Producto): void {
    this.productoSeleccionado = { ...producto };
    this.sincronizarRelacionesSeleccionadas();
    this.mostrarFormulario = true;
  }

  compararPorId = (a: { id?: number } | null, b: { id?: number } | null): boolean => {
    if (a === b) return true;
    if (!a || !b) return false;
    return a.id === b.id;
  };

  private sincronizarRelacionesSeleccionadas(): void {
    const categoriaId = this.productoSeleccionado.categoria?.id;
    if (categoriaId != null) {
      const categoriaRef = this.categorias.find(c => c.id === categoriaId);
      if (categoriaRef) this.productoSeleccionado.categoria = categoriaRef;
    }

    const proveedorId = this.productoSeleccionado.proveedor?.id;
    if (proveedorId != null) {
      const proveedorRef = this.proveedores.find(p => p.id === proveedorId);
      if (proveedorRef) this.productoSeleccionado.proveedor = proveedorRef;
    }
  }

  verImagen(producto: Producto): void {
    if (!producto.imagenUrl) {
      Swal.fire({
        icon: 'info',
        title: 'Sin imagen',
        text: 'Este producto no tiene una imagen registrada.'
      });
      return;
    }

    Swal.fire({
      title: producto.nombre,
      imageUrl: producto.imagenUrl,
      imageAlt: producto.nombre,
      width: 600,
      confirmButtonText: 'Cerrar'
    });
  }

  eliminar(id: number): void {
    Swal.fire({
      title: '¿Eliminar producto?',
      text: 'Esta acción no se puede deshacer',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.productoService.eliminar(id).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Eliminado',
              text: 'Producto eliminado correctamente',
              timer: 2000,
              showConfirmButton: false
            });
            this.cargarProductos();
            this.cdr.detectChanges();
          },
          error: (error) => {
            AppLogger.error('Error al eliminar producto');
            Swal.fire({
              icon: 'error',
              title: 'Error',
              text: 'No se pudo eliminar el producto'
            });
          }
        });
      }
    });
  }

  cancelar(): void {
    this.productoSeleccionado = this.nuevoProducto();
    this.imagenSeleccionada = null;
    this.imagenPreview = null;
    this.mostrarFormulario = false;
  }

  abrirNuevo(): void {
    this.productoSeleccionado = this.nuevoProducto();
    this.imagenSeleccionada = null;
    this.imagenPreview = null;
    this.mostrarFormulario = true;
  }

  cerrarAlHacerClick(event: MouseEvent): void {
    if ((event.target as Element).classList.contains('modal-overlay')) {
      this.cancelar();
    }
  }

  onImagenSeleccionada(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    
    if (file) {
      // Validar que sea una imagen
      if (!file.type.startsWith('image/')) {
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: 'Por favor seleccione una imagen válida'
        });
        return;
      }

      // Validar tamaño (máximo 5MB)
      if (file.size > 5 * 1024 * 1024) {
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: 'La imagen no debe exceder 5MB'
        });
        return;
      }

      this.imagenSeleccionada = file;

      // Crear preview
      const reader = new FileReader();
      reader.onload = (e) => {
        this.imagenPreview = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  private nuevoProducto(): Producto {
    return {
      codigo: '',
      nombre: '',
      precioCompra: 0,
      precioVenta: 0,
      stock: 0,
      stockMinimo: 0,
      activo: true
    };
  }
}
