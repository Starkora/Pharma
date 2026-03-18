package com.pharmasys.controller;

import com.pharmasys.dto.request.CompraRequestDto;
import com.pharmasys.dto.response.CompraResponseDto;
import com.pharmasys.model.Compra;
import com.pharmasys.model.DetalleCompra;
import com.pharmasys.model.Producto;
import com.pharmasys.model.Proveedor;
import com.pharmasys.model.Usuario;
import com.pharmasys.repository.ProveedorRepository;
import com.pharmasys.repository.UsuarioRepository;
import com.pharmasys.service.CompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {
    
    private final CompraService compraService;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    
    @GetMapping
    public ResponseEntity<List<CompraResponseDto>> obtenerTodas() {
        return ResponseEntity.ok(compraService.obtenerTodasResumen());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Compra> obtenerPorId(@PathVariable Long id) {
        return compraService.obtenerPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/proveedor/{proveedorId}")
    public ResponseEntity<List<Compra>> obtenerPorProveedor(@PathVariable Long proveedorId) {
        return ResponseEntity.ok(compraService.obtenerPorProveedor(proveedorId));
    }
    
    @GetMapping("/periodo")
    public ResponseEntity<List<CompraResponseDto>> obtenerPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        return ResponseEntity.ok(compraService.obtenerPorPeriodoResumen(fechaInicio, fechaFin));
    }
    
    @GetMapping("/total")
    public ResponseEntity<BigDecimal> calcularTotal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        return ResponseEntity.ok(compraService.calcularTotalCompras(fechaInicio, fechaFin));
    }
    
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CompraRequestDto request, Authentication authentication) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(compraService.crear(mapearCompra(request, authentication)));
        } catch (RuntimeException e) {
            String message = e.getMessage() != null && !e.getMessage().isBlank()
                ? e.getMessage()
                : "No se pudo procesar la compra";
            return ResponseEntity.badRequest().body(Map.of("error", message, "message", message));
        }
    }
    
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        try {
            compraService.cancelar(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}/recibir")
    public ResponseEntity<?> marcarComoRecibida(@PathVariable Long id) {
        try {
            compraService.marcarComoRecibida(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Compra marcada como recibida"
            ));
        } catch (RuntimeException e) {
            String message = e.getMessage() != null && !e.getMessage().isBlank()
                ? e.getMessage()
                : "No se pudo marcar la compra como recibida";
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", message,
                "error", message
            ));
        }
    }

    private Compra mapearCompra(CompraRequestDto request, Authentication authentication) {
        Compra compra = new Compra();
        compra.setProveedor(resolveProveedor(request.getProveedor().getId()));
        compra.setUsuario(resolveUsuario(authentication, request.getUsuario()));
        compra.setEstado(request.getEstado() != null ? request.getEstado() : Compra.EstadoCompra.PENDIENTE);
        compra.setObservaciones(request.getObservaciones());

        List<DetalleCompra> detalles = new ArrayList<>();
        for (CompraRequestDto.DetalleCompraRequestDto detalleDto : request.getDetalles()) {
            detalles.add(mapearDetalle(detalleDto));
        }

        compra.setDetalles(detalles);
        return compra;
    }

    private DetalleCompra mapearDetalle(CompraRequestDto.DetalleCompraRequestDto detalleDto) {
        Producto producto = new Producto();
        producto.setId(detalleDto.getProducto().getId());

        DetalleCompra detalle = new DetalleCompra();
        detalle.setProducto(producto);
        detalle.setCantidad(detalleDto.getCantidad());
        detalle.setPrecioUnitario(detalleDto.getPrecioUnitario());
        detalle.setSubtotal(detalleDto.getSubtotal());
        return detalle;
    }

    private Proveedor resolveProveedor(Long proveedorId) {
        return proveedorRepository.findById(proveedorId)
            .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
    }

    private Usuario resolveUsuario(Authentication authentication, CompraRequestDto.IdReferenceDto usuarioRequest) {
        if (authentication != null && authentication.isAuthenticated()) {
            return usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
        }

        if (usuarioRequest != null && usuarioRequest.getId() != null) {
            return usuarioRepository.findById(usuarioRequest.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        }

        throw new RuntimeException("No se pudo determinar el usuario de la compra");
    }
}
