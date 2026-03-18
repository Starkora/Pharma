package com.pharmasys.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pharmasys.model.Compra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompraRequestDto {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdReferenceDto {
        @NotNull(message = "El id es obligatorio")
        private Long id;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetalleCompraRequestDto {
        @Valid
        @NotNull(message = "El producto es obligatorio")
        private IdReferenceDto producto;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        private Integer cantidad;

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a cero")
        private BigDecimal precioUnitario;

        @DecimalMin(value = "0.0", inclusive = true, message = "El subtotal no puede ser negativo")
        private BigDecimal subtotal;
    }

    @Valid
    @NotNull(message = "El proveedor es obligatorio")
    private IdReferenceDto proveedor;

    @Valid
    private IdReferenceDto usuario;

    private Compra.EstadoCompra estado;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;

    @DecimalMin(value = "0.0", inclusive = true, message = "El subtotal no puede ser negativo")
    private BigDecimal subtotal;

    @DecimalMin(value = "0.0", inclusive = true, message = "El impuesto no puede ser negativo")
    private BigDecimal impuesto;

    @DecimalMin(value = "0.0", inclusive = true, message = "El total no puede ser negativo")
    private BigDecimal total;

    @Valid
    @NotEmpty(message = "La compra debe tener al menos un detalle")
    private List<DetalleCompraRequestDto> detalles;
}