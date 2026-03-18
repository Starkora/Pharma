package com.pharmasys.repository;

import com.pharmasys.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    @Query("""
        SELECT DISTINCT c
        FROM Compra c
        LEFT JOIN FETCH c.proveedor
        LEFT JOIN FETCH c.usuario
        LEFT JOIN FETCH c.detalles d
        LEFT JOIN FETCH d.producto
        ORDER BY c.fecha DESC
        """)
    List<Compra> findAllWithRelations();
    
    Optional<Compra> findByNumeroCompra(String numeroCompra);
    
    List<Compra> findByProveedorId(Long proveedorId);
    
    List<Compra> findByUsuarioId(Long usuarioId);
    
    List<Compra> findByFechaBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    @Query("""
        SELECT DISTINCT c
        FROM Compra c
        LEFT JOIN FETCH c.proveedor
        LEFT JOIN FETCH c.usuario
        LEFT JOIN FETCH c.detalles d
        LEFT JOIN FETCH d.producto
        WHERE c.fecha BETWEEN :fechaInicio AND :fechaFin
        ORDER BY c.fecha DESC
        """)
    List<Compra> findByFechaBetweenWithRelations(@Param("fechaInicio") LocalDateTime fechaInicio,
                                                 @Param("fechaFin") LocalDateTime fechaFin);
    
    List<Compra> findByEstado(Compra.EstadoCompra estado);
    
    @Query("SELECT SUM(c.total) FROM Compra c WHERE c.fecha BETWEEN :fechaInicio AND :fechaFin AND c.estado = 'RECIBIDA'")
    BigDecimal calcularTotalComprasPorPeriodo(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                              @Param("fechaFin") LocalDateTime fechaFin);
}
