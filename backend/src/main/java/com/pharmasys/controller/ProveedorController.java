package com.pharmasys.controller;

import com.pharmasys.dto.request.ProveedorRequestDto;
import com.pharmasys.model.Proveedor;
import com.pharmasys.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {
    
    private final ProveedorService proveedorService;
    
    @GetMapping
    public ResponseEntity<List<Proveedor>> obtenerTodos() {
        return ResponseEntity.ok(proveedorService.obtenerTodos());
    }
    
    @GetMapping("/activos")
    public ResponseEntity<List<Proveedor>> obtenerActivos() {
        return ResponseEntity.ok(proveedorService.obtenerActivos());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Proveedor> obtenerPorId(@PathVariable Long id) {
        return proveedorService.obtenerPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/buscar")
    public ResponseEntity<List<Proveedor>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(proveedorService.buscarPorNombre(nombre));
    }
    
    @PostMapping
    public ResponseEntity<Proveedor> crear(@Valid @RequestBody ProveedorRequestDto proveedorRequest) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(proveedorService.crear(toEntity(proveedorRequest)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Proveedor> actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorRequestDto proveedorRequest) {
        try {
            return ResponseEntity.ok(proveedorService.actualizar(id, toEntity(proveedorRequest)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            proveedorService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Proveedor toEntity(ProveedorRequestDto dto) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(dto.getNombre().trim());
        proveedor.setRuc(trimToNull(dto.getRuc()));
        proveedor.setDireccion(trimToNull(dto.getDireccion()));
        proveedor.setTelefono(trimToNull(dto.getTelefono()));
        proveedor.setEmail(trimToNull(dto.getEmail()));
        proveedor.setPersonaContacto(trimToNull(dto.getPersonaContacto()));
        proveedor.setActivo(dto.getActivo());
        return proveedor;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
