export interface Producto {
  id?: number;
  codigo: string;
  nombre: string;
  descripcion?: string;
  categoria?: Categoria;
  proveedor?: Proveedor;
  precioCompra: number;
  precioVenta: number;
  stock: number;
  stockMinimo: number;
  unidadMedida?: string;
  requiereReceta?: boolean;
  fechaVencimiento?: string;
  lote?: string;
  imagenUrl?: string;
  activo?: boolean;
  fechaCreacion?: string;
  ultimaModificacion?: string;
}

export interface Categoria {
  id?: number;
  nombre: string;
  descripcion?: string;
  activo?: boolean;
  fechaCreacion?: string;
}

export interface Proveedor {
  id?: number;
  nombre: string;
  ruc?: string;
  direccion?: string;
  telefono?: string;
  email?: string;
  personaContacto?: string;
  activo?: boolean;
  fechaCreacion?: string;
  ultimaModificacion?: string;
}

export interface Cliente {
  id?: number;
  nombre: string;
  apellido: string;
  dni?: string;
  direccion?: string;
  telefono?: string;
  email?: string;
  fechaNacimiento?: string;
  activo?: boolean;
  fechaCreacion?: string;
  ultimaModificacion?: string;
}

export interface Venta {
  id?: number;
  numeroVenta?: string;
  cliente?: Cliente;
  usuario?: any;
  fecha?: string;
  subtotal: number;
  impuesto: number;
  descuento: number;
  total: number;
  metodoPago?: string;
  estado?: 'COMPLETADA' | 'CANCELADA' | 'PENDIENTE';
  observaciones?: string;
  detalles: DetalleVenta[];
}

export interface DetalleVenta {
  id?: number;
  producto: Producto;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  descuento?: number;
}

export interface Compra {
  id?: number;
  numeroCompra?: string;
  proveedor: Proveedor;
  usuario?: any;
  fecha?: string;
  subtotal: number;
  impuesto: number;
  total: number;
  estado?: 'PENDIENTE' | 'RECIBIDA' | 'CANCELADA';
  observaciones?: string;
  detalles: DetalleCompra[];
}

export interface DetalleCompra {
  id?: number;
  producto: Producto;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Comprobante {
  id?: number;
  tipoComprobante: string;      // BOLETA / FACTURA
  serie: string;
  numero: number;
  numeroCompleto: string;
  venta?: any;
  tipoDocComprador?: string;    // 0=sin doc, 1=DNI, 6=RUC
  numDocComprador?: string;
  razonSocialComprador?: string;
  total?: number;
  estado: string;               // EMITIDO / ERROR / ANULADO
  linkPdf?: string;
  linkXml?: string;
  linkCdr?: string;
  codigoHash?: string;
  mensajeError?: string;
  fechaEmision?: string;
}

export interface EmitirComprobanteDTO {
  ventaId: number;
  tipoComprobante: string;      // BOLETA / FACTURA
  tipoDocComprador: string;     // 0=sin doc, 1=DNI, 6=RUC
  numDocComprador: string;
  razonSocialComprador: string;
}

