package com.farmayopin.api.service;

import com.farmayopin.api.dto.producto.ProductoCompraHistorialResponse;
import com.farmayopin.api.dto.producto.ProductoRequest;
import com.farmayopin.api.dto.producto.ProductoResponse;
import com.farmayopin.api.exception.ResourceNotFoundException;
import com.farmayopin.api.model.ItemCompra;
import com.farmayopin.api.model.Producto;
import com.farmayopin.api.repository.ItemCompraRepository;
import com.farmayopin.api.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ItemCompraRepository itemCompraRepository;

    public ProductoService(ProductoRepository productoRepository, ItemCompraRepository itemCompraRepository) {
        this.productoRepository = productoRepository;
        this.itemCompraRepository = itemCompraRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarProductos() {
        return productoRepository.findAll().stream()
                .map(this::mapToProductoResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        return mapToProductoResponse(producto);
    }

    @Transactional
    public ProductoResponse crearProducto(ProductoRequest request) {
        Producto producto = Producto.builder()
                .nombre(request.getNombre())
                .precio(request.getPrecio())
                .detalle(request.getDetalle())
                .foto(request.getFoto())
                .stock(request.getStock())
                .build();

        Producto guardado = productoRepository.save(producto);
        return mapToProductoResponse(guardado);
    }

    @Transactional
    public ProductoResponse actualizarProducto(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));

        producto.setNombre(request.getNombre());
        producto.setPrecio(request.getPrecio());
        producto.setDetalle(request.getDetalle());
        producto.setFoto(request.getFoto());
        producto.setStock(request.getStock());

        Producto actualizado = productoRepository.save(producto);
        return mapToProductoResponse(actualizado);
    }

    @Transactional(readOnly = true)
    public List<ProductoCompraHistorialResponse> obtenerHistorialComprasProducto(Long productoId) {
        if (!productoRepository.existsById(productoId)) {
            throw new ResourceNotFoundException("Producto no encontrado con id: " + productoId);
        }

        List<ItemCompra> items = itemCompraRepository.findByProductoIdOrderByCompraFechaDesc(productoId);

        return items.stream()
                .map(item -> ProductoCompraHistorialResponse.builder()
                        .compraId(item.getCompra() != null ? item.getCompra().getId() : null)
                        .fecha(item.getCompra() != null ? item.getCompra().getFecha() : null)
                        .cantidad(item.getCantidad())
                        .cliente(item.getCompra() != null && item.getCompra().getUsuario() != null
                                ? item.getCompra().getUsuario().getNombre()
                                : "Desconocido")
                        .precioUnitario(item.getPrecioUnitario())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());
    }

    private ProductoResponse mapToProductoResponse(Producto producto) {
        return ProductoResponse.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .precio(producto.getPrecio())
                .detalle(producto.getDetalle())
                .foto(producto.getFoto())
                .stock(producto.getStock())
                .build();
    }
}
