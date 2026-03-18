package com.pharmasys.service;

import com.pharmasys.model.Proveedor;
import com.pharmasys.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProveedorService {
    
    private final ProveedorRepository proveedorRepository;
    
    public List<Proveedor> obtenerTodos() {
        return proveedorRepository.findAll();
    }
    
    public List<Proveedor> obtenerActivos() {
        return proveedorRepository.findByActivoTrue();
    }
    
    public Optional<Proveedor> obtenerPorId(Long id) {
        return proveedorRepository.findById(id);
    }
    
    public List<Proveedor> buscarPorNombre(String nombre) {
        return proveedorRepository.findByNombreContainingIgnoreCase(nombre);
    }
    
    public Proveedor crear(Proveedor proveedor) {
        if (proveedor.getRuc() != null && proveedorRepository.existsByRuc(proveedor.getRuc())) {
            throw new RuntimeException("Ya existe un proveedor con el RUC: " + proveedor.getRuc());
        }
        return proveedorRepository.save(proveedor);
    }
    
    public Proveedor actualizar(Long id, Proveedor proveedor) {
        return proveedorRepository.findById(id)
            .map(p -> {
                p.setNombre(proveedor.getNombre());
                p.setDireccion(proveedor.getDireccion());
                p.setTelefono(proveedor.getTelefono());
                p.setEmail(proveedor.getEmail());
                p.setPersonaContacto(proveedor.getPersonaContacto());
                p.setActivo(proveedor.getActivo());
                return proveedorRepository.save(p);
            })
            .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));
    }
    
    public void eliminar(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));
        proveedor.setActivo(false);
        proveedorRepository.save(proveedor);
    }
}
