package com.pharmasys.dto.response;

import com.pharmasys.model.Compra;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompraResponseDto {

    private Long id;
    private String numeroCompra;
    private LocalDateTime fecha;
    private ProveedorResponseDto proveedor;
    private BigDecimal subtotal;
    private BigDecimal impuesto;
    private BigDecimal total;
    private Compra.EstadoCompra estado;
    private String observaciones;
    private List<DetalleCompraResponseDto> detalles;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProveedorResponseDto {
        private Long id;
        private String nombre;
        private String ruc;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoResponseDto {
        private Long id;
        private String nombre;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleCompraResponseDto {
        private Long id;
        private ProductoResponseDto producto;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
    }
}
