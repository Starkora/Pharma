package com.pharmasys.service;

import com.pharmasys.model.Rol;
import com.pharmasys.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class RolService {

    private final RolRepository rolRepository;

    @Transactional(readOnly = true)
    public List<Rol> listarTodos() {
        return rolRepository.findAllByOrderByNombreAsc();
    }

    public Rol crear(Rol rol) {
        String nombre = rol.getNombre().trim().toUpperCase();
        if (rolRepository.existsByNombre(nombre)) {
            throw new IllegalArgumentException("El rol '" + nombre + "' ya existe");
        }
        rol.setNombre(nombre);
        return rolRepository.save(rol);
    }

    public void eliminar(Long id) {
        if (!rolRepository.existsById(id)) {
            throw new NoSuchElementException("Rol no encontrado");
        }
        rolRepository.deleteById(id);
    }
}
