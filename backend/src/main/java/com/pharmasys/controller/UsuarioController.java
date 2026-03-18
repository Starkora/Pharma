package com.pharmasys.controller;

import com.pharmasys.dto.request.CambiarPasswordRequestDto;
import com.pharmasys.dto.request.UsuarioRequestDto;
import com.pharmasys.model.Usuario;
import com.pharmasys.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private static final String JSON_FIELD_ERROR = "error";
    private Usuario sanitizar(Usuario u) {
        u.setPassword(null);
        return u;
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> listarTodos() {
        List<Usuario> lista = usuarioService.listarTodos();
        lista.forEach(this::sanitizar);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/activos")
    public ResponseEntity<List<Usuario>> listarActivos() {
        List<Usuario> lista = usuarioService.listarActivos();
        lista.forEach(this::sanitizar);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id)
                .map(u -> ResponseEntity.ok(sanitizar(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody UsuarioRequestDto request) {
        try {
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(JSON_FIELD_ERROR, "El nombre de usuario es obligatorio"));
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(JSON_FIELD_ERROR, "La contraseña es obligatoria"));
            }

            // Validar username y email únicos
            if (usuarioService.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(Map.of(JSON_FIELD_ERROR, "El nombre de usuario ya está en uso"));
            }
            if (request.getEmail() != null && usuarioService.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(Map.of(JSON_FIELD_ERROR, "El correo electrónico ya está registrado"));
            }
            Usuario nuevo = usuarioService.crear(toEntity(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(sanitizar(nuevo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(JSON_FIELD_ERROR, "No se pudo crear el usuario"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequestDto request) {
        try {
            // Validar email único si cambió
            if (request.getEmail() != null) {
                var existe = usuarioService.buscarPorEmail(request.getEmail());
                if (existe.isPresent() && !existe.get().getId().equals(id)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of(JSON_FIELD_ERROR, "El correo electrónico ya está en uso"));
                }
            }
            Usuario actualizado = usuarioService.actualizar(id, toEntity(request));
            return ResponseEntity.ok(sanitizar(actualizado));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(JSON_FIELD_ERROR, "No se pudo actualizar el usuario"));
        }
    }

    @PutMapping("/{id}/cambiar-password")
    public ResponseEntity<Object> cambiarPassword(@PathVariable Long id,
                                             @Valid @RequestBody CambiarPasswordRequestDto request) {
        try {
            String nuevaPassword = request.getPassword();
            usuarioService.cambiarPassword(id, nuevaPassword);
            return ResponseEntity.ok(Map.of("message", "Contraseña actualizada"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(JSON_FIELD_ERROR, "No se pudo actualizar la contraseña"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable Long id) {
        try {
            usuarioService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(JSON_FIELD_ERROR, "No se pudo eliminar el usuario"));
        }
    }

    private Usuario toEntity(UsuarioRequestDto dto) {
        Usuario usuario = new Usuario();
        usuario.setUsername(trimToNull(dto.getUsername()));
        usuario.setPassword(trimToNull(dto.getPassword()));
        usuario.setNombre(dto.getNombre().trim());
        usuario.setApellido(dto.getApellido().trim());
        usuario.setEmail(trimToNull(dto.getEmail()));
        usuario.setTelefono(trimToNull(dto.getTelefono()));
        usuario.setRoles(dto.getRoles());
        usuario.setActivo(dto.getActivo());
        return usuario;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
