package com.pharmasys.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class VentaRequestDto {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdReferenceDto {
        @NotNull(message = "El id es obligatorio")
        private Long id;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetalleVentaRequestDto {
        @Valid
        @NotNull(message = "El producto es obligatorio")
        private IdReferenceDto producto;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        private Integer cantidad;

        @DecimalMin(value = "0.0", inclusive = true, message = "El descuento no puede ser negativo")
        private BigDecimal descuento;

        @DecimalMin(value = "0.0", inclusive = true, message = "El precio unitario no puede ser negativo")
        private BigDecimal precioUnitario;

        @DecimalMin(value = "0.0", inclusive = true, message = "El subtotal no puede ser negativo")
        private BigDecimal subtotal;
    }

    @Valid
    private IdReferenceDto cliente;

    @Valid
    private IdReferenceDto usuario;

    @DecimalMin(value = "0.0", inclusive = true, message = "El descuento no puede ser negativo")
    private BigDecimal descuento;

    @Size(max = 50, message = "El método de pago no puede exceder 50 caracteres")
    private String metodoPago;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;

    @DecimalMin(value = "0.0", inclusive = true, message = "El subtotal no puede ser negativo")
    private BigDecimal subtotal;

    @DecimalMin(value = "0.0", inclusive = true, message = "El impuesto no puede ser negativo")
    private BigDecimal impuesto;

    @DecimalMin(value = "0.0", inclusive = true, message = "El total no puede ser negativo")
    private BigDecimal total;

    @Valid
    @NotEmpty(message = "La venta debe tener al menos un detalle")
    private List<DetalleVentaRequestDto> detalles;
}