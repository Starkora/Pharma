package com.pharmasys.controller;

import com.pharmasys.dto.request.ClienteRequestDto;
import com.pharmasys.model.Cliente;
import com.pharmasys.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ClienteController {
    
    private final ClienteService clienteService;
    
    @GetMapping
    public ResponseEntity<List<Cliente>> obtenerTodos() {
        return ResponseEntity.ok(clienteService.obtenerTodos());
    }
    
    @GetMapping("/activos")
    public ResponseEntity<List<Cliente>> obtenerActivos() {
        return ResponseEntity.ok(clienteService.obtenerActivos());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> obtenerPorId(@PathVariable Long id) {
        return clienteService.obtenerPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/dni/{dni}")
    public ResponseEntity<Cliente> obtenerPorDni(@PathVariable String dni) {
        return clienteService.obtenerPorDni(dni)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/buscar")
    public ResponseEntity<List<Cliente>> buscar(@RequestParam String termino) {
        return ResponseEntity.ok(clienteService.buscar(termino));
    }
    
    @PostMapping
    public ResponseEntity<Cliente> crear(@Valid @RequestBody ClienteRequestDto clienteRequest) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(clienteService.crear(toEntity(clienteRequest)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequestDto clienteRequest) {
        try {
            return ResponseEntity.ok(clienteService.actualizar(id, toEntity(clienteRequest)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            clienteService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Cliente toEntity(ClienteRequestDto dto) {
        Cliente cliente = new Cliente();
        cliente.setNombre(dto.getNombre().trim());
        cliente.setApellido(dto.getApellido().trim());
        cliente.setDni(trimToNull(dto.getDni()));
        cliente.setDireccion(trimToNull(dto.getDireccion()));
        cliente.setTelefono(trimToNull(dto.getTelefono()));
        cliente.setEmail(trimToNull(dto.getEmail()));
        cliente.setFechaNacimiento(dto.getFechaNacimiento());
        cliente.setActivo(dto.getActivo());
        return cliente;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
