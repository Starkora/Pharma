import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './services/auth.service';
import { Usuario } from './models/auth.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div *ngIf="currentUser">
      <header class="app-header">
        <div class="header-content">
          <h1>PharmaSys</h1>
          <div class="user-info">
            <span><i class="fas fa-user-circle"></i> {{ currentUser.nombre }} {{ currentUser.apellido }}</span>
            <button class="btn btn-logout" (click)="logout()"><i class="fas fa-sign-out-alt"></i> Cerrar Sesión</button>
          </div>
        </div>
      </header>
      
      <nav>
        <ul>
          <li><a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}"><i class="fas fa-home"></i> Inicio</a></li>
          <li><a routerLink="/productos" routerLinkActive="active"><i class="fas fa-pills"></i> Productos</a></li>
          <li><a routerLink="/ventas" routerLinkActive="active"><i class="fas fa-shopping-cart"></i> Ventas</a></li>
          <li><a routerLink="/compras" routerLinkActive="active"><i class="fas fa-box"></i> Compras</a></li>
          <li><a routerLink="/clientes" routerLinkActive="active"><i class="fas fa-users"></i> Clientes</a></li>
          <li><a routerLink="/proveedores" routerLinkActive="active"><i class="fas fa-building"></i> Proveedores</a></li>
          <li><a routerLink="/categorias" routerLinkActive="active"><i class="fas fa-list"></i> Categorías</a></li>
          <li><a routerLink="/reportes" routerLinkActive="active"><i class="fas fa-chart-bar"></i> Reportes</a></li>
          <li><a routerLink="/facturacion" routerLinkActive="active"><i class="fas fa-file-invoice"></i> Facturación</a></li>
          <li *ngIf="authService.hasRole('ROLE_ADMIN')"><a routerLink="/usuarios" routerLinkActive="active"><i class="fas fa-users-cog"></i> Usuarios</a></li>
        </ul>
      </nav>
    </div>
    
    <div class="container" [class.no-auth]="!currentUser">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    .app-header {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 0;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    }
    
    .header-content {
      max-width: 1400px;
      margin: 0 auto;
      padding: 15px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .header-content h1 {
      margin: 0;
      font-size: 1.8em;
    }
    
    .user-info {
      display: flex;
      align-items: center;
      gap: 15px;
    }
    
    .user-info span {
      font-size: 1em;
    }
    
    .btn-logout {
      background: rgba(255,255,255,0.2);
      color: white;
      border: 1px solid rgba(255,255,255,0.3);
      padding: 8px 16px;
      border-radius: 5px;
      cursor: pointer;
      transition: all 0.3s;
    }
    
    .btn-logout:hover {
      background: rgba(255,255,255,0.3);
    }
    
    .container.no-auth {
      padding: 0;
      max-width: 100%;
    }
  `]
})
export class AppComponent implements OnInit {
  title = 'PharmaSys';
  currentUser: Usuario | null = null;

  constructor(
    public authService: AuthService,
    private router: Router
  ) {
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
    });
  }

  ngOnInit(): void {
    // Evita un request de validación al arrancar que solo genera ruido si la
    // cookie ya expiró. La sesión se limpia por interceptor ante 401/403.
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login'])
    });
  }
}
