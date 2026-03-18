package com.pharmasys.service;

import com.pharmasys.model.Producto;
import com.pharmasys.model.Categoria;
import com.pharmasys.model.Proveedor;
import com.pharmasys.repository.CategoriaRepository;
import com.pharmasys.repository.ProductoRepository;
import com.pharmasys.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoService {
    
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    
    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }
    
    public List<Producto> obtenerActivos() {
        return productoRepository.findByActivoTrue();
    }
    
    public Optional<Producto> obtenerPorId(Long id) {
        return productoRepository.findById(id);
    }
    
    public Optional<Producto> obtenerPorCodigo(String codigo) {
        return productoRepository.findByCodigo(codigo);
    }
    
    public List<Producto> buscarPorNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCase(nombre);
    }
    
    public List<Producto> obtenerPorCategoria(Long categoriaId) {
        return productoRepository.findByCategoriaId(categoriaId);
    }
    
    public List<Producto> obtenerProductosBajoStock() {
        return productoRepository.findProductosBajoStock();
    }
    
    public Producto crear(Producto producto) {
        if (productoRepository.existsByCodigo(producto.getCodigo())) {
            throw new RuntimeException("Ya existe un producto con el código: " + producto.getCodigo());
        }
        return productoRepository.save(producto);
    }
    
    public Producto actualizar(Long id, Producto producto) {
        return productoRepository.findById(id)
            .map(p -> {
                p.setCodigo(producto.getCodigo());
                p.setNombre(producto.getNombre());
                p.setDescripcion(producto.getDescripcion());
                p.setCategoria(resolverCategoria(producto.getCategoria()));
                p.setProveedor(resolverProveedor(producto.getProveedor()));
                p.setPrecioCompra(producto.getPrecioCompra());
                p.setPrecioVenta(producto.getPrecioVenta());
                p.setStock(producto.getStock());
                p.setStockMinimo(producto.getStockMinimo());
                p.setUnidadMedida(producto.getUnidadMedida());
                p.setRequiereReceta(producto.getRequiereReceta());
                p.setFechaVencimiento(producto.getFechaVencimiento());
                p.setLote(producto.getLote());
                p.setImagenUrl(producto.getImagenUrl());
                p.setActivo(producto.getActivo());
                return productoRepository.save(p);
            })
            .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
    }

    private Categoria resolverCategoria(Categoria categoria) {
        if (categoria == null || categoria.getId() == null) {
            return null;
        }
        return categoriaRepository.findById(categoria.getId()).orElse(null);
    }

    private Proveedor resolverProveedor(Proveedor proveedor) {
        if (proveedor == null || proveedor.getId() == null) {
            return null;
        }
        return proveedorRepository.findById(proveedor.getId()).orElse(null);
    }
    
    public void eliminar(Long id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        producto.setActivo(false);
        productoRepository.save(producto);
    }
    
    public void actualizarStock(Long id, Integer cantidad) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        producto.setStock(producto.getStock() + cantidad);
        productoRepository.save(producto);
    }
}
