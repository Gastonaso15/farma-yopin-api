package com.farmayopin.api.controller;

import com.farmayopin.api.dto.compra.CompraItemResponse;
import com.farmayopin.api.dto.compra.CompraResponse;
import com.farmayopin.api.exception.GlobalExceptionHandler;
import com.farmayopin.api.model.Rol;
import com.farmayopin.api.model.Usuario;
import com.farmayopin.api.security.CustomUserDetails;
import com.farmayopin.api.service.CompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CompraControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CompraService compraService;

    @InjectMocks
    private CompraController compraController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(compraController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void verHistoricoComprasPropio_Returns200() throws Exception {
        CompraResponse compra = CompraResponse.builder()
                .id(50L)
                .fecha(LocalDateTime.now())
                .total(new BigDecimal("150.00"))
                .clienteNombre("Juan")
                .clienteEmail("juan@test.com")
                .items(List.of(
                        CompraItemResponse.builder()
                                .id(1L)
                                .nombreProducto("Paracetamol")
                                .cantidad(1)
                                .precioUnitario(new BigDecimal("150.00"))
                                .subtotal(new BigDecimal("150.00"))
                                .build()
                ))
                .build();

        when(compraService.obtenerHistorialComprasUsuario(1L)).thenReturn(List.of(compra));

        mockMvc.perform(get("/api/compras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(50L))
                .andExpect(jsonPath("$[0].total").value(150.00))
                .andExpect(jsonPath("$[0].items[0].nombreProducto").value("Paracetamol"));
    }
}
