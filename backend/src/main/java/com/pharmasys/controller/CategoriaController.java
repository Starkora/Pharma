package com.pharmasys.controller;

import com.pharmasys.dto.request.CategoriaRequestDto;
import com.pharmasys.model.Categoria;
import com.pharmasys.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CategoriaController {
    
    private final CategoriaService categoriaService;
    
    @GetMapping
    public ResponseEntity<List<Categoria>> obtenerTodas() {
        return ResponseEntity.ok(categoriaService.obtenerTodas());
    }
    
    @GetMapping("/activas")
    public ResponseEntity<List<Categoria>> obtenerActivas() {
        return ResponseEntity.ok(categoriaService.obtenerActivas());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Categoria> obtenerPorId(@PathVariable Long id) {
        return categoriaService.obtenerPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Categoria> crear(@Valid @RequestBody CategoriaRequestDto categoriaRequest) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoriaService.crear(toEntity(categoriaRequest)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Categoria> actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequestDto categoriaRequest) {
        try {
            return ResponseEntity.ok(categoriaService.actualizar(id, toEntity(categoriaRequest)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            categoriaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Categoria toEntity(CategoriaRequestDto dto) {
        Categoria categoria = new Categoria();
        categoria.setNombre(dto.getNombre().trim());
        categoria.setDescripcion(trimToNull(dto.getDescripcion()));
        categoria.setActivo(dto.getActivo());
        return categoria;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
