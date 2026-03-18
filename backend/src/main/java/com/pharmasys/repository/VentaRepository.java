package com.pharmasys.repository;

import com.pharmasys.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("""
        SELECT v
        FROM Venta v
        LEFT JOIN FETCH v.cliente
        ORDER BY v.fecha DESC
        """)
    List<Venta> findAllForList();

    @Query("""
        SELECT DISTINCT v
        FROM Venta v
        LEFT JOIN FETCH v.cliente
        LEFT JOIN FETCH v.usuario
        LEFT JOIN FETCH v.detalles d
        LEFT JOIN FETCH d.producto
        ORDER BY v.fecha DESC
        """)
    List<Venta> findAllWithRelations();
    
    Optional<Venta> findByNumeroVenta(String numeroVenta);
    
    List<Venta> findByClienteId(Long clienteId);
    
    List<Venta> findByUsuarioId(Long usuarioId);
    
    List<Venta> findByFechaBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    @Query("""
        SELECT DISTINCT v
        FROM Venta v
        LEFT JOIN FETCH v.cliente
        LEFT JOIN FETCH v.usuario
        LEFT JOIN FETCH v.detalles d
        LEFT JOIN FETCH d.producto
        WHERE v.fecha BETWEEN :fechaInicio AND :fechaFin
        ORDER BY v.fecha DESC
        """)
    List<Venta> findByFechaBetweenWithRelations(@Param("fechaInicio") LocalDateTime fechaInicio,
                                                @Param("fechaFin") LocalDateTime fechaFin);
    
    List<Venta> findByEstado(Venta.EstadoVenta estado);
    
    @Query("SELECT SUM(v.total) FROM Venta v WHERE v.fecha BETWEEN :fechaInicio AND :fechaFin AND v.estado = 'COMPLETADA'")
    BigDecimal calcularTotalVentasPorPeriodo(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                             @Param("fechaFin") LocalDateTime fechaFin);
}
