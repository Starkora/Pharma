package com.pharmasys.controller;

import com.pharmasys.dto.request.RolRequestDto;
import com.pharmasys.model.Rol;
import com.pharmasys.service.RolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    @GetMapping
    public ResponseEntity<List<Rol>> listarTodos() {
        return ResponseEntity.ok(rolService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody RolRequestDto request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(rolService.crear(toEntity(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo crear el rol"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable Long id) {
        try {
            rolService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo eliminar el rol"));
        }
    }

    private Rol toEntity(RolRequestDto request) {
        Rol rol = new Rol();
        rol.setNombre(request.getNombre() != null ? request.getNombre().trim() : null);
        rol.setDescripcion(request.getDescripcion());
        return rol;
    }
}
