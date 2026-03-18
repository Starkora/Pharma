package com.pharmasys.repository;

import com.pharmasys.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    
    Optional<Categoria> findByNombre(String nombre);
    
    List<Categoria> findByActivoTrue();
    
    boolean existsByNombre(String nombre);
}
