package com.pharmasys.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;
    
    @Column(nullable = false, length = 200)
    private String nombre;
    
    @Column(length = 500)
    private String descripcion;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Categoria categoria;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Proveedor proveedor;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioCompra;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta;
    
    @Column(nullable = false)
    private Integer stock = 0;
    
    @Column(nullable = false)
    private Integer stockMinimo = 0;
    
    @Column(length = 50)
    private String unidadMedida;
    
    @Column(name = "requiere_receta")
    private Boolean requiereReceta = false;
    
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
    
    @Column(length = 100)
    private String lote;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;
    
    @Column(nullable = false)
    private Boolean activo = true;
    
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    @Column(name = "ultima_modificacion")
    private LocalDateTime ultimaModificacion;
    
    @PreUpdate
    public void preUpdate() {
        this.ultimaModificacion = LocalDateTime.now();
    }
}
