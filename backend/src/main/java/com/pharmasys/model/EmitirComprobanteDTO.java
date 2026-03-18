package com.pharmasys.model;

import lombok.Data;

@Data
public class EmitirComprobanteDTO {
    private Long ventaId;
    private String tipoComprobante;   // BOLETA / FACTURA
    private String tipoDocComprador;  // 0=sin doc, 1=DNI, 6=RUC
    private String numDocComprador;
    private String razonSocialComprador;
}
