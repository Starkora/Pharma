import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-inicio',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="card">
      <div class="card-header">
        <h2>Bienvenido a PharmaSys</h2>
      </div>
      
      <div class="grid grid-4 mt-2">
        <div class="card text-center">
          <h3>Productos</h3>
          <p>Gestión completa de inventario</p>
          <a routerLink="/productos" class="btn btn-primary">Ir a Productos</a>
        </div>
        
        <div class="card text-center">
          <h3>Ventas</h3>
          <p>Procesar ventas y consultar historial</p>
          <a routerLink="/ventas" class="btn btn-success">Ir a Ventas</a>
        </div>
        
        <div class="card text-center">
          <h3>Clientes</h3>
          <p>Administrar información de clientes</p>
          <a routerLink="/clientes" class="btn btn-primary">Ir a Clientes</a>
        </div>
        
        <div class="card text-center">
          <h3>Reportes</h3>
          <p>Análisis y estadísticas del negocio</p>
          <a routerLink="/reportes" class="btn btn-warning">Ir a Reportes</a>
        </div>
      </div>
      
      <div class="mt-2">
        <h3>Resumen del Día</h3>
        <div class="grid grid-3 mt-1">
          <div class="card">
            <h4>Ventas Hoy</h4>
            <p style="font-size: 2rem; color: #2ecc71; margin: 10px 0;">S/ 0.00</p>
          </div>
          
          <div class="card">
            <h4>Productos Bajo Stock</h4>
            <p style="font-size: 2rem; color: #e74c3c; margin: 10px 0;">0</p>
          </div>
          
          <div class="card">
            <h4>Compras Pendientes</h4>
            <p style="font-size: 2rem; color: #f39c12; margin: 10px 0;">0</p>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .card h3 {
      color: #2c3e50;
      margin-bottom: 10px;
    }
    
    .card h4 {
      color: #34495e;
      margin-bottom: 5px;
    }
    
    .card p {
      color: #7f8c8d;
      margin-bottom: 15px;
    }
  `]
})
export class InicioComponent { }
