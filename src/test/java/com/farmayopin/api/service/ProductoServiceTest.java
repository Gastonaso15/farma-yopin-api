package com.farmayopin.api.service;

import com.farmayopin.api.dto.producto.ProductoCompraHistorialResponse;
import com.farmayopin.api.dto.producto.ProductoRequest;
import com.farmayopin.api.dto.producto.ProductoResponse;
import com.farmayopin.api.exception.ResourceNotFoundException;
import com.farmayopin.api.model.Compra;
import com.farmayopin.api.model.ItemCompra;
import com.farmayopin.api.model.Producto;
import com.farmayopin.api.model.Usuario;
import com.farmayopin.api.repository.ItemCompraRepository;
import com.farmayopin.api.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ItemCompraRepository itemCompraRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ProductoService productoService;

    private Producto producto;
    private ProductoRequest productoRequest;

    @BeforeEach
    void setUp() {
        producto = Producto.builder()
                .id(1L)
                .nombre("Paracetamol 500mg")
                .precio(new BigDecimal("150.50"))
                .detalle("Analgésico y antipirético")
                .foto("http://foto.jpg")
                .stock(50)
                .build();

        productoRequest = ProductoRequest.builder()
                .nombre("Paracetamol 500mg")
                .precio(new BigDecimal("150.50"))
                .detalle("Analgésico y antipirético")
                .foto("http://foto.jpg")
                .stock(50)
                .build();
    }

    @Test
    void listarProductos_ReturnsAllProducts() {
        when(productoRepository.findAll()).thenReturn(List.of(producto));

        List<ProductoResponse> result = productoService.listarProductos();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Paracetamol 500mg", result.get(0).getNombre());
    }

    @Test
    void obtenerPorId_Found_ReturnsProducto() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        ProductoResponse result = productoService.obtenerPorId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Paracetamol 500mg", result.getNombre());
    }

    @Test
    void obtenerPorId_NotFound_ThrowsResourceNotFoundException() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productoService.obtenerPorId(99L));
    }

    @Test
    void crearProducto_Success() {
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        ProductoResponse result = productoService.crearProducto(productoRequest);

        assertNotNull(result);
        assertEquals("Paracetamol 500mg", result.getNombre());
        assertEquals(new BigDecimal("150.50"), result.getPrecio());
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    void actualizarProducto_Success() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        ProductoRequest updateRequest = ProductoRequest.builder()
                .nombre("Paracetamol 1g")
                .precio(new BigDecimal("200.00"))
                .detalle("Doble concentración")
                .foto("http://foto2.jpg")
                .stock(100)
                .build();

        ProductoResponse result = productoService.actualizarProducto(1L, updateRequest);

        assertNotNull(result);
        verify(productoRepository).save(producto);
    }

    @Test
    void actualizarProducto_NotFound_ThrowsResourceNotFoundException() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productoService.actualizarProducto(99L, productoRequest));
    }

    @Test
    void obtenerHistorialComprasProducto_ReturnsHistory() {
        when(productoRepository.existsById(1L)).thenReturn(true);

        Usuario cliente = Usuario.builder().nombre("Maria Lopez").build();
        Compra compra = Compra.builder().id(10L).fecha(LocalDateTime.now()).usuario(cliente).build();
        ItemCompra item = ItemCompra.builder()
                .id(100L)
                .compra(compra)
                .producto(producto)
                .nombreProducto("Paracetamol 500mg")
                .cantidad(3)
                .precioUnitario(new BigDecimal("150.50"))
                .subtotal(new BigDecimal("451.50"))
                .build();

        when(itemCompraRepository.findByProductoIdOrderByCompraFechaDesc(1L)).thenReturn(List.of(item));

        List<ProductoCompraHistorialResponse> result = productoService.obtenerHistorialComprasProducto(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Maria Lopez", result.get(0).getCliente());
        assertEquals(3, result.get(0).getCantidad());
        assertEquals(new BigDecimal("451.50"), result.get(0).getSubtotal());
    }

    @Test
    void obtenerHistorialComprasProducto_ProductNotFound_ThrowsResourceNotFoundException() {
        when(productoRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> productoService.obtenerHistorialComprasProducto(99L));
    }

    @Test
    void actualizarImagenProducto_Success() {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "foto.png", "image/png", "test".getBytes()
        );

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(fileStorageService.getUploadDir()).thenReturn("img");
        when(fileStorageService.almacenarImagen(file, "producto_1")).thenReturn("img/producto_1_abc.png");
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductoResponse response = productoService.actualizarImagenProducto(1L, file);

        assertNotNull(response);
        assertEquals("img/producto_1_abc.png", response.getFoto());
        verify(fileStorageService).almacenarImagen(file, "producto_1");
    }

    @Test
    void eliminarImagenProducto_Success() {
        producto.setFoto("img/antigua.jpg");
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductoResponse response = productoService.eliminarImagenProducto(1L);

        assertNotNull(response);
        assertNull(response.getFoto());
        verify(fileStorageService).eliminarImagen("img/antigua.jpg");
    }
}
