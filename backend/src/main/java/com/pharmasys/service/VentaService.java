package com.pharmasys.service;

import com.pharmasys.dto.response.VentaResumenDto;
import com.pharmasys.model.*;
import com.pharmasys.repository.VentaRepository;
import com.pharmasys.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class VentaService {
    
    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final WhatsAppNotificationService whatsAppNotificationService;
    
    // IGV en Perú: 18%
    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");

    public List<VentaResumenDto> obtenerTodasResumen() {
        return ventaRepository.findAllForList().stream()
            .map(this::toResumen)
            .toList();
    }
    
    public List<Venta> obtenerTodas() {
        return ventaRepository.findAllWithRelations();
    }
    
    public Optional<Venta> obtenerPorId(Long id) {
        return ventaRepository.findById(id);
    }
    
    public Optional<Venta> obtenerPorNumero(String numeroVenta) {
        return ventaRepository.findByNumeroVenta(numeroVenta);
    }
    
    public List<Venta> obtenerPorCliente(Long clienteId) {
        return ventaRepository.findByClienteId(clienteId);
    }
    
    public List<Venta> obtenerPorPeriodo(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return ventaRepository.findByFechaBetweenWithRelations(fechaInicio, fechaFin);
    }
    
    public BigDecimal calcularTotalVentas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        BigDecimal total = ventaRepository.calcularTotalVentasPorPeriodo(fechaInicio, fechaFin);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public Venta crear(Venta venta) {
        // Validaciones iniciales
        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            throw new RuntimeException("La venta debe tener al menos un producto");
        }
        
        // Generar número de venta único
        String numeroVenta = generarNumeroVenta();
        venta.setNumeroVenta(numeroVenta);
        venta.setFecha(LocalDateTime.now());
        venta.setEstado(Venta.EstadoVenta.COMPLETADA);
        
        // Calcular totales y validar stock
        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (DetalleVenta detalle : venta.getDetalles()) {
            // Validar producto existe y está activo
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalle.getProducto().getId()));
            
            if (!producto.getActivo()) {
                throw new RuntimeException("El producto " + producto.getNombre() + " no está activo");
            }
            
            // Validar stock disponible
            if (producto.getStock() < detalle.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para " + producto.getNombre() + 
                    ". Stock disponible: " + producto.getStock() + ", solicitado: " + detalle.getCantidad());
            }
            
            // Validar cantidad positiva
            if (detalle.getCantidad() <= 0) {
                throw new RuntimeException("La cantidad debe ser mayor a cero");
            }
            
            // Establecer precio unitario del producto (precio actual)
            detalle.setPrecioUnitario(producto.getPrecioVenta());
            
            // Calcular subtotal del detalle
            BigDecimal subtotalDetalle = detalle.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(detalle.getCantidad()))
                .subtract(detalle.getDescuento() != null ? detalle.getDescuento() : BigDecimal.ZERO);
            
            detalle.setSubtotal(subtotalDetalle.setScale(2, RoundingMode.HALF_UP));
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            
            subtotal = subtotal.add(subtotalDetalle);
            
            // Actualizar stock del producto
            producto.setStock(producto.getStock() - detalle.getCantidad());
            productoRepository.save(producto);
        }
        
        // Calcular impuesto (IGV 18%)
        venta.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        BigDecimal impuesto = subtotal.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_UP);
        venta.setImpuesto(impuesto);
        
        // Aplicar descuento general si existe
        BigDecimal descuento = venta.getDescuento() != null ? venta.getDescuento() : BigDecimal.ZERO;
        venta.setDescuento(descuento);
        
        // Calcular total final
        BigDecimal total = subtotal.add(impuesto).subtract(descuento).setScale(2, RoundingMode.HALF_UP);
        venta.setTotal(total);
        
        // Validar método de pago
        if (venta.getMetodoPago() == null || venta.getMetodoPago().isEmpty()) {
            venta.setMetodoPago("EFECTIVO");
        }
        
        Venta ventaGuardada = ventaRepository.save(venta);
        whatsAppNotificationService.notificarVentaRealizada(ventaGuardada);
        return ventaGuardada;
    }
    
    public void cancelar(Long id) {
        Venta venta = ventaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));
        
        if (venta.getEstado() == Venta.EstadoVenta.CANCELADA) {
            throw new RuntimeException("La venta ya está cancelada");
        }
        
        // Devolver stock a cada producto
        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            
            producto.setStock(producto.getStock() + detalle.getCantidad());
            productoRepository.save(producto);
        }
        
        venta.setEstado(Venta.EstadoVenta.CANCELADA);
        ventaRepository.save(venta);
    }
    
    private String generarNumeroVenta() {
        // Formato: V-YYYYMMDD-NNNN
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = ventaRepository.count() + 1;
        return String.format("V-%s-%04d", fecha, count);
    }

    private VentaResumenDto toResumen(Venta venta) {
        VentaResumenDto.ClienteResumenDto clienteDto = null;
        if (venta.getCliente() != null) {
            clienteDto = new VentaResumenDto.ClienteResumenDto(
                venta.getCliente().getId(),
                venta.getCliente().getNombre(),
                venta.getCliente().getApellido()
            );
        }

        return new VentaResumenDto(
            venta.getId(),
            venta.getNumeroVenta(),
            venta.getFecha(),
            clienteDto,
            venta.getTotal(),
            venta.getMetodoPago(),
            venta.getEstado()
        );
    }
}
