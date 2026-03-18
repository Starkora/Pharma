package com.pharmasys.service;

import com.pharmasys.model.Cliente;
import com.pharmasys.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ClienteService {
    
    private final ClienteRepository clienteRepository;
    
    public List<Cliente> obtenerTodos() {
        return clienteRepository.findAll();
    }
    
    public List<Cliente> obtenerActivos() {
        return clienteRepository.findByActivoTrue();
    }
    
    public Optional<Cliente> obtenerPorId(Long id) {
        return clienteRepository.findById(id);
    }
    
    public Optional<Cliente> obtenerPorDni(String dni) {
        return clienteRepository.findByDni(dni);
    }
    
    public List<Cliente> buscar(String termino) {
        return clienteRepository.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(termino, termino);
    }
    
    public Cliente crear(Cliente cliente) {
        if (clienteRepository.existsByDni(cliente.getDni())) {
            throw new IllegalArgumentException("Ya existe un cliente con el DNI: " + cliente.getDni());
        }
        return clienteRepository.save(cliente);
    }
    
    public Cliente actualizar(Long id, Cliente cliente) {
        return clienteRepository.findById(id)
            .map(c -> {
                c.setNombre(cliente.getNombre());
                c.setApellido(cliente.getApellido());
                c.setDireccion(cliente.getDireccion());
                c.setTelefono(cliente.getTelefono());
                c.setEmail(cliente.getEmail());
                c.setFechaNacimiento(cliente.getFechaNacimiento());
                c.setActivo(cliente.getActivo());
                return clienteRepository.save(c);
            })
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
    }
    
    public void eliminar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }
}
