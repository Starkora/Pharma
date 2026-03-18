package com.pharmasys.service;

import com.pharmasys.dto.response.CompraResponseDto;
import com.pharmasys.model.*;
import com.pharmasys.repository.CompraRepository;
import com.pharmasys.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompraService {
    
    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;
    
    // IGV en Perú: 18%
    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");

    public List<CompraResponseDto> obtenerTodasResumen() {
        return compraRepository.findAllWithRelations().stream()
            .map(this::toResponseDto)
            .toList();
    }
    
    public List<Compra> obtenerTodas() {
        return compraRepository.findAllWithRelations();
    }
    
    public Optional<Compra> obtenerPorId(Long id) {
        return compraRepository.findById(id);
    }
    
    public List<Compra> obtenerPorProveedor(Long proveedorId) {
        return compraRepository.findByProveedorId(proveedorId);
    }
    
    public List<Compra> obtenerPorPeriodo(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return compraRepository.findByFechaBetweenWithRelations(fechaInicio, fechaFin);
    }

    public List<CompraResponseDto> obtenerPorPeriodoResumen(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return compraRepository.findByFechaBetweenWithRelations(fechaInicio, fechaFin).stream()
            .map(this::toResponseDto)
            .toList();
    }
    
    public BigDecimal calcularTotalCompras(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        BigDecimal total = compraRepository.calcularTotalComprasPorPeriodo(fechaInicio, fechaFin);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public Compra crear(Compra compra) {
        // Validaciones iniciales
        if (compra.getDetalles() == null || compra.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La compra debe tener al menos un producto");
        }
        
        if (compra.getProveedor() == null || compra.getProveedor().getId() == null) {
            throw new IllegalArgumentException("Debe especificar un proveedor");
        }
        
        // Generar número de compra único
        String numeroCompra = generarNumeroCompra();
        compra.setNumeroCompra(numeroCompra);
        compra.setFecha(LocalDateTime.now());
        
        // Establecer estado por defecto si no se especificó
        if (compra.getEstado() == null) {
            compra.setEstado(Compra.EstadoCompra.PENDIENTE);
        }
        
        // Calcular totales
        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (DetalleCompra detalle : compra.getDetalles()) {
            // Validar producto existe
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado: " + detalle.getProducto().getId()));
            
            // Validar cantidad positiva
            if (detalle.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
            }
            
            // Validar precio positivo
            if (detalle.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El precio unitario debe ser mayor a cero");
            }
            
            // Calcular subtotal del detalle
            BigDecimal subtotalDetalle = detalle.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(detalle.getCantidad()))
                .setScale(2, RoundingMode.HALF_UP);
            
            detalle.setSubtotal(subtotalDetalle);
            detalle.setCompra(compra);
            detalle.setProducto(producto);
            
            subtotal = subtotal.add(subtotalDetalle);
            
            // Actualizar stock SOLO si la compra está RECIBIDA
            if (compra.getEstado() == Compra.EstadoCompra.RECIBIDA) {
                producto.setStock(producto.getStock() + detalle.getCantidad());
                
                // Actualizar precio de compra del producto (último precio)
                producto.setPrecioCompra(detalle.getPrecioUnitario());
                
                productoRepository.save(producto);
            }
        }
        
        // Calcular impuesto (IGV 18%)
        compra.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        BigDecimal impuesto = subtotal.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_UP);
        compra.setImpuesto(impuesto);
        
        // Calcular total final
        BigDecimal total = subtotal.add(impuesto).setScale(2, RoundingMode.HALF_UP);
        compra.setTotal(total);
        
        return compraRepository.save(compra);
    }
    
    public Compra marcarComoRecibida(Long id) {
        Compra compra = compraRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Compra no encontrada con ID: " + id));
        
        if (compra.getEstado() == Compra.EstadoCompra.RECIBIDA) {
            throw new IllegalStateException("La compra ya está marcada como recibida");
        }
        
        if (compra.getEstado() == Compra.EstadoCompra.CANCELADA) {
            throw new IllegalStateException("No se puede recibir una compra cancelada");
        }
        
        // Incrementar stock de todos los productos
        for (DetalleCompra detalle : compra.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));
            
            producto.setStock(producto.getStock() + detalle.getCantidad());
            producto.setPrecioCompra(detalle.getPrecioUnitario());
            productoRepository.save(producto);
        }
        
        compra.setEstado(Compra.EstadoCompra.RECIBIDA);
        return compraRepository.save(compra);
    }
    
    public void cancelar(Long id) {
        Compra compra = compraRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Compra no encontrada con ID: " + id));
        
        if (compra.getEstado() == Compra.EstadoCompra.CANCELADA) {
            throw new IllegalStateException("La compra ya está cancelada");
        }
        
        // Si estaba recibida, devolver stock (decrementar)
        if (compra.getEstado() == Compra.EstadoCompra.RECIBIDA) {
            for (DetalleCompra detalle : compra.getDetalles()) {
                Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));
                
                int nuevoStock = producto.getStock() - detalle.getCantidad();
                if (nuevoStock < 0) {
                    throw new IllegalStateException("No se puede cancelar la compra: stock insuficiente para " + 
                        producto.getNombre() + ". Stock actual: " + producto.getStock());
                }
                
                producto.setStock(nuevoStock);
                productoRepository.save(producto);
            }
        }
        
        compra.setEstado(Compra.EstadoCompra.CANCELADA);
        compraRepository.save(compra);
    }
    
    private String generarNumeroCompra() {
        // Formato: C-YYYYMMDD-NNNN
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = compraRepository.count() + 1;
        return String.format("C-%s-%04d", fecha, count);
    }

    private CompraResponseDto toResponseDto(Compra compra) {
        CompraResponseDto.ProveedorResponseDto proveedorDto = null;
        if (compra.getProveedor() != null) {
            proveedorDto = new CompraResponseDto.ProveedorResponseDto(
                compra.getProveedor().getId(),
                compra.getProveedor().getNombre(),
                compra.getProveedor().getRuc()
            );
        }

        List<CompraResponseDto.DetalleCompraResponseDto> detallesDto = compra.getDetalles() == null
            ? List.of()
            : compra.getDetalles().stream()
                .map(detalle -> {
                    CompraResponseDto.ProductoResponseDto productoDto = null;
                    if (detalle.getProducto() != null) {
                        productoDto = new CompraResponseDto.ProductoResponseDto(
                            detalle.getProducto().getId(),
                            detalle.getProducto().getNombre()
                        );
                    }

                    return new CompraResponseDto.DetalleCompraResponseDto(
                        detalle.getId(),
                        productoDto,
                        detalle.getCantidad(),
                        detalle.getPrecioUnitario(),
                        detalle.getSubtotal()
                    );
                })
                .toList();

        return new CompraResponseDto(
            compra.getId(),
            compra.getNumeroCompra(),
            compra.getFecha(),
            proveedorDto,
            compra.getSubtotal(),
            compra.getImpuesto(),
            compra.getTotal(),
            compra.getEstado(),
            compra.getObservaciones(),
            detallesDto
        );
    }
}
