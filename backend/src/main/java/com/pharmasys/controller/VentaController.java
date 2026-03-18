package com.pharmasys.controller;

import com.pharmasys.dto.request.VentaRequestDto;
import com.pharmasys.dto.response.VentaResumenDto;
import com.pharmasys.model.Cliente;
import com.pharmasys.model.DetalleVenta;
import com.pharmasys.model.Producto;
import com.pharmasys.model.Usuario;
import com.pharmasys.model.Venta;
import com.pharmasys.repository.ClienteRepository;
import com.pharmasys.repository.UsuarioRepository;
import com.pharmasys.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {
    
    private final VentaService ventaService;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    
    @GetMapping
    public ResponseEntity<List<VentaResumenDto>> obtenerTodas() {
        return ResponseEntity.ok(ventaService.obtenerTodasResumen());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Venta> obtenerPorId(@PathVariable Long id) {
        return ventaService.obtenerPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/numero/{numeroVenta}")
    public ResponseEntity<Venta> obtenerPorNumero(@PathVariable String numeroVenta) {
        return ventaService.obtenerPorNumero(numeroVenta)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Venta>> obtenerPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(ventaService.obtenerPorCliente(clienteId));
    }
    
    @GetMapping("/periodo")
    public ResponseEntity<List<Venta>> obtenerPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        return ResponseEntity.ok(ventaService.obtenerPorPeriodo(fechaInicio, fechaFin));
    }
    
    @GetMapping("/total")
    public ResponseEntity<BigDecimal> calcularTotal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        return ResponseEntity.ok(ventaService.calcularTotalVentas(fechaInicio, fechaFin));
    }
    
    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody VentaRequestDto request, Authentication authentication) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ventaService.crear(mapearVenta(request, authentication)));
        } catch (RuntimeException e) {
            String message = e.getMessage() != null && !e.getMessage().isBlank()
                ? e.getMessage()
                : "No se pudo procesar la venta";
            return ResponseEntity.badRequest().body(Map.of("error", message, "message", message));
        }
    }
    
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        try {
            ventaService.cancelar(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private Venta mapearVenta(VentaRequestDto request, Authentication authentication) {
        Venta venta = new Venta();
        venta.setCliente(resolveCliente(request.getCliente()));
        venta.setUsuario(resolveUsuario(authentication, request.getUsuario()));
        venta.setDescuento(request.getDescuento() != null
            ? request.getDescuento().setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO);
        venta.setMetodoPago(request.getMetodoPago());
        venta.setObservaciones(request.getObservaciones());

        List<DetalleVenta> detalles = new ArrayList<>();
        for (VentaRequestDto.DetalleVentaRequestDto detalleDto : request.getDetalles()) {
            detalles.add(mapearDetalle(detalleDto));
        }

        venta.setDetalles(detalles);
        return venta;
    }

    private DetalleVenta mapearDetalle(VentaRequestDto.DetalleVentaRequestDto detalleDto) {
        Producto producto = new Producto();
        producto.setId(detalleDto.getProducto().getId());

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(detalleDto.getCantidad());
        detalle.setDescuento(detalleDto.getDescuento() != null
            ? detalleDto.getDescuento().setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO);
        detalle.setPrecioUnitario(detalleDto.getPrecioUnitario());
        detalle.setSubtotal(detalleDto.getSubtotal());
        return detalle;
    }

    private Cliente resolveCliente(VentaRequestDto.IdReferenceDto clienteRequest) {
        if (clienteRequest == null || clienteRequest.getId() == null) {
            return null;
        }

        return clienteRepository.findById(clienteRequest.getId())
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }

    private Usuario resolveUsuario(Authentication authentication, VentaRequestDto.IdReferenceDto usuarioRequest) {
        if (authentication != null && authentication.isAuthenticated()) {
            return usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
        }

        if (usuarioRequest != null && usuarioRequest.getId() != null) {
            return usuarioRepository.findById(usuarioRequest.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        }

        throw new RuntimeException("No se pudo determinar el usuario de la venta");
    }
}
