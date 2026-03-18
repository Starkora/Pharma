package com.pharmasys.repository;

import com.pharmasys.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
    Optional<Producto> findByCodigo(String codigo);
    
    List<Producto> findByActivoTrue();
    
    List<Producto> findByNombreContainingIgnoreCase(String nombre);
    
    List<Producto> findByCategoriaId(Long categoriaId);
    
    List<Producto> findByProveedorId(Long proveedorId);
    
    @Query("SELECT p FROM Producto p WHERE p.stock <= p.stockMinimo AND p.activo = true")
    List<Producto> findProductosBajoStock();
    
    boolean existsByCodigo(String codigo);
}
