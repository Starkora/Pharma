import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsuarioService } from '../../services/usuario.service';
import { AuthService } from '../../services/auth.service';
import { RolService, Rol } from '../../services/rol.service';
import { AppLogger } from '../../services/app-logger.service';
import { Usuario } from '../../models/auth.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card">
      <div class="card-header">
        <h2><i class="fas fa-users-cog"></i> Gestión de Usuarios</h2>
        <button class="btn btn-primary" (click)="abrirNuevo()">
          <i class="fas fa-plus"></i> Nuevo Usuario
        </button>
      </div>

      <!-- Modal Formulario Usuario -->
      <div class="modal-overlay" *ngIf="mostrandoModal" (click)="cerrarAlHacerClick($event)">
        <div class="modal-container modal-lg">
          <div class="modal-header">
            <h3><i class="fas fa-user-edit"></i> {{ usuarioSeleccionado.id ? 'Editar' : 'Nuevo' }} Usuario</h3>
            <button class="btn-close-modal" (click)="cancelar()"><i class="fas fa-times"></i></button>
          </div>
          <div class="modal-body">
            <form #usuarioForm="ngForm">
              <div class="grid grid-2">
                <div class="form-group">
                  <label>Usuario (login) *</label>
                  <input type="text" class="form-control"
                    [(ngModel)]="usuarioSeleccionado.username" name="username" required
                    [disabled]="!!usuarioSeleccionado.id"
                    placeholder="ej: jperez">
                  <small *ngIf="!!usuarioSeleccionado.id" style="color:#999;">
                    El nombre de usuario no se puede cambiar
                  </small>
                </div>
                <div class="form-group">
                  <label>{{ usuarioSeleccionado.id ? 'Nueva Contraseña' : 'Contraseña *' }}</label>
                  <input type="password" class="form-control"
                    [(ngModel)]="nuevoPassword" name="password"
                    [required]="!usuarioSeleccionado.id"
                    minlength="6"
                    placeholder="{{ usuarioSeleccionado.id ? 'Dejar vacío para no cambiar' : 'Mínimo 6 caracteres' }}">
                </div>
                <div class="form-group">
                  <label>Nombre *</label>
                  <input type="text" class="form-control"
                    [(ngModel)]="usuarioSeleccionado.nombre" name="nombre" required>
                </div>
                <div class="form-group">
                  <label>Apellido *</label>
                  <input type="text" class="form-control"
                    [(ngModel)]="usuarioSeleccionado.apellido" name="apellido" required>
                </div>
                <div class="form-group">
                  <label>Email</label>
                  <input type="email" class="form-control"
                    [(ngModel)]="usuarioSeleccionado.email" name="email">
                </div>
                <div class="form-group">
                  <label>Teléfono</label>
                  <input type="text" class="form-control"
                    [(ngModel)]="usuarioSeleccionado.telefono" name="telefono"
                    placeholder="999 999 999">
                </div>
                <div class="form-group">
                  <label>Estado</label>
                  <select class="form-control" [(ngModel)]="usuarioSeleccionado.activo" name="activo">
                    <option [value]="true">Activo</option>
                    <option [value]="false">Inactivo</option>
                  </select>
                </div>
              </div>

              <!-- Roles -->
              <div class="form-group mt-2">
                <label><i class="fas fa-shield-alt"></i> Roles del Usuario</label>

                <!-- Lista de roles de la BD -->
                <div class="roles-grid" *ngIf="roles.length > 0; else cargandoRoles">
                  <label *ngFor="let rol of roles" class="rol-item">
                    <input type="checkbox"
                      [checked]="tieneRol(rol.nombre)"
                      (change)="toggleRol(rol.nombre, $event)">
                    <span class="badge rol-badge" [style.background]="colorRol(rol.nombre)"
                          [title]="rol.descripcion || ''">
                      {{ rol.nombre }}
                    </span>
                    <button type="button" class="btn-del-rol" (click)="eliminarRolCatalogo(rol, $event)"
                      title="Eliminar del catálogo">
                      <i class="fas fa-times"></i>
                    </button>
                  </label>
                </div>
                <ng-template #cargandoRoles>
                  <p style="color:#999; padding:8px;"><i class="fas fa-spinner fa-spin"></i> Cargando roles...</p>
                </ng-template>

                <!-- Agregar nuevo rol -->
                <div class="add-role-row">
                  <input type="text" class="form-control add-role-input"
                    [(ngModel)]="nuevoRolInput" name="nuevoRolInput"
                    placeholder="Nombre (Ej: ROLE_SUPERVISOR)"
                    (keyup.enter)="agregarNuevoRol()">
                  <input type="text" class="form-control add-role-input"
                    [(ngModel)]="nuevoRolDesc" name="nuevoRolDesc"
                    placeholder="Descripción (opcional)"
                    (keyup.enter)="agregarNuevoRol()">
                  <button type="button" class="btn btn-sm btn-outline-primary" (click)="agregarNuevoRol()"
                    [disabled]="!nuevoRolInput.trim() || guardandoRol">
                    <i class="fas" [class.fa-plus]="!guardandoRol" [class.fa-spinner]="guardandoRol"
                       [class.fa-spin]="guardandoRol"></i>
                    {{ guardandoRol ? 'Guardando...' : 'Crear rol' }}
                  </button>
                </div>
                <small style="color:#999;">
                  <i class="fas fa-info-circle"></i>
                  Los nuevos roles se reflejarán inmediatamente en la lista de roles.
                </small>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" (click)="cancelar()">Cancelar</button>
            <button type="button" class="btn btn-success"
              [disabled]="!usuarioForm.valid || usuarioSeleccionado.roles.length === 0"
              (click)="guardar()">
              <i class="fas fa-save"></i> Guardar
            </button>
          </div>
        </div>
      </div>

      <!-- Modal Cambiar Contraseña -->
      <div class="modal-overlay" *ngIf="mostrandoCambioPassword" (click)="cerrarPasswordModal($event)">
        <div class="modal-container" style="max-width:420px;">
          <div class="modal-header">
            <h3><i class="fas fa-key"></i> Cambiar Contraseña</h3>
            <button class="btn-close-modal" (click)="mostrandoCambioPassword = false">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="modal-body">
            <p style="margin-bottom:10px; color:#666;">
              Usuario: <strong>{{ usuarioParaPassword?.username }}</strong>
            </p>
            <div class="form-group">
              <label>Nueva Contraseña *</label>
              <input type="password" class="form-control" [(ngModel)]="passwordNuevo"
                placeholder="Mínimo 6 caracteres">
            </div>
            <div class="form-group">
              <label>Confirmar Contraseña *</label>
              <input type="password" class="form-control" [(ngModel)]="passwordConfirmar"
                placeholder="Repite la contraseña">
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" (click)="mostrandoCambioPassword = false">Cancelar</button>
            <button class="btn btn-warning" (click)="confirmarCambioPassword()"
              [disabled]="!passwordNuevo || passwordNuevo.length < 6 || passwordNuevo !== passwordConfirmar">
              <i class="fas fa-key"></i> Cambiar
            </button>
          </div>
        </div>
      </div>

      <!-- Tabla de usuarios -->
      <div class="table-container mt-2">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Usuario</th>
              <th>Nombre Completo</th>
              <th>Email</th>
              <th>Roles</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let u of usuarios">
              <td>{{ u.id }}</td>
              <td><i class="fas fa-user" style="margin-right:6px;color:#888;"></i>{{ u.username }}</td>
              <td>{{ u.nombre }} {{ u.apellido }}</td>
              <td>{{ u.email || '-' }}</td>
              <td>
                <span *ngFor="let rol of u.roles" class="badge rol-badge"
                      [style.background]="colorRol(rol)"
                      style="margin-right:4px; font-size:0.75rem;">
                  {{ rol }}
                </span>
              </td>
              <td>
                <span class="badge" [class.badge-success]="u.activo" [class.badge-danger]="!u.activo">
                  {{ u.activo ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
              <td>
                <button class="btn btn-sm btn-primary" (click)="editar(u)" title="Editar">
                  <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-warning" (click)="abrirCambioPassword(u)" title="Cambiar contraseña">
                  <i class="fas fa-key"></i>
                </button>
                <button class="btn btn-sm btn-danger" (click)="eliminar(u.id!)"
                  [disabled]="u.id === currentUserId" title="Eliminar">
                  <i class="fas fa-trash"></i>
                </button>
              </td>
            </tr>
            <tr *ngIf="usuarios.length === 0">
              <td colspan="7" style="text-align:center; color:#999;">
                <i class="fas fa-inbox"></i> No hay usuarios registrados
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .roles-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      padding: 12px;
      background: #f8f9fa;
      border-radius: 8px;
      border: 1px solid #e8ecf0;
      max-height: 200px;
      overflow-y: auto;
    }
    .rol-item {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
    }
    .rol-item input[type="checkbox"] {
      width: 16px; height: 16px; cursor: pointer;
    }
    .rol-badge {
      color: #fff;
      padding: 4px 10px;
      border-radius: 4px;
      font-size: 0.8rem;
      font-weight: 500;
    }
    .add-role-row {
      display: flex;
      gap: 8px;
      margin-top: 10px;
      align-items: center;
    }
    .btn-del-rol {
      background: none;
      border: none;
      color: #bbb;
      cursor: pointer;
      padding: 0 2px;
      font-size: 0.75rem;
      line-height: 1;
    }
    .btn-del-rol:hover { color: #e74c3c; }
  `]
})
export class UsuariosComponent implements OnInit {

  usuarios: Usuario[] = [];
  usuarioSeleccionado: Usuario = this.nuevoUsuario();
  nuevoPassword = '';
  mostrandoModal = false;

  mostrandoCambioPassword = false;
  usuarioParaPassword: Usuario | null = null;
  passwordNuevo = '';
  passwordConfirmar = '';

  roles: Rol[] = [];
  nuevoRolInput = '';
  nuevoRolDesc = '';
  guardandoRol = false;

  currentUserId: number | undefined;

  // Paleta de colores generada deterministicamente por nombre de rol
  private readonly PALETA = [
    '#9b59b6','#3498db','#2ecc71','#e67e22','#e74c3c',
    '#1abc9c','#2980b9','#8e44ad','#27ae60','#d35400',
    '#c0392b','#16a085','#2c3e50','#7f8c8d','#f39c12'
  ];

  constructor(
    private usuarioService: UsuarioService,
    private rolService: RolService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.authService.currentUserValue?.id;
    this.cargarUsuarios();
    this.cargarRoles();
  }

  cargarRoles(): void {
    this.rolService.obtenerTodos().subscribe({
      next: (data) => {
        this.roles = data;
        this.cdr.detectChanges();
      },
      error: () => AppLogger.error('Error al cargar roles')
    });
  }

  cargarUsuarios(): void {
    this.usuarioService.obtenerTodos().subscribe({
      next: (data) => {
        this.usuarios = data;
        this.cdr.detectChanges();
      },
      error: () => AppLogger.error('Error al cargar usuarios')
    });
  }

  agregarNuevoRol(): void {
    const nombre = this.nuevoRolInput.trim().toUpperCase();
    if (!nombre) return;

    // Si ya existe en el catálogo, solo asignarlo
    const existente = this.roles.find(r => r.nombre === nombre);
    if (existente) {
      this.toggleRolPorNombre(nombre, true);
      this.nuevoRolInput = '';
      return;
    }

    // Crear en BD
    this.guardandoRol = true;
    this.rolService.crear({ nombre, descripcion: this.nuevoRolDesc.trim() || undefined }).subscribe({
      next: (rolCreado) => {
        this.roles = [...this.roles, rolCreado].sort((a, b) => a.nombre.localeCompare(b.nombre));
        this.toggleRolPorNombre(rolCreado.nombre, true);
        this.nuevoRolInput = '';
        this.nuevoRolDesc = '';
        this.guardandoRol = false;
      },
      error: (err) => {
        this.guardandoRol = false;
        Swal.fire({ icon: 'error', title: 'Error', text: err?.error?.error || 'No se pudo crear el rol' });
      }
    });
  }

  eliminarRolCatalogo(rol: Rol, event: MouseEvent): void {
    event.stopPropagation();
    Swal.fire({
      title: `¿Eliminar rol "${rol.nombre}"?`,
      text: 'Se eliminará del catálogo, pero los usuarios que ya lo tienen lo conservarán.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {
        this.rolService.eliminar(rol.id!).subscribe({
          next: () => {
            this.roles = this.roles.filter(r => r.id !== rol.id);
          },
          error: err => Swal.fire({ icon: 'error', title: 'Error', text: err?.error?.error || 'No se pudo eliminar' })
        });
      }
    });
  }

  private toggleRolPorNombre(nombre: string, checked: boolean): void {
    if (checked && !this.usuarioSeleccionado.roles.includes(nombre)) {
      this.usuarioSeleccionado.roles = [...this.usuarioSeleccionado.roles, nombre];
    } else if (!checked) {
      this.usuarioSeleccionado.roles = this.usuarioSeleccionado.roles.filter(r => r !== nombre);
    }
  }

  abrirNuevo(): void {
    this.usuarioSeleccionado = this.nuevoUsuario();
    this.nuevoPassword = '';
    this.nuevoRolInput = '';
    this.nuevoRolDesc = '';
    this.mostrandoModal = true;
  }

  editar(usuario: Usuario): void {
    this.usuarioSeleccionado = { ...usuario, roles: [...usuario.roles] };
    this.nuevoPassword = '';
    this.nuevoRolInput = '';
    this.nuevoRolDesc = '';
    this.mostrandoModal = true;
  }

  cancelar(): void {
    this.mostrandoModal = false;
    this.usuarioSeleccionado = this.nuevoUsuario();
    this.nuevoPassword = '';
    this.nuevoRolInput = '';
    this.nuevoRolDesc = '';
  }

  cerrarAlHacerClick(event: MouseEvent): void {
    if ((event.target as Element).classList.contains('modal-overlay')) {
      this.cancelar();
    }
  }

  cerrarPasswordModal(event: MouseEvent): void {
    if ((event.target as Element).classList.contains('modal-overlay')) {
      this.mostrandoCambioPassword = false;
    }
  }

  tieneRol(rol: string): boolean {
    return this.usuarioSeleccionado.roles.includes(rol);
  }

  toggleRol(rol: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked && !this.usuarioSeleccionado.roles.includes(rol)) {
      this.usuarioSeleccionado.roles = [...this.usuarioSeleccionado.roles, rol];
    } else if (!checked) {
      this.usuarioSeleccionado.roles = this.usuarioSeleccionado.roles.filter(r => r !== rol);
    }
  }

  guardar(): void {
    const payload: Usuario = { ...this.usuarioSeleccionado };

    if (!this.usuarioSeleccionado.id) {
      // Nuevo usuario: incluir password
      payload.password = this.nuevoPassword;
    } else if (this.nuevoPassword) {
      // Editar: password solo si se llenó
      payload.password = this.nuevoPassword;
    }

    const op = this.usuarioSeleccionado.id
      ? this.usuarioService.actualizar(this.usuarioSeleccionado.id, payload)
      : this.usuarioService.crear(payload);

    op.subscribe({
      next: () => {
        Swal.fire({
          icon: 'success',
          title: this.usuarioSeleccionado.id ? 'Usuario actualizado' : 'Usuario creado',
          timer: 1500, showConfirmButton: false
        });
        this.cargarUsuarios();
        this.cancelar();
      },
      error: (err) => {
        const msg = err?.error?.error || 'Error al guardar usuario';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  abrirCambioPassword(usuario: Usuario): void {
    this.usuarioParaPassword = usuario;
    this.passwordNuevo = '';
    this.passwordConfirmar = '';
    this.mostrandoCambioPassword = true;
  }

  confirmarCambioPassword(): void {
    if (!this.usuarioParaPassword || this.passwordNuevo !== this.passwordConfirmar) return;

    this.usuarioService.cambiarPassword(this.usuarioParaPassword.id!, this.passwordNuevo).subscribe({
      next: () => {
        Swal.fire({ icon: 'success', title: 'Contraseña actualizada', timer: 1500, showConfirmButton: false });
        this.mostrandoCambioPassword = false;
      },
      error: err => {
        const msg = err?.error?.error || 'Error al cambiar contraseña';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  eliminar(id: number): void {
    Swal.fire({
      title: '¿Eliminar usuario?',
      text: 'Esta acción no se puede deshacer',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {
        this.usuarioService.eliminar(id).subscribe({
          next: () => {
            Swal.fire({ icon: 'success', title: 'Usuario eliminado', timer: 1500, showConfirmButton: false });
            this.cargarUsuarios();
          },
          error: err => {
            const msg = err?.error?.error || 'No se pudo eliminar el usuario';
            Swal.fire({ icon: 'error', title: 'Error', text: msg });
          }
        });
      }
    });
  }

  // Genera un color consistente para cualquier nombre de rol
  colorRol(rol: string): string {
    let hash = 0;
    for (let i = 0; i < rol.length; i++) {
      hash = rol.charCodeAt(i) + ((hash << 5) - hash);
    }
    return this.PALETA[Math.abs(hash) % this.PALETA.length];
  }

  private nuevoUsuario(): Usuario {
    return { username: '', nombre: '', apellido: '', email: '', activo: true, roles: [] };
  }
}
