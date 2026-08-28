package com.farmayopin.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmayopin.api.dto.carrito.CarritoItemRequest;
import com.farmayopin.api.dto.carrito.CarritoItemResponse;
import com.farmayopin.api.dto.carrito.CarritoItemUpdateRequest;
import com.farmayopin.api.dto.carrito.CarritoResponse;
import com.farmayopin.api.dto.compra.CompraResponse;
import com.farmayopin.api.exception.GlobalExceptionHandler;
import com.farmayopin.api.model.Rol;
import com.farmayopin.api.model.Usuario;
import com.farmayopin.api.security.CustomUserDetails;
import com.farmayopin.api.service.CarritoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

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
class CarritoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CarritoService carritoService;

    @InjectMocks
    private CarritoController carritoController;

    private ObjectMapper objectMapper;
    private CustomUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan")
                .email("juan@test.com")
                .rol(Rol.CLIENTE)
                .build();
        mockUserDetails = new CustomUserDetails(usuario);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUserDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(carritoController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void verCarrito_Returns200() throws Exception {
        CarritoResponse response = CarritoResponse.builder()
                .id(1L)
                .total(new BigDecimal("100.00"))
                .items(List.of(
                        CarritoItemResponse.builder()
                                .id(10L)
                                .productoId(5L)
                                .nombreProducto("Ibuprofeno")
                                .precioUnitario(new BigDecimal("50.00"))
                                .cantidad(2)
                                .subtotal(new BigDecimal("100.00"))
                                .build()
                ))
                .build();

        when(carritoService.obtenerCarrito(1L)).thenReturn(response);

        mockMvc.perform(get("/api/carrito"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.total").value(100.00))
                .andExpect(jsonPath("$.items[0].nombreProducto").value("Ibuprofeno"));
    }

    @Test
    void agregarAlCarrito_Returns200() throws Exception {
        CarritoItemRequest request = CarritoItemRequest.builder()
                .productoId(5L)
                .cantidad(2)
                .build();

        CarritoResponse response = CarritoResponse.builder()
                .id(1L)
                .total(new BigDecimal("100.00"))
                .items(List.of())
                .build();

        when(carritoService.agregarItem(eq(1L), any(CarritoItemRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/carrito/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void editarItemCarrito_Returns200() throws Exception {
        CarritoItemUpdateRequest request = CarritoItemUpdateRequest.builder()
                .cantidad(4)
                .build();

        CarritoResponse response = CarritoResponse.builder()
                .id(1L)
                .total(new BigDecimal("200.00"))
                .items(List.of())
                .build();

        when(carritoService.actualizarCantidadItem(eq(1L), eq(10L), any(CarritoItemUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/carrito/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void eliminarItemCarrito_Returns200() throws Exception {
        CarritoResponse response = CarritoResponse.builder()
                .id(1L)
                .total(BigDecimal.ZERO)
                .items(List.of())
                .build();

        when(carritoService.eliminarItem(1L, 10L)).thenReturn(response);

        mockMvc.perform(delete("/api/carrito/items/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void pagarCarrito_Returns201() throws Exception {
        CompraResponse compraResponse = CompraResponse.builder()
                .id(100L)
                .fecha(LocalDateTime.now())
                .total(new BigDecimal("100.00"))
                .clienteNombre("Juan")
                .items(List.of())
                .build();

        when(carritoService.pagarCarrito(1L)).thenReturn(compraResponse);

        mockMvc.perform(post("/api/carrito/pago"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.total").value(100.00));
    }
}
