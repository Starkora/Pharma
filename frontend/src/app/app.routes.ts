import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./components/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: '',
    loadComponent: () => import('./components/inicio/inicio.component').then(m => m.InicioComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'productos',
    loadComponent: () => import('./components/productos/productos.component').then(m => m.ProductosComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'ventas',
    loadComponent: () => import('./components/ventas/ventas.component').then(m => m.VentasComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'compras',
    loadComponent: () => import('./components/compras/compras.component').then(m => m.ComprasComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'clientes',
    loadComponent: () => import('./components/clientes/clientes.component').then(m => m.ClientesComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'proveedores',
    loadComponent: () => import('./components/proveedores/proveedores.component').then(m => m.ProveedoresComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'categorias',
    loadComponent: () => import('./components/categorias/categorias.component').then(m => m.CategoriasComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'reportes',
    loadComponent: () => import('./components/reportes/reportes.component').then(m => m.ReportesComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'usuarios',
    loadComponent: () => import('./components/usuarios/usuarios.component').then(m => m.UsuariosComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'facturacion',
    loadComponent: () => import('./components/facturacion/facturacion.component').then(m => m.FacturacionComponent),
    canActivate: [AuthGuard]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
