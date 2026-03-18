package com.pharmasys.controller;

import com.pharmasys.model.Comprobante;
import com.pharmasys.model.EmitirComprobanteDTO;
import com.pharmasys.service.FacturacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/facturacion")
@RequiredArgsConstructor
public class FacturacionController {

    private final FacturacionService facturacionService;

    @GetMapping
    public ResponseEntity<List<Comprobante>> listarTodos() {
        return ResponseEntity.ok(facturacionService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Comprobante> buscarPorId(@PathVariable Long id) {
        return facturacionService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/venta/{ventaId}")
    public ResponseEntity<Comprobante> buscarPorVenta(@PathVariable Long ventaId) {
        return facturacionService.buscarPorVenta(ventaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/emitir")
    public ResponseEntity<Object> emitir(@RequestBody EmitirComprobanteDTO dto) {
        try {
            Comprobante comprobante = facturacionService.emitir(dto);
            return ResponseEntity.ok(comprobante);
        } catch (RuntimeException e) {
            String message = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "No se pudo emitir el comprobante";
            return ResponseEntity.badRequest().body(Map.of("error", message, "message", message));
        }
    }
}
