package com.pharmasys.controller;

import com.pharmasys.model.Rol;
import com.pharmasys.service.RolService;
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
    public ResponseEntity<Object> crear(@RequestBody Rol rol) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(rolService.crear(rol));
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
}
