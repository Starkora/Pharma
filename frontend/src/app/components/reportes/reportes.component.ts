import { Component, OnInit, OnDestroy, AfterViewInit, ElementRef, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VentaService } from '../../services/venta.service';
import { CompraService } from '../../services/compra.service';
import { ProductoService } from '../../services/producto.service';
import { AppLogger } from '../../services/app-logger.service';
import { SolesPipe } from '../../pipes/soles.pipe';
import { Venta, Compra, Producto } from '../../models/models';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

interface ProductoVendido {
  nombre: string;
  cantidad: number;
  total: number;
}

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule, SolesPipe],
  template: `
    <div class="card">
      <div class="card-header">
        <h2><i class="fas fa-chart-bar"></i> Reportes y Estadísticas</h2>
        <div style="display:flex; gap:10px; align-items:center;">
          <select class="form-control" style="width:auto;" [(ngModel)]="mesSeleccionado" (change)="cargarDatos()">
            <option *ngFor="let mes of meses; let i = index" [ngValue]="i + 1">{{ mes }}</option>
          </select>
          <select class="form-control" style="width:auto;" [(ngModel)]="anioSeleccionado" (change)="cargarDatos()">
            <option *ngFor="let a of anios" [ngValue]="a">{{ a }}</option>
          </select>
        </div>
      </div>

      <!-- KPIs principales -->
      <div class="grid grid-4 mt-2">
        <div class="kpi-card kpi-green">
          <div class="kpi-icon"><i class="fas fa-arrow-up"></i></div>
          <div class="kpi-data">
            <span class="kpi-label">Ventas del Mes</span>
            <span class="kpi-value">{{ totalVentas | soles }}</span>
            <span class="kpi-sub">{{ ventasMes.length }} transacciones</span>
          </div>
        </div>
        <div class="kpi-card kpi-red">
          <div class="kpi-icon"><i class="fas fa-arrow-down"></i></div>
          <div class="kpi-data">
            <span class="kpi-label">Compras del Mes</span>
            <span class="kpi-value">{{ totalCompras | soles }}</span>
            <span class="kpi-sub">{{ comprasMes.length }} órdenes</span>
          </div>
        </div>
        <div class="kpi-card" [class.kpi-blue]="utilidad >= 0" [class.kpi-red]="utilidad < 0">
          <div class="kpi-icon"><i class="fas fa-percentage"></i></div>
          <div class="kpi-data">
            <span class="kpi-label">Utilidad del Mes</span>
            <span class="kpi-value">{{ utilidad | soles }}</span>
            <span class="kpi-sub">Ventas - Compras</span>
          </div>
        </div>
        <div class="kpi-card kpi-orange">
          <div class="kpi-icon"><i class="fas fa-exclamation-triangle"></i></div>
          <div class="kpi-data">
            <span class="kpi-label">Stock Crítico</span>
            <span class="kpi-value">{{ productosBajoStock.length }}</span>
            <span class="kpi-sub">Productos bajo mínimo</span>
          </div>
        </div>
      </div>

      <!-- Gráficos -->
      <div class="grid grid-2 mt-2">
        <!-- Gráfico de Ventas por día -->
        <div class="card">
          <h3><i class="fas fa-chart-line"></i> Ventas Diarias del Mes</h3>
          <canvas #ventasChart style="max-height: 250px;"></canvas>
        </div>

        <!-- Gráfico Productos más vendidos -->
        <div class="card">
          <h3><i class="fas fa-chart-pie"></i> Productos Más Vendidos</h3>
          <canvas #productosChart style="max-height: 250px;"></canvas>
        </div>
      </div>

      <!-- Tabla Productos más vendidos -->
      <div class="card mt-2">
        <h3><i class="fas fa-trophy"></i> Ranking de Productos Más Vendidos</h3>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>#</th>
                <th>Producto</th>
                <th>Cantidad Vendida</th>
                <th>Total Recaudado</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let p of productosVendidos; let i = index">
                <td>
                  <span class="medal" [class.gold]="i===0" [class.silver]="i===1" [class.bronze]="i===2">
                    {{ i + 1 }}
                  </span>
                </td>
                <td>{{ p.nombre }}</td>
                <td>{{ p.cantidad }}</td>
                <td>{{ p.total | soles }}</td>
              </tr>
              <tr *ngIf="productosVendidos.length === 0">
                <td colspan="4" style="text-align:center; color:#999;">
                  <i class="fas fa-inbox"></i> Sin ventas en este período
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="grid grid-2 mt-2">
        <!-- Productos por vencer -->
        <div class="card">
          <h3><i class="fas fa-calendar-times"></i> Productos por Vencer <span class="badge badge-warning" style="margin-left:8px;">30 días</span></h3>
          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th>Stock</th>
                  <th>Vence</th>
                  <th>Días</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let p of productosPorVencer" [class.row-danger]="diasParaVencer(p) <= 7">
                  <td>{{ p.nombre }}</td>
                  <td>{{ p.stock }}</td>
                  <td>{{ p.fechaVencimiento | date:'dd/MM/yyyy' }}</td>
                  <td>
                    <span class="badge" [class.badge-danger]="diasParaVencer(p) <= 7" [class.badge-warning]="diasParaVencer(p) > 7">
                      {{ diasParaVencer(p) }} días
                    </span>
                  </td>
                </tr>
                <tr *ngIf="productosPorVencer.length === 0">
                  <td colspan="4" style="text-align:center; color:#999;">
                    <i class="fas fa-check-circle" style="color:#2ecc71;"></i> Sin vencimientos próximos
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Productos bajo stock -->
        <div class="card">
          <h3><i class="fas fa-boxes"></i> Productos Bajo Stock</h3>
          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th>Stock Actual</th>
                  <th>Stock Mínimo</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let p of productosBajoStock">
                  <td>{{ p.nombre }}</td>
                  <td>{{ p.stock }}</td>
                  <td>{{ p.stockMinimo }}</td>
                  <td>
                    <span class="badge" [class.badge-danger]="p.stock === 0" [class.badge-warning]="p.stock > 0">
                      {{ p.stock === 0 ? 'Sin stock' : 'Stock bajo' }}
                    </span>
                  </td>
                </tr>
                <tr *ngIf="productosBajoStock.length === 0">
                  <td colspan="4" style="text-align:center; color:#999;">
                    <i class="fas fa-check-circle" style="color:#2ecc71;"></i> Stock en niveles normales
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Últimas ventas -->
      <div class="card mt-2">
        <h3><i class="fas fa-history"></i> Últimas Ventas</h3>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Nº Venta</th>
                <th>Fecha</th>
                <th>Cliente</th>
                <th>Método Pago</th>
                <th>Total</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let v of ultimasVentas">
                <td>{{ v.numeroVenta }}</td>
                <td>{{ v.fecha | date:'dd/MM/yyyy HH:mm' }}</td>
                <td>{{ v.cliente ? v.cliente.nombre + ' ' + v.cliente.apellido : 'Público general' }}</td>
                <td>{{ v.metodoPago }}</td>
                <td>{{ v.total | soles }}</td>
                <td>
                  <span class="badge"
                    [class.badge-success]="v.estado === 'COMPLETADA'"
                    [class.badge-warning]="v.estado === 'PENDIENTE'"
                    [class.badge-danger]="v.estado === 'CANCELADA'">
                    {{ v.estado }}
                  </span>
                </td>
              </tr>
              <tr *ngIf="ultimasVentas.length === 0">
                <td colspan="6" style="text-align:center; color:#999;">Sin ventas registradas</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .kpi-card {
      display: flex;
      align-items: center;
      gap: 15px;
      padding: 20px;
      border-radius: 8px;
      color: white;
    }
    .kpi-green { background: linear-gradient(135deg, #2ecc71, #27ae60); }
    .kpi-red   { background: linear-gradient(135deg, #e74c3c, #c0392b); }
    .kpi-blue  { background: linear-gradient(135deg, #3498db, #2980b9); }
    .kpi-orange{ background: linear-gradient(135deg, #f39c12, #e67e22); }
    .kpi-icon { font-size: 2rem; opacity: 0.85; }
    .kpi-data { display: flex; flex-direction: column; }
    .kpi-label { font-size: 0.85rem; opacity: 0.9; }
    .kpi-value { font-size: 1.6rem; font-weight: bold; }
    .kpi-sub   { font-size: 0.78rem; opacity: 0.8; margin-top: 2px; }

    .medal {
      display: inline-block;
      width: 26px; height: 26px;
      border-radius: 50%;
      text-align: center;
      line-height: 26px;
      font-weight: bold;
      font-size: 0.85rem;
      background: #bdc3c7;
      color: white;
    }
    .medal.gold   { background: #f1c40f; color: #7d6608; }
    .medal.silver { background: #95a5a6; }
    .medal.bronze { background: #ca6f1e; }

    .row-danger { background-color: #fff5f5; }
  `]
})
export class ReportesComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('ventasChart') ventasChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('productosChart') productosChartRef!: ElementRef<HTMLCanvasElement>;

  private chartVentas: Chart | null = null;
  private chartProductos: Chart | null = null;
  private cargaId = 0;

  meses = ['Enero','Febrero','Marzo','Abril','Mayo','Junio','Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'];
  anios: number[] = [];
  mesSeleccionado: number;
  anioSeleccionado: number;

  ventasMes: Venta[] = [];
  comprasMes: Compra[] = [];
  productosBajoStock: Producto[] = [];
  productosPorVencer: Producto[] = [];
  productosVendidos: ProductoVendido[] = [];
  ultimasVentas: Venta[] = [];

  totalVentas = 0;
  totalCompras = 0;
  utilidad = 0;

  constructor(
    private ventaService: VentaService,
    private compraService: CompraService,
    private productoService: ProductoService,
    private cdr: ChangeDetectorRef
  ) {
    const hoy = new Date();
    this.mesSeleccionado = hoy.getMonth() + 1;
    this.anioSeleccionado = hoy.getFullYear();
    for (let a = hoy.getFullYear(); a >= hoy.getFullYear() - 3; a--) {
      this.anios.push(a);
    }
  }

  ngOnInit(): void {
    this.cargarDatos();
  }

  ngAfterViewInit(): void {
    // Los gráficos se crean en cargarDatos() después de obtener los datos
  }

  ngOnDestroy(): void {
    this.chartVentas?.destroy();
    this.chartProductos?.destroy();
  }

  cargarDatos(): void {
    const cargaActual = ++this.cargaId;

    const anio = Number(this.anioSeleccionado);
    const mes = Number(this.mesSeleccionado);

    const fechaInicio = `${anio}-${String(mes).padStart(2, '0')}-01T00:00:00`;
    const ultimoDiaDelMes = new Date(anio, mes, 0).getDate();
    const fechaFin = `${anio}-${String(mes).padStart(2, '0')}-${String(ultimoDiaDelMes).padStart(2, '0')}T23:59:59`;


    const ventas$ = this.ventaService.obtenerPorPeriodo(fechaInicio, fechaFin).pipe(
      switchMap((ventasPeriodo) => {

        if (Array.isArray(ventasPeriodo) && ventasPeriodo.length > 0) {
          return of(ventasPeriodo);
        }


        return this.ventaService.obtenerTodas().pipe(
          map((todas) => {
            const filtradas = this.filtrarVentasPorMes(todas);
            return filtradas.length > 0 ? filtradas : todas;
          }),
          catchError(() => of([] as Venta[]))
        );
      }),
      catchError(() => this.ventaService.obtenerTodas().pipe(
        map((todas) => {
          const filtradas = this.filtrarVentasPorMes(todas);
          return filtradas.length > 0 ? filtradas : todas;
        }),
        catchError(() => of([] as Venta[]))
      ))
    );

    const compras$ = this.compraService.obtenerPorPeriodo(fechaInicio, fechaFin).pipe(
      catchError(() => of([] as Compra[]))
    );

    const bajoStock$ = this.productoService.obtenerBajoStock().pipe(
      catchError(() => of([] as Producto[]))
    );

    const todos$ = this.productoService.obtenerActivos().pipe(
      catchError(() => of([] as Producto[]))
    );

    forkJoin({
      ventas: ventas$,
      compras: compras$,
      bajoStock: bajoStock$,
      todos: todos$
    }).subscribe({
      next: ({ ventas, compras, bajoStock, todos }) => {
        if (cargaActual !== this.cargaId) {
          return;
        }

        this.procesarVentasYCompras(ventas, compras, bajoStock, todos);
      },
      error: () => AppLogger.error('Error al cargar reportes')
    });
  }

  private procesarVentasYCompras(
    ventas: Venta[],
    compras: Compra[],
    bajoStock: Producto[],
    todos: Producto[]
  ): void {
    this.ventasMes = ventas;
        this.comprasMes = compras;
        this.productosBajoStock = bajoStock;
        this.ultimasVentas = [...ventas]
          .sort((a, b) =>
          new Date(b.fecha!).getTime() - new Date(a.fecha!).getTime()
          )
          .slice(0, 10);

        // Totales
        this.totalVentas = ventas
          .filter(v => this.esVentaCompletada(v))
          .reduce((s, v) => s + v.total, 0);
        this.totalCompras = compras
          .filter(c => c.estado === 'RECIBIDA')
          .reduce((s, c) => s + c.total, 0);
        this.utilidad = this.totalVentas - this.totalCompras;

        // Productos por vencer en 30 días
        const hoy = new Date();
        const en30 = new Date(); en30.setDate(hoy.getDate() + 30);
        this.productosPorVencer = todos
          .filter(p => p.fechaVencimiento)
          .filter(p => {
            const vence = new Date(p.fechaVencimiento!);
            return vence >= hoy && vence <= en30;
          })
          .sort((a, b) => new Date(a.fechaVencimiento!).getTime() - new Date(b.fechaVencimiento!).getTime());

        // Ranking productos más vendidos
        const mapaProductos = new Map<string, ProductoVendido>();
        ventas.filter(v => this.esVentaCompletada(v)).forEach(v => {
          (v.detalles || []).forEach(d => {
            const nombre = d.producto.nombre;
            const actual = mapaProductos.get(nombre) || { nombre, cantidad: 0, total: 0 };
            mapaProductos.set(nombre, {
              nombre,
              cantidad: actual.cantidad + d.cantidad,
              total: actual.total + d.subtotal
            });
          });
        });
        this.productosVendidos = Array.from(mapaProductos.values())
          .sort((a, b) => b.cantidad - a.cantidad)
          .slice(0, 10);

        // Fuerza refresco de tabla en casos donde la vista no repinta tras
        // múltiples respuestas asíncronas (forkJoin + chart rendering).
        this.cdr.detectChanges();

        // Construir gráficos después de que Angular renderice los canvas
        setTimeout(() => {
          this.construirGraficoVentas();
          this.construirGraficoProductos();
        }, 100);
  }

  private filtrarVentasPorMes(ventas: Venta[]): Venta[] {
    const anio = Number(this.anioSeleccionado);
    const mes = Number(this.mesSeleccionado);

    return (ventas || []).filter(v => {
      if (!v?.fecha) {
        return false;
      }

      const fecha = new Date(v.fecha);
      return fecha.getFullYear() === anio
        && (fecha.getMonth() + 1) === mes;
    });
  }

  private esVentaCompletada(v: Venta): boolean {
    return (v?.estado || '').toUpperCase() === 'COMPLETADA';
  }

  diasParaVencer(p: Producto): number {
    const hoy = new Date();
    const vence = new Date(p.fechaVencimiento!);
    return Math.ceil((vence.getTime() - hoy.getTime()) / (1000 * 60 * 60 * 24));
  }

  private construirGraficoVentas(): void {
    if (!this.ventasChartRef?.nativeElement) return;
    this.chartVentas?.destroy();

    // Agrupar ventas por día del mes
    const diasEnMes = new Date(this.anioSeleccionado, this.mesSeleccionado, 0).getDate();
    const totalesPorDia = Array(diasEnMes).fill(0);

    this.ventasMes
      .filter(v => v.estado === 'COMPLETADA')
      .forEach(v => {
        const dia = new Date(v.fecha!).getDate() - 1;
        totalesPorDia[dia] += v.total;
      });

    if (totalesPorDia.every(total => total === 0)) {
      this.limpiarCanvas(this.ventasChartRef.nativeElement);
      return;
    }

    this.chartVentas = new Chart(this.ventasChartRef.nativeElement, {
      type: 'line',
      data: {
        labels: Array.from({ length: diasEnMes }, (_, i) => `${i + 1}`),
        datasets: [{
          label: 'Ventas (S/)',
          data: totalesPorDia,
          borderColor: '#2ecc71',
          backgroundColor: 'rgba(46,204,113,0.1)',
          borderWidth: 2,
          tension: 0.3,
          fill: true,
          pointBackgroundColor: '#2ecc71'
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { display: false } },
        scales: {
          y: { beginAtZero: true, ticks: { callback: v => `S/${v}` } }
        }
      }
    });
  }

  private construirGraficoProductos(): void {
    if (!this.productosChartRef?.nativeElement) return;
    this.chartProductos?.destroy();

    if (this.productosVendidos.length === 0) {
      this.limpiarCanvas(this.productosChartRef.nativeElement);
      return;
    }

    const colores = ['#3498db','#2ecc71','#e74c3c','#f39c12','#9b59b6','#1abc9c','#e67e22','#34495e'];
    const top = this.productosVendidos.slice(0, 6);

    this.chartProductos = new Chart(this.productosChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: top.map(p => p.nombre),
        datasets: [{
          data: top.map(p => p.cantidad),
          backgroundColor: colores.slice(0, top.length),
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { position: 'right', labels: { font: { size: 11 } } }
        }
      }
    });
  }

  private limpiarCanvas(canvas: HTMLCanvasElement): void {
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
  }
}

