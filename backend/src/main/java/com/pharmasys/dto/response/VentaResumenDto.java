package com.pharmasys.dto.response;

import com.pharmasys.model.Venta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaResumenDto {

    private Long id;
    private String numeroVenta;
    private LocalDateTime fecha;
    private ClienteResumenDto cliente;
    private BigDecimal total;
    private String metodoPago;
    private Venta.EstadoVenta estado;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteResumenDto {
        private Long id;
        private String nombre;
        private String apellido;
    }
}
