package com.farmayopin.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmayopin.api.dto.producto.ProductoCompraHistorialResponse;
import com.farmayopin.api.dto.producto.ProductoRequest;
import com.farmayopin.api.dto.producto.ProductoResponse;
import com.farmayopin.api.exception.GlobalExceptionHandler;
import com.farmayopin.api.exception.ResourceNotFoundException;
import com.farmayopin.api.service.FileStorageService;
import com.farmayopin.api.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductoService productoService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ProductoController productoController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void listarProductos_Returns200() throws Exception {
        ProductoResponse p = ProductoResponse.builder()
                .id(1L)
                .nombre("Amoxicilina")
                .precio(new BigDecimal("300.00"))
                .detalle("Antibiótico")
                .stock(15)
                .build();

        when(productoService.listarProductos()).thenReturn(List.of(p));

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nombre").value("Amoxicilina"));
    }

    @Test
    void obtenerPorId_Success_Returns200() throws Exception {
        ProductoResponse p = ProductoResponse.builder()
                .id(1L)
                .nombre("Amoxicilina")
                .precio(new BigDecimal("300.00"))
                .detalle("Antibiótico")
                .stock(15)
                .build();

        when(productoService.obtenerPorId(1L)).thenReturn(p);

        mockMvc.perform(get("/api/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Amoxicilina"));
    }

    @Test
    void obtenerPorId_NotFound_Returns404() throws Exception {
        when(productoService.obtenerPorId(99L))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado con id: 99"));

        mockMvc.perform(get("/api/productos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void crearProducto_Success_Returns201() throws Exception {
        ProductoRequest request = ProductoRequest.builder()
                .nombre("Ibuprofeno")
                .precio(new BigDecimal("150.00"))
                .detalle("Antiinflamatorio")
                .stock(20)
                .build();

        ProductoResponse response = ProductoResponse.builder()
                .id(2L)
                .nombre("Ibuprofeno")
                .precio(new BigDecimal("150.00"))
                .detalle("Antiinflamatorio")
                .stock(20)
                .build();

        when(productoService.crearProducto(any(ProductoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.nombre").value("Ibuprofeno"));
    }

    @Test
    void actualizarProducto_Success_Returns200() throws Exception {
        ProductoRequest request = ProductoRequest.builder()
                .nombre("Ibuprofeno Forte")
                .precio(new BigDecimal("200.00"))
                .detalle("Antiinflamatorio 600mg")
                .stock(30)
                .build();

        ProductoResponse response = ProductoResponse.builder()
                .id(2L)
                .nombre("Ibuprofeno Forte")
                .precio(new BigDecimal("200.00"))
                .detalle("Antiinflamatorio 600mg")
                .stock(30)
                .build();

        when(productoService.actualizarProducto(eq(2L), any(ProductoRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/productos/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.nombre").value("Ibuprofeno Forte"));
    }

    @Test
    void obtenerHistorialComprasProducto_Returns200() throws Exception {
        ProductoCompraHistorialResponse item = ProductoCompraHistorialResponse.builder()
                .compraId(10L)
                .fecha(LocalDateTime.now())
                .cantidad(2)
                .cliente("Carlos")
                .precioUnitario(new BigDecimal("150.00"))
                .subtotal(new BigDecimal("300.00"))
                .build();

        when(productoService.obtenerHistorialComprasProducto(2L)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/productos/2/compras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].compraId").value(10L))
                .andExpect(jsonPath("$[0].cliente").value("Carlos"))
                .andExpect(jsonPath("$[0].cantidad").value(2));
    }

    @Test
    void subirImagenProducto_Returns200() throws Exception {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "producto.png", "image/png", "sample bytes".getBytes()
        );

        ProductoResponse response = ProductoResponse.builder()
                .id(1L)
                .nombre("Amoxicilina")
                .foto("img/producto_1_abc.png")
                .precio(new BigDecimal("300.00"))
                .stock(10)
                .build();

        when(productoService.actualizarImagenProducto(eq(1L), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/productos/1/imagen").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.foto").value("img/producto_1_abc.png"));
    }

    @Test
    void uploadImagenGeneral_Returns200() throws Exception {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "general.jpg", "image/jpeg", "image bytes".getBytes()
        );

        when(fileStorageService.almacenarImagen(any(), eq("upload"))).thenReturn("img/upload_123.jpg");

        mockMvc.perform(multipart("/api/productos/imagenes/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rutaRelativa").value("img/upload_123.jpg"))
                .andExpect(jsonPath("$.mensaje").value("Imagen subida exitosamente."));
    }

    @Test
    void eliminarImagenProducto_Returns200() throws Exception {
        ProductoResponse response = ProductoResponse.builder()
                .id(1L)
                .nombre("Amoxicilina")
                .foto(null)
                .precio(new BigDecimal("300.00"))
                .stock(10)
                .build();

        when(productoService.eliminarImagenProducto(1L)).thenReturn(response);

        mockMvc.perform(delete("/api/productos/1/imagen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.foto").doesNotExist());
    }
}
