import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <h1>PharmaSys</h1>
          <p>Sistema de Gestión de Farmacia</p>
        </div>
        
        <form (ngSubmit)="onSubmit()" #loginForm="ngForm">
          <div class="form-group">
            <label for="username">Usuario</label>
            <input 
              type="text" 
              id="username"
              class="form-control" 
              [(ngModel)]="username" 
              name="username"
              required
              placeholder="Ingrese su usuario"
              autofocus>
          </div>
          
          <div class="form-group">
            <label for="password">Contraseña</label>
            <input 
              type="password" 
              id="password"
              class="form-control" 
              [(ngModel)]="password" 
              name="password"
              required
              placeholder="Ingrese su contraseña">
          </div>
          
          <div *ngIf="error" class="alert alert-error">
            <i class="fas fa-exclamation-circle"></i> {{ error }}
          </div>
          
          <button 
            type="submit" 
            class="btn btn-primary btn-block" 
            [disabled]="loading || !loginForm.valid">
            <i class="fas" [class.fa-spinner]="loading" [class.fa-spin]="loading" [class.fa-sign-in-alt]="!loading"></i>
            {{ loading ? 'Iniciando sesión...' : 'Iniciar Sesión' }}
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 20px;
    }
    
    .login-card {
      background: white;
      border-radius: 10px;
      box-shadow: 0 10px 40px rgba(0,0,0,0.2);
      padding: 40px;
      width: 100%;
      max-width: 400px;
    }
    
    .login-header {
      text-align: center;
      margin-bottom: 30px;
    }
    
    .login-header h1 {
      color: #667eea;
      margin: 0 0 10px 0;
      font-size: 2.5em;
    }
    
    .login-header p {
      color: #666;
      margin: 0;
      font-size: 1.1em;
    }
    
    .form-group {
      margin-bottom: 20px;
    }
    
    .form-group label {
      display: block;
      margin-bottom: 5px;
      color: #333;
      font-weight: 500;
    }
    
    .form-control {
      width: 100%;
      padding: 12px;
      border: 1px solid #ddd;
      border-radius: 5px;
      font-size: 1em;
      box-sizing: border-box;
    }
    
    .form-control:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }
    
    .btn-block {
      width: 100%;
      padding: 12px;
      margin-top: 10px;
    }
    
    .alert-error {
      background-color: #fee;
      border: 1px solid #fcc;
      color: #c33;
      padding: 12px;
      border-radius: 5px;
      margin-bottom: 15px;
    }
    
    .login-footer {
      margin-top: 30px;
      padding-top: 20px;
      border-top: 1px solid #eee;
      text-align: center;
      color: #666;
      font-size: 0.9em;
    }
    
    .login-footer p {
      margin: 5px 0;
    }
  `]
})
export class LoginComponent implements OnInit {
  username = '';
  password = '';
  loading = false;
  error = '';
  returnUrl = '/';

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
  }

  ngOnInit(): void {
    // Redirección rápida si ya hay sesión local vigente.
    // El interceptor global se encargará de limpiarla si el token expiró.
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/']);
    }
  }

  onSubmit(): void {
    if (!this.username || !this.password) {
      return;
    }

    this.loading = true;
    this.error = '';

    this.authService.login(this.username, this.password).subscribe({
      next: (response) => {
        this.loading = false;
        Swal.fire({
          icon: 'success',
          title: '¡Bienvenido!',
          text: `Hola ${response.usuario?.nombre}`,
          timer: 1200,
          showConfirmButton: false
        }).then(() => {
          this.router.navigate([this.returnUrl]);
        });
      },
      error: (err) => {
        this.error = err.error?.message || 'Error de conexión. Verifique que el servidor esté activo.';
        this.loading = false;
        Swal.fire({
          icon: 'error',
          title: 'Error de conexión',
          text: this.error,
          confirmButtonText: 'Entendido'
        });
      }
    });
  }
}
