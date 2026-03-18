package com.pharmasys.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "comprobantes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_comprobante", nullable = false, length = 10)
    private String tipoComprobante; // BOLETA / FACTURA

    @Column(nullable = false, length = 10)
    private String serie; // B001 / F001

    @Column(nullable = false)
    private Integer numero;

    @Column(name = "numero_completo", length = 20)
    private String numeroCompleto; // Ej: B001-00000001

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "venta_id")
    @JsonIgnoreProperties({"detalles", "cliente", "usuario", "hibernateLazyInitializer", "handler"})
    private Venta venta;

    @Column(name = "tipo_doc_comprador", length = 2)
    private String tipoDocComprador; // 0=sin doc, 1=DNI, 6=RUC

    @Column(name = "num_doc_comprador", length = 15)
    private String numDocComprador;

    @Column(name = "razon_social_comprador", length = 200)
    private String razonSocialComprador;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 20)
    private String estado; // EMITIDO / ERROR / ANULADO

    @Column(name = "link_pdf", length = 500)
    private String linkPdf;

    @Column(name = "link_xml", length = 500)
    private String linkXml;

    @Column(name = "link_cdr", length = 500)
    private String linkCdr;

    @Column(name = "codigo_hash", length = 200)
    private String codigoHash;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision = LocalDateTime.now();
}
