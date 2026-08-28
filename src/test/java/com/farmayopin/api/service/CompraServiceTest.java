package com.farmayopin.api.service;

import com.farmayopin.api.dto.compra.CompraResponse;
import com.farmayopin.api.exception.ResourceNotFoundException;
import com.farmayopin.api.model.*;
import com.farmayopin.api.repository.CompraRepository;
import com.farmayopin.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CompraService compraService;

    private Usuario usuario;
    private Compra compra;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan Perez")
                .email("juan@test.com")
                .rol(Rol.CLIENTE)
                .build();

        compra = Compra.builder()
                .id(100L)
                .usuario(usuario)
                .fecha(LocalDateTime.now())
                .total(new BigDecimal("500.00"))
                .items(List.of(
                        ItemCompra.builder()
                                .id(1L)
                                .nombreProducto("Aspirina")
                                .cantidad(2)
                                .precioUnitario(new BigDecimal("250.00"))
                                .subtotal(new BigDecimal("500.00"))
                                .build()
                ))
                .build();
    }

    @Test
    void obtenerHistorialComprasUsuario_UserExists_ReturnsPurchases() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(compraRepository.findByUsuarioIdOrderByFechaDesc(1L)).thenReturn(List.of(compra));

        List<CompraResponse> result = compraService.obtenerHistorialComprasUsuario(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
        assertEquals(new BigDecimal("500.00"), result.get(0).getTotal());
        assertEquals("Juan Perez", result.get(0).getClienteNombre());
        assertEquals(1, result.get(0).getItems().size());
        assertEquals("Aspirina", result.get(0).getItems().get(0).getNombreProducto());
    }

    @Test
    void obtenerHistorialComprasUsuario_UserNotFound_ThrowsResourceNotFoundException() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> compraService.obtenerHistorialComprasUsuario(99L));
    }
}
