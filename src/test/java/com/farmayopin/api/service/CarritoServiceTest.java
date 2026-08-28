package com.farmayopin.api.service;

import com.farmayopin.api.dto.carrito.CarritoItemRequest;
import com.farmayopin.api.dto.carrito.CarritoItemUpdateRequest;
import com.farmayopin.api.dto.carrito.CarritoResponse;
import com.farmayopin.api.dto.compra.CompraResponse;
import com.farmayopin.api.exception.BadRequestException;
import com.farmayopin.api.exception.InsufficientStockException;
import com.farmayopin.api.exception.ResourceNotFoundException;
import com.farmayopin.api.model.*;
import com.farmayopin.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    @Mock
    private CarritoRepository carritoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CompraRepository compraRepository;

    @InjectMocks
    private CarritoService carritoService;

    private Usuario usuario;
    private Producto producto;
    private Carrito carrito;
    private ItemCarrito itemCarrito;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan Perez")
                .email("juan@test.com")
                .rol(Rol.CLIENTE)
                .build();

        producto = Producto.builder()
                .id(10L)
                .nombre("Ibuprofeno 400mg")
                .precio(new BigDecimal("100.00"))
                .stock(20)
                .build();

        carrito = Carrito.builder()
                .id(1L)
                .usuario(usuario)
                .items(new ArrayList<>())
                .build();

        itemCarrito = ItemCarrito.builder()
                .id(100L)
                .carrito(carrito)
                .producto(producto)
                .cantidad(2)
                .build();
    }

    @Test
    void obtenerCarrito_Success() {
        carrito.getItems().add(itemCarrito);
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));

        CarritoResponse response = carritoService.obtenerCarrito(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("200.00"), response.getTotal());
    }

    @Test
    void agregarItem_NewItem_Success() {
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(carritoRepository.save(any(Carrito.class))).thenReturn(carrito);

        CarritoItemRequest request = CarritoItemRequest.builder()
                .productoId(10L)
                .cantidad(3)
                .build();

        CarritoResponse response = carritoService.agregarItem(1L, request);

        assertNotNull(response);
        verify(carritoRepository).save(carrito);
    }

    @Test
    void agregarItem_ExistingItem_SumsQuantity() {
        carrito.getItems().add(itemCarrito);
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(carritoRepository.save(any(Carrito.class))).thenReturn(carrito);

        CarritoItemRequest request = CarritoItemRequest.builder()
                .productoId(10L)
                .cantidad(3)
                .build();

        CarritoResponse response = carritoService.agregarItem(1L, request);

        assertNotNull(response);
        assertEquals(5, itemCarrito.getCantidad());
    }

    @Test
    void agregarItem_InsufficientStock_ThrowsInsufficientStockException() {
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

        CarritoItemRequest request = CarritoItemRequest.builder()
                .productoId(10L)
                .cantidad(25) // Stock is 20
                .build();

        assertThrows(InsufficientStockException.class, () -> carritoService.agregarItem(1L, request));
    }

    @Test
    void actualizarCantidadItem_QuantityGreaterThanZero_UpdatesQuantity() {
        carrito.getItems().add(itemCarrito);
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(carritoRepository.save(any(Carrito.class))).thenReturn(carrito);

        CarritoItemUpdateRequest request = CarritoItemUpdateRequest.builder()
                .cantidad(5)
                .build();

        CarritoResponse response = carritoService.actualizarCantidadItem(1L, 100L, request);

        assertNotNull(response);
        assertEquals(5, itemCarrito.getCantidad());
    }

    @Test
    void actualizarCantidadItem_QuantityZero_RemovesItem() {
        carrito.getItems().add(itemCarrito);
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(carritoRepository.save(any(Carrito.class))).thenReturn(carrito);

        CarritoItemUpdateRequest request = CarritoItemUpdateRequest.builder()
                .cantidad(0)
                .build();

        CarritoResponse response = carritoService.actualizarCantidadItem(1L, 100L, request);

        assertNotNull(response);
        assertTrue(carrito.getItems().isEmpty());
    }

    @Test
    void eliminarItem_Success() {
        carrito.getItems().add(itemCarrito);
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(carritoRepository.save(any(Carrito.class))).thenReturn(carrito);

        CarritoResponse response = carritoService.eliminarItem(1L, 100L);

        assertNotNull(response);
        assertTrue(carrito.getItems().isEmpty());
    }

    @Test
    void pagarCarrito_Success_DeductsStockAndEmptiesCart() {
        carrito.getItems().add(itemCarrito);
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> {
            Compra c = invocation.getArgument(0);
            c.setId(500L);
            return c;
        });

        CompraResponse response = carritoService.pagarCarrito(1L);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals(new BigDecimal("200.00"), response.getTotal());
        assertEquals(18, producto.getStock()); // 20 - 2 = 18
        assertTrue(carrito.getItems().isEmpty());
        verify(compraRepository).save(any(Compra.class));
        verify(carritoRepository).save(carrito);
    }

    @Test
    void pagarCarrito_EmptyCart_ThrowsBadRequestException() {
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));

        assertThrows(BadRequestException.class, () -> carritoService.pagarCarrito(1L));
    }

    @Test
    void pagarCarrito_InsufficientStockAtCheckout_ThrowsInsufficientStockException() {
        producto.setStock(1);
        carrito.getItems().add(itemCarrito); // item quantity is 2
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));

        assertThrows(InsufficientStockException.class, () -> carritoService.pagarCarrito(1L));
    }
}
