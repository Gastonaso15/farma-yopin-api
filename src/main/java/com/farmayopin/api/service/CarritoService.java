package com.farmayopin.api.service;

import com.farmayopin.api.dto.carrito.CarritoItemRequest;
import com.farmayopin.api.dto.carrito.CarritoItemResponse;
import com.farmayopin.api.dto.carrito.CarritoItemUpdateRequest;
import com.farmayopin.api.dto.carrito.CarritoResponse;
import com.farmayopin.api.dto.compra.CompraItemResponse;
import com.farmayopin.api.dto.compra.CompraResponse;
import com.farmayopin.api.exception.BadRequestException;
import com.farmayopin.api.exception.InsufficientStockException;
import com.farmayopin.api.exception.ResourceNotFoundException;
import com.farmayopin.api.model.*;
import com.farmayopin.api.repository.CarritoRepository;
import com.farmayopin.api.repository.CompraRepository;
import com.farmayopin.api.repository.ProductoRepository;
import com.farmayopin.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CompraRepository compraRepository;

    public CarritoService(
            CarritoRepository carritoRepository,
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository,
            CompraRepository compraRepository) {
        this.carritoRepository = carritoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.compraRepository = compraRepository;
    }

    @Transactional
    public Carrito obtenerOcrearCarritoEntidad(Long usuarioId) {
        return carritoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    Usuario usuario = usuarioRepository.findById(usuarioId)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
                    Carrito nuevoCarrito = Carrito.builder()
                            .usuario(usuario)
                            .items(new ArrayList<>())
                            .build();
                    return carritoRepository.save(nuevoCarrito);
                });
    }

    @Transactional
    public CarritoResponse obtenerCarrito(Long usuarioId) {
        Carrito carrito = obtenerOcrearCarritoEntidad(usuarioId);
        return mapToCarritoResponse(carrito);
    }

    @Transactional
    public CarritoResponse agregarItem(Long usuarioId, CarritoItemRequest request) {
        Carrito carrito = obtenerOcrearCarritoEntidad(usuarioId);

        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + request.getProductoId()));

        Optional<ItemCarrito> itemExistenteOpt = carrito.getItems().stream()
                .filter(item -> item.getProducto().getId().equals(producto.getId()))
                .findFirst();

        int cantidadFinal = request.getCantidad();
        if (itemExistenteOpt.isPresent()) {
            cantidadFinal += itemExistenteOpt.get().getCantidad();
        }

        if (producto.getStock() < cantidadFinal) {
            throw new InsufficientStockException(
                    String.format("Stock insuficiente para el producto '%s'. Stock disponible: %d, cantidad solicitada: %d",
                            producto.getNombre(), producto.getStock(), cantidadFinal)
            );
        }

        if (itemExistenteOpt.isPresent()) {
            itemExistenteOpt.get().setCantidad(cantidadFinal);
        } else {
            ItemCarrito nuevoItem = ItemCarrito.builder()
                    .carrito(carrito)
                    .producto(producto)
                    .cantidad(request.getCantidad())
                    .build();
            carrito.getItems().add(nuevoItem);
        }

        Carrito guardado = carritoRepository.save(carrito);
        return mapToCarritoResponse(guardado);
    }

    @Transactional
    public CarritoResponse actualizarCantidadItem(Long usuarioId, Long itemId, CarritoItemUpdateRequest request) {
        Carrito carrito = obtenerOcrearCarritoEntidad(usuarioId);

        ItemCarrito item = carrito.getItems().stream()
                .filter(i -> i.getId() != null && i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado en el carrito con id: " + itemId));

        if (request.getCantidad() <= 0) {
            carrito.getItems().remove(item);
        } else {
            if (item.getProducto().getStock() < request.getCantidad()) {
                throw new InsufficientStockException(
                        String.format("Stock insuficiente para el producto '%s'. Stock disponible: %d, cantidad solicitada: %d",
                                item.getProducto().getNombre(), item.getProducto().getStock(), request.getCantidad())
                );
            }
            item.setCantidad(request.getCantidad());
        }

        Carrito guardado = carritoRepository.save(carrito);
        return mapToCarritoResponse(guardado);
    }

    @Transactional
    public CarritoResponse eliminarItem(Long usuarioId, Long itemId) {
        Carrito carrito = obtenerOcrearCarritoEntidad(usuarioId);

        boolean removido = carrito.getItems().removeIf(i -> i.getId() != null && i.getId().equals(itemId));
        if (!removido) {
            throw new ResourceNotFoundException("Ítem no encontrado en el carrito con id: " + itemId);
        }

        Carrito guardado = carritoRepository.save(carrito);
        return mapToCarritoResponse(guardado);
    }

    @Transactional
    public CompraResponse pagarCarrito(Long usuarioId) {
        Carrito carrito = obtenerOcrearCarritoEntidad(usuarioId);

        if (carrito.getItems().isEmpty()) {
            throw new BadRequestException("No se puede pagar un carrito vacío");
        }

        // Validar stock para cada ítem
        for (ItemCarrito item : carrito.getItems()) {
            Producto prod = item.getProducto();
            if (prod.getStock() < item.getCantidad()) {
                throw new InsufficientStockException(
                        String.format("Stock insuficiente para el producto '%s'. Stock disponible: %d, cantidad requerida: %d",
                                prod.getNombre(), prod.getStock(), item.getCantidad())
                );
            }
        }

        // Crear la compra
        BigDecimal total = carrito.calcularTotal();
        Compra compra = Compra.builder()
                .usuario(carrito.getUsuario())
                .fecha(LocalDateTime.now())
                .total(total)
                .items(new ArrayList<>())
                .build();

        for (ItemCarrito item : carrito.getItems()) {
            Producto prod = item.getProducto();
            // Descontar stock
            prod.setStock(prod.getStock() - item.getCantidad());

            ItemCompra itemCompra = ItemCompra.builder()
                    .compra(compra)
                    .producto(prod)
                    .nombreProducto(prod.getNombre())
                    .cantidad(item.getCantidad())
                    .precioUnitario(prod.getPrecio())
                    .subtotal(item.calcularSubtotal())
                    .build();

            compra.getItems().add(itemCompra);
        }

        Compra compraGuardada = compraRepository.save(compra);

        // Vaciar el carrito
        carrito.getItems().clear();
        carritoRepository.save(carrito);

        return mapToCompraResponse(compraGuardada);
    }

    private CarritoResponse mapToCarritoResponse(Carrito carrito) {
        List<CarritoItemResponse> itemsResponse = carrito.getItems().stream()
                .map(item -> CarritoItemResponse.builder()
                        .id(item.getId())
                        .productoId(item.getProducto().getId())
                        .nombreProducto(item.getProducto().getNombre())
                        .foto(item.getProducto().getFoto())
                        .precioUnitario(item.getProducto().getPrecio())
                        .cantidad(item.getCantidad())
                        .subtotal(item.calcularSubtotal())
                        .build())
                .collect(Collectors.toList());

        return CarritoResponse.builder()
                .id(carrito.getId())
                .items(itemsResponse)
                .total(carrito.calcularTotal())
                .build();
    }

    private CompraResponse mapToCompraResponse(Compra compra) {
        List<CompraItemResponse> items = compra.getItems().stream()
                .map(i -> CompraItemResponse.builder()
                        .id(i.getId())
                        .productoId(i.getProducto() != null ? i.getProducto().getId() : null)
                        .nombreProducto(i.getNombreProducto())
                        .cantidad(i.getCantidad())
                        .precioUnitario(i.getPrecioUnitario())
                        .subtotal(i.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return CompraResponse.builder()
                .id(compra.getId())
                .fecha(compra.getFecha())
                .total(compra.getTotal())
                .clienteNombre(compra.getUsuario() != null ? compra.getUsuario().getNombre() : null)
                .clienteEmail(compra.getUsuario() != null ? compra.getUsuario().getEmail() : null)
                .items(items)
                .build();
    }
}
