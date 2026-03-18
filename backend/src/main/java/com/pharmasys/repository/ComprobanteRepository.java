package com.pharmasys.repository;

import com.pharmasys.model.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    List<Comprobante> findAllByOrderByFechaEmisionDesc();

    Optional<Comprobante> findByVenta_Id(Long ventaId);

    List<Comprobante> findAllByVenta_IdOrderByFechaEmisionDesc(Long ventaId);

    @Query("SELECT COALESCE(MAX(c.numero), 0) FROM Comprobante c WHERE c.serie = :serie")
    Integer findMaxNumeroBySerie(@Param("serie") String serie);
}
