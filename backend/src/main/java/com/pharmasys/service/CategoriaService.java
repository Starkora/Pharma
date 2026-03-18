package com.pharmasys.service;

import com.pharmasys.model.Categoria;
import com.pharmasys.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoriaService {
    
    private final CategoriaRepository categoriaRepository;
    
    public List<Categoria> obtenerTodas() {
        return categoriaRepository.findAll();
    }
    
    public List<Categoria> obtenerActivas() {
        return categoriaRepository.findByActivoTrue();
    }
    
    public Optional<Categoria> obtenerPorId(Long id) {
        return categoriaRepository.findById(id);
    }
    
    public Categoria crear(Categoria categoria) {
        if (categoriaRepository.existsByNombre(categoria.getNombre())) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + categoria.getNombre());
        }
        return categoriaRepository.save(categoria);
    }
    
    public Categoria actualizar(Long id, Categoria categoria) {
        return categoriaRepository.findById(id)
            .map(c -> {
                c.setNombre(categoria.getNombre());
                c.setDescripcion(categoria.getDescripcion());
                c.setActivo(categoria.getActivo());
                return categoriaRepository.save(c);
            })
            .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));
    }
    
    public void eliminar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));
        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }
}
