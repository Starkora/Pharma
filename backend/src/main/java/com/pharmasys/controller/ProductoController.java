package com.pharmasys.controller;

import com.pharmasys.dto.request.ProductoRequestDto;
import com.pharmasys.model.Categoria;
import com.pharmasys.model.Producto;
import com.pharmasys.model.Proveedor;
import com.pharmasys.service.BlobStorageService;
import com.pharmasys.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {
    
    private final ProductoService productoService;
    private final BlobStorageService blobStorageService;
    
    @GetMapping
    public ResponseEntity<List<Producto>> obtenerTodos() {
        List<Producto> productos = productoService.obtenerTodos();
        firmarImagenes(productos);
        return ResponseEntity.ok(productos);
    }
    
    @GetMapping("/activos")
    public ResponseEntity<List<Producto>> obtenerActivos() {
        List<Producto> productos = productoService.obtenerActivos();
        firmarImagenes(productos);
        return ResponseEntity.ok(productos);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerPorId(@PathVariable Long id) {
        return productoService.obtenerPorId(id)
            .map(this::firmarImagen)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Producto> obtenerPorCodigo(@PathVariable String codigo) {
        return productoService.obtenerPorCodigo(codigo)
            .map(this::firmarImagen)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/buscar")
    public ResponseEntity<List<Producto>> buscarPorNombre(@RequestParam String nombre) {
        List<Producto> productos = productoService.buscarPorNombre(nombre);
        firmarImagenes(productos);
        return ResponseEntity.ok(productos);
    }
    
    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<List<Producto>> obtenerPorCategoria(@PathVariable Long categoriaId) {
        List<Producto> productos = productoService.obtenerPorCategoria(categoriaId);
        firmarImagenes(productos);
        return ResponseEntity.ok(productos);
    }
    
    @GetMapping("/bajo-stock")
    public ResponseEntity<List<Producto>> obtenerBajoStock() {
        List<Producto> productos = productoService.obtenerProductosBajoStock();
        firmarImagenes(productos);
        return ResponseEntity.ok(productos);
    }
    
    @PostMapping
    public ResponseEntity<Producto> crear(@Valid @RequestBody ProductoRequestDto productoRequest) {
        try {
            Producto producto = toEntity(productoRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(productoService.crear(producto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequestDto productoRequest) {
        try {
            Producto producto = toEntity(productoRequest);
            productoService.obtenerPorId(id).ifPresent(existente -> {
                // En update normal (sin multipart), no persistir URLs temporales/SAS que vengan del frontend.
                if (existente.getImagenUrl() != null && !existente.getImagenUrl().isBlank()) {
                    producto.setImagenUrl(blobStorageService.normalizarUrlPersistible(existente.getImagenUrl()));
                } else {
                    producto.setImagenUrl(null);
                }
            });

            Producto actualizado = productoService.actualizar(id, producto);
            firmarImagen(actualizado);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            // Obtener producto para eliminar su imagen si existe
            Optional<Producto> producto = productoService.obtenerPorId(id);
            if (producto.isPresent() && producto.get().getImagenUrl() != null) {
                blobStorageService.eliminarImagenSiExiste(producto.get().getImagenUrl());
            }
            productoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/productos/con-imagen
     * Crear producto con imagen (FormData)
     */
    @PostMapping(value = "/con-imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crearConImagen(
            @Valid @RequestPart("producto") ProductoRequestDto productoRequest,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        try {
            Producto producto = toEntity(productoRequest);
            // Subir imagen a Azure Blob si existe
            if (imagen != null && !imagen.isEmpty()) {
                String imagenUrl = blobStorageService.subirImagenProducto(imagen);
                producto.setImagenUrl(blobStorageService.normalizarUrlPersistible(imagenUrl));
            }
            
            // Guardar producto
            Producto productoGuardado = productoService.crear(producto);
            firmarImagen(productoGuardado);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(productoGuardado);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No se pudo crear el producto");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * PUT /api/productos/{id}/con-imagen
     * Actualizar producto con imagen opcional (FormData)
     */
        @PutMapping(value = "/{id}/con-imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizarConImagen(
            @PathVariable Long id,
            @Valid @RequestPart("producto") ProductoRequestDto productoRequest,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        try {
            Producto productoActualizado = toEntity(productoRequest);
            // Obtener producto existente
            Producto productoExistente = productoService.obtenerPorId(id)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            
            // Actualizar ID
            productoActualizado.setId(id);
            
            // Subir nueva imagen a Azure Blob si existe
            if (imagen != null && !imagen.isEmpty()) {
                // Eliminar imagen anterior si existe
                if (productoExistente.getImagenUrl() != null) {
                    blobStorageService.eliminarImagenSiExiste(productoExistente.getImagenUrl());
                }
                // Guardar nueva imagen
                String imagenUrl = blobStorageService.subirImagenProducto(imagen);
                productoActualizado.setImagenUrl(blobStorageService.normalizarUrlPersistible(imagenUrl));
            } else {
                // Mantener imagen existente
                productoActualizado.setImagenUrl(blobStorageService.normalizarUrlPersistible(productoExistente.getImagenUrl()));
            }
            
            Producto resultado = productoService.actualizar(id, productoActualizado);
            firmarImagen(resultado);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No se pudo actualizar el producto");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Void> actualizarStock(@PathVariable Long id, @RequestParam Integer cantidad) {
        try {
            productoService.actualizarStock(id, cantidad);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private void firmarImagenes(List<Producto> productos) {
        if (productos == null || productos.isEmpty()) {
            return;
        }
        for (Producto producto : productos) {
            firmarImagen(producto);
        }
    }

    private Producto firmarImagen(Producto producto) {
        if (producto != null && producto.getImagenUrl() != null && !producto.getImagenUrl().isBlank()) {
            producto.setImagenUrl(blobStorageService.generarUrlLecturaConSas(producto.getImagenUrl()));
        }
        return producto;
    }

    private Producto toEntity(ProductoRequestDto dto) {
        Producto producto = new Producto();
        producto.setCodigo(dto.getCodigo().trim());
        producto.setNombre(dto.getNombre().trim());
        producto.setDescripcion(trimToNull(dto.getDescripcion()));

        if (dto.getCategoria() != null && dto.getCategoria().getId() != null) {
            Categoria categoria = new Categoria();
            categoria.setId(dto.getCategoria().getId());
            producto.setCategoria(categoria);
        } else {
            producto.setCategoria(null);
        }

        if (dto.getProveedor() != null && dto.getProveedor().getId() != null) {
            Proveedor proveedor = new Proveedor();
            proveedor.setId(dto.getProveedor().getId());
            producto.setProveedor(proveedor);
        } else {
            producto.setProveedor(null);
        }

        producto.setPrecioCompra(dto.getPrecioCompra());
        producto.setPrecioVenta(dto.getPrecioVenta());
        producto.setStock(dto.getStock());
        producto.setStockMinimo(dto.getStockMinimo());
        producto.setUnidadMedida(trimToNull(dto.getUnidadMedida()));
        producto.setRequiereReceta(dto.getRequiereReceta() != null ? dto.getRequiereReceta() : Boolean.FALSE);
        producto.setFechaVencimiento(dto.getFechaVencimiento());
        producto.setLote(trimToNull(dto.getLote()));
        producto.setImagenUrl(trimToNull(dto.getImagenUrl()));
        producto.setActivo(dto.getActivo() != null ? dto.getActivo() : Boolean.TRUE);
        return producto;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
