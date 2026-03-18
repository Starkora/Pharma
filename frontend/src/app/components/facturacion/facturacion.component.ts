import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FacturacionService } from '../../services/facturacion.service';
import { VentaService } from '../../services/venta.service';
import { AppLogger } from '../../services/app-logger.service';
import { Comprobante, EmitirComprobanteDTO, Venta } from '../../models/models';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-facturacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card">
      <div class="card-header">
        <h2><i class="fas fa-file-invoice"></i> Facturación Electrónica – SUNAT</h2>
        <button class="btn btn-primary" (click)="abrirModalEmitir()">
          <i class="fas fa-plus"></i> Emitir Comprobante
        </button>
      </div>

      <!-- Modal Emitir Comprobante -->
      <div class="modal-overlay" *ngIf="mostrandoModal" (click)="cerrarAlHacerClick($event)">
        <div class="modal-container modal-lg">
          <div class="modal-header">
            <h3><i class="fas fa-file-invoice-dollar"></i> Emitir Comprobante Electrónico</h3>
            <button class="btn-close-modal" (click)="mostrandoModal = false">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="modal-body">
            <!-- Selección de Venta -->
            <div class="form-group">
              <label>Venta a Facturar *</label>
              <select class="form-control" [(ngModel)]="dto.ventaId" (change)="onVentaChange()"
                name="ventaId" required>
                <option [value]="0">-- Seleccione una venta --</option>
                <option *ngFor="let v of ventasSinComprobante" [value]="v.id">
                  Venta #{{ v.id }} – {{ v.fecha | date:'dd/MM/yyyy HH:mm' }}
                  – S/ {{ v.total | number:'1.2-2' }}
                  <span *ngIf="v.cliente"> – {{ v.cliente.nombre }} {{ v.cliente.apellido }}</span>
                </option>
              </select>
            </div>

            <!-- Datos de la venta seleccionada -->
            <div *ngIf="ventaSeleccionada" class="info-box mb-3">
              <strong>Detalles de la venta:</strong>
              <div class="table-container mt-1">
                <table class="table" style="font-size:0.85rem;">
                  <thead>
                    <tr>
                      <th>Producto</th>
                      <th>Cant.</th>
                      <th>P. Unit.</th>
                      <th>Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let d of ventaSeleccionada.detalles">
                      <td>{{ d.producto.nombre || 'N/A' }}</td>
                      <td>{{ d.cantidad }}</td>
                      <td>S/ {{ d.precioUnitario | number:'1.2-2' }}</td>
                      <td>S/ {{ (d.cantidad * d.precioUnitario) | number:'1.2-2' }}</td>
                    </tr>
                  </tbody>
                  <tfoot>
                    <tr>
                      <td colspan="3" style="text-align:right; font-weight:bold;">Total:</td>
                      <td style="font-weight:bold;">S/ {{ ventaSeleccionada.total | number:'1.2-2' }}</td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>

            <div class="grid grid-2">
              <!-- Tipo de Comprobante -->
              <div class="form-group">
                <label>Tipo de Comprobante *</label>
                <div class="radio-group">
                  <label class="radio-option" [class.selected]="dto.tipoComprobante === 'BOLETA'">
                    <input type="radio" value="BOLETA" [(ngModel)]="dto.tipoComprobante" name="tipo" (change)="onTipoComprobanteChange()">
                    <i class="fas fa-receipt"></i> Boleta de Venta
                  </label>
                  <label class="radio-option" [class.selected]="dto.tipoComprobante === 'FACTURA'">
                    <input type="radio" value="FACTURA" [(ngModel)]="dto.tipoComprobante" name="tipo" (change)="onTipoComprobanteChange()">
                    <i class="fas fa-file-invoice"></i> Factura Electrónica
                  </label>
                </div>
              </div>

              <!-- Tipo de Documento del Comprador -->
              <div class="form-group">
                <label>Tipo de Documento Comprador *</label>
                <select class="form-control" [(ngModel)]="dto.tipoDocComprador" name="tipoDoc"
                  (change)="onTipoDocChange()">
                  <option value="0">Sin Documento (Consumidor Final)</option>
                  <option value="1">DNI</option>
                  <option value="4">Carné de Extranjería</option>
                  <option value="6">RUC</option>
                </select>
              </div>

              <!-- Número de Documento -->
              <div class="form-group" *ngIf="dto.tipoDocComprador !== '0'">
                <label>
                  {{ dto.tipoDocComprador === '6' ? 'RUC' : 'Número de Documento' }} *
                </label>
                <input type="text" class="form-control" [(ngModel)]="dto.numDocComprador"
                  name="numDoc"
                  [maxlength]="dto.tipoDocComprador === '6' ? 11 : 8"
                  [placeholder]="dto.tipoDocComprador === '6' ? '20XXXXXXXXX' : '00000000'">
              </div>

              <!-- Razón Social -->
              <div class="form-group">
                <label>{{ dto.tipoDocComprador === '6' ? 'Razón Social' : 'Nombre / Razón Social' }}</label>
                <input type="text" class="form-control" [(ngModel)]="dto.razonSocialComprador"
                  name="razonSocial" placeholder="Nombre del comprador">
              </div>
            </div>

            <!-- Advertencia para Factura -->
            <div *ngIf="dto.tipoComprobante === 'FACTURA'" class="alert-info" style="background:#fff3cd; border:1px solid #ffc107; border-radius:6px; padding:10px; margin-top:8px;">
              <i class="fas fa-exclamation-triangle" style="color:#f39c12;"></i>
              <strong> Factura Electrónica</strong>: requiere RUC del comprador (persona natural o empresa).
              Asegúrese de seleccionar tipo RUC e ingresar los 11 dígitos.
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" (click)="mostrandoModal = false">
              Cancelar
            </button>
            <button type="button" class="btn btn-success" [disabled]="emitiendo || !formularioValido()"
              (click)="emitirComprobante()">
              <i class="fas" [class.fa-spinner]="emitiendo" [class.fa-spin]="emitiendo"
                 [class.fa-paper-plane]="!emitiendo"></i>
              {{ emitiendo ? 'Emitiendo...' : 'Emitir a SUNAT' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Tabla de Comprobantes -->
      <div class="table-container mt-2">
        <table class="table">
          <thead>
            <tr>
              <th>Nº Comprobante</th>
              <th>Fecha</th>
              <th>Tipo</th>
              <th>Comprador</th>
              <th>Total</th>
              <th>Estado</th>
              <th>Documentos</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of comprobantes">
              <td><strong>{{ c.numeroCompleto }}</strong></td>
              <td>{{ c.fechaEmision | date:'dd/MM/yyyy HH:mm' }}</td>
              <td>
                <span class="badge" [class.badge-info]="c.tipoComprobante === 'BOLETA'"
                      [class.badge-primary]="c.tipoComprobante === 'FACTURA'"
                      style="background: {{ c.tipoComprobante==='BOLETA'?'#17a2b8':'#007bff' }}; color:#fff; padding:3px 8px; border-radius:4px;">
                  <i class="fas" [class.fa-receipt]="c.tipoComprobante === 'BOLETA'"
                     [class.fa-file-invoice]="c.tipoComprobante === 'FACTURA'"></i>
                  {{ c.tipoComprobante }}
                </span>
              </td>
              <td>
                <span *ngIf="c.numDocComprador && c.tipoDocComprador !== '0'">
                  {{ c.numDocComprador }} –
                </span>
                {{ c.razonSocialComprador || 'Consumidor Final' }}
              </td>
              <td>S/ {{ c.total | number:'1.2-2' }}</td>
              <td>
                <span class="badge estado-badge"
                  [class.estado-emitido]="c.estado === 'EMITIDO'"
                  [class.estado-error]="c.estado === 'ERROR'"
                  [class.estado-anulado]="c.estado === 'ANULADO'">
                  <i class="fas"
                    [class.fa-check-circle]="c.estado === 'EMITIDO'"
                    [class.fa-times-circle]="c.estado === 'ERROR'"
                    [class.fa-ban]="c.estado === 'ANULADO'"></i>
                  {{ c.estado }}
                </span>
                <small *ngIf="c.estado === 'ERROR' && c.mensajeError"
                  class="error-msg" [title]="c.mensajeError">
                  <i class="fas fa-info-circle"></i>
                </small>
              </td>
              <td>
                <a *ngIf="c.linkPdf" [href]="c.linkPdf" target="_blank"
                  class="btn btn-sm btn-danger" title="Ver PDF">
                  <i class="fas fa-file-pdf"></i>
                </a>
                <span *ngIf="!c.linkPdf && c.estado !== 'ERROR'" class="text-muted">
                  <i class="fas fa-clock"></i>
                </span>
                <span *ngIf="c.estado === 'ERROR'" class="text-muted" [title]="c.mensajeError || ''">
                  <small>{{ (c.mensajeError || '').substring(0, 40) }}{{ (c.mensajeError || '').length > 40 ? '...' : '' }}</small>
                </span>
              </td>
            </tr>
            <tr *ngIf="comprobantes.length === 0">
              <td colspan="7" style="text-align:center; color:#999;">
                <i class="fas fa-file-invoice"></i> No hay comprobantes emitidos
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .info-box {
      background: #f8f9fa;
      border: 1px solid #dee2e6;
      border-radius: 8px;
      padding: 12px;
    }
    .radio-group {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }
    .radio-option {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 16px;
      border: 2px solid #dee2e6;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
    }
    .radio-option.selected {
      border-color: #2c7be5;
      background: #e8f0fe;
      color: #2c7be5;
    }
    .radio-option input { display: none; }

    .estado-badge {
      padding: 4px 10px;
      border-radius: 4px;
      font-size: 0.8rem;
      font-weight: 600;
    }
    .estado-emitido { background: #d4edda; color: #155724; }
    .estado-error   { background: #f8d7da; color: #721c24; }
    .estado-anulado { background: #e2e3e5; color: #383d41; }

    .error-msg { color: #dc3545; margin-left: 4px; cursor: help; }
    .text-muted { color: #999; }
  `]
})
export class FacturacionComponent implements OnInit {

  comprobantes: Comprobante[] = [];
  ventasSinComprobante: Venta[] = [];
  ventaSeleccionada: Venta | null = null;

  mostrandoModal = false;
  emitiendo = false;

  dto: EmitirComprobanteDTO = this.dtoVacio();

  constructor(
    private readonly facturacionService: FacturacionService,
    private readonly ventaService: VentaService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarComprobantes();
  }

  cargarComprobantes(): void {
    this.facturacionService.listarTodos().subscribe({
      next: (data) => {
        this.comprobantes = data;
        this.cdr.detectChanges();
      },
      error: () => AppLogger.error('Error al cargar comprobantes')
    });
  }

  abrirModalEmitir(): void {
    this.dto = this.dtoVacio();
    this.ventaSeleccionada = null;
    this.mostrandoModal = true;
    this.cargarVentasSinComprobante();
  }

  cargarVentasSinComprobante(): void {
    this.ventaService.obtenerTodas().subscribe({
      next: (ventas: Venta[]) => {
        // Solo ventas COMPLETADAS
        const completadas = ventas.filter((v: Venta) => v.estado === 'COMPLETADA');
        // Filtrar las que ya tienen comprobante valido (ERROR no bloquea reemision)
        const conComprobante = new Set(
          this.comprobantes
            .filter(c => (c.estado || '').toUpperCase() !== 'ERROR')
            .map(c => c.venta?.id)
            .filter(Boolean)
        );
        this.ventasSinComprobante = completadas.filter((v: Venta) => !conComprobante.has(v.id));
        this.cdr.detectChanges();
      },
      error: () => AppLogger.error('Error al cargar ventas')
    });
  }

  onVentaChange(): void {
    if (this.dto.ventaId && this.dto.ventaId > 0) {
      this.ventaSeleccionada = this.ventasSinComprobante.find(v => v.id === Number(this.dto.ventaId)) || null;
      // Autocompletar datos del cliente si existe
      if (this.ventaSeleccionada?.cliente) {
        const c = this.ventaSeleccionada.cliente;
        if (c.dni && this.dto.tipoComprobante !== 'FACTURA') {
          this.dto.tipoDocComprador = '1';
          this.dto.numDocComprador = c.dni;
        }
        this.dto.razonSocialComprador = `${c.nombre} ${c.apellido}`.trim();
      }
    } else {
      this.ventaSeleccionada = null;
    }
  }

  onTipoDocChange(): void {
    if (this.dto.tipoDocComprador === '0') {
      this.dto.numDocComprador = '';
      this.dto.razonSocialComprador = 'CONSUMIDOR FINAL';
    }
    if (this.dto.tipoComprobante === 'FACTURA' && this.dto.tipoDocComprador !== '6') {
      this.dto.tipoDocComprador = '6';
      this.dto.numDocComprador = '';
      Swal.fire({
        icon: 'info',
        title: 'Factura Electrónica',
        text: 'Las facturas solo pueden emitirse con RUC del comprador (persona natural con RUC 10 o empresa con RUC 20).',
        timer: 2500,
        showConfirmButton: false
      });
    }

    if (this.dto.tipoDocComprador === '6' && this.dto.numDocComprador && !this.esRucFormatoValido(this.dto.numDocComprador)) {
      this.dto.numDocComprador = '';
    }

    if (this.dto.tipoDocComprador === '1' && this.dto.numDocComprador && !this.esDniFormatoValido(this.dto.numDocComprador)) {
      this.dto.numDocComprador = '';
    }
  }

  onTipoComprobanteChange(): void {
    if (this.dto.tipoComprobante !== 'FACTURA') {
      return;
    }

    const tipoAnterior = this.dto.tipoDocComprador;
    this.dto.tipoDocComprador = '6';

    // Si venia de DNI u otro doc, fuerza captura de RUC para evitar errores del proveedor.
    if (!this.esRucFormatoValido(this.dto.numDocComprador || '')) {
      this.dto.numDocComprador = '';
    }

    if (tipoAnterior !== '6') {
      Swal.fire({
        icon: 'info',
        title: 'Factura Electrónica',
        text: 'Puedes emitir factura a persona natural o empresa, pero en ambos casos debe tener RUC válido (11 dígitos).',
        timer: 3000,
        showConfirmButton: false
      });
    }
  }

  formularioValido(): boolean {
    if (!this.dto.ventaId || this.dto.ventaId <= 0) return false;
    if (!this.dto.tipoComprobante) return false;
    if (this.dto.tipoDocComprador !== '0' && !this.dto.numDocComprador) return false;
    if (this.dto.tipoComprobante === 'FACTURA' && this.dto.tipoDocComprador !== '6') return false;
    if (this.dto.tipoDocComprador === '6' && !this.esRucFormatoValido(this.dto.numDocComprador || '')) return false;
    if (this.dto.tipoDocComprador === '1' && !this.esDniFormatoValido(this.dto.numDocComprador || '')) return false;
    return true;
  }

  emitirComprobante(): void {
    if (!this.formularioValido()) return;

    // Si no hay razón social, asignar valor por defecto
    if (!this.dto.razonSocialComprador) {
      this.dto.razonSocialComprador = 'CONSUMIDOR FINAL';
    }

    this.emitiendo = true;
    this.facturacionService.emitir(this.dto).subscribe({
      next: (comprobante: Comprobante) => {
        this.emitiendo = false;
        this.mostrandoModal = false;

        if (comprobante.estado === 'EMITIDO') {
          Swal.fire({
            icon: 'success',
            title: '¡Comprobante emitido!',
            text: `Número: ${comprobante.numeroCompleto}`,
            confirmButtonText: 'Aceptar'
          }).then(() => {
            if (this.esUrlSegura(comprobante.linkPdf)) {
              window.open(comprobante.linkPdf, '_blank', 'noopener,noreferrer');
            }
          });
        } else {
          Swal.fire({
            icon: 'warning',
            title: 'Comprobante con error',
            text: comprobante.mensajeError || 'Hubo un error al emitir. Revisa la configuración de APISUNAT.',
            confirmButtonText: 'Entendido'
          });
        }
        this.cargarComprobantes();
      },
      error: (err) => {
        this.emitiendo = false;
        const msg = err?.error?.message || err?.error?.error || 'Error al emitir comprobante';
        Swal.fire({ icon: 'error', title: 'Error', text: msg });
      }
    });
  }

  cerrarAlHacerClick(event: MouseEvent): void {
    if ((event.target as Element).classList.contains('modal-overlay')) {
      this.mostrandoModal = false;
    }
  }

  private dtoVacio(): EmitirComprobanteDTO {
    return {
      ventaId: 0,
      tipoComprobante: 'BOLETA',
      tipoDocComprador: '0',
      numDocComprador: '',
      razonSocialComprador: 'CONSUMIDOR FINAL'
    };
  }

  private esUrlSegura(url?: string): boolean {
    if (!url) {
      return false;
    }
    try {
      const parsed = new URL(url);
      return parsed.protocol === 'https:' || parsed.protocol === 'http:';
    } catch {
      return false;
    }
  }

  private esRucFormatoValido(ruc: string): boolean {
    return /^\d{11}$/.test((ruc || '').trim());
  }

  private esDniFormatoValido(dni: string): boolean {
    return /^\d{8}$/.test((dni || '').trim());
  }
}
