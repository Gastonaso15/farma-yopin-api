package com.farmayopin.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmayopin.api.dto.carrito.CarritoItemRequest;
import com.farmayopin.api.dto.carrito.CarritoItemUpdateRequest;
import com.farmayopin.api.model.Producto;
import com.farmayopin.api.model.Rol;
import com.farmayopin.api.model.Usuario;
import com.farmayopin.api.repository.*;
import com.farmayopin.api.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CarritoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private ItemCarritoRepository itemCarritoRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ItemCompraRepository itemCompraRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private String clienteToken;
    private Long clienteId;
    private Producto productoA;

    @BeforeEach
    void setUp() {
        itemCompraRepository.deleteAll();
        compraRepository.deleteAll();
        itemCarritoRepository.deleteAll();
        carritoRepository.deleteAll();
        usuarioRepository.deleteAll();
        productoRepository.deleteAll();

        Usuario cliente = Usuario.builder()
                .nombre("Cliente Carrito")
                .email("carrito_cliente@farmayopin.com")
                .password(passwordEncoder.encode("cliente123"))
                .rol(Rol.CLIENTE)
                .build();
        cliente = usuarioRepository.save(cliente);
        clienteId = cliente.getId();
        clienteToken = jwtUtil.generateToken(cliente.getId(), cliente.getEmail(), cliente.getRol(), cliente.getNombre());

        productoA = Producto.builder()
                .nombre("Vitamina C 1000mg")
                .precio(new BigDecimal("120.00"))
                .detalle("Suplemento vitamínico")
                .foto("http://img.com/vitc.jpg")
                .stock(10)
                .build();
        productoA = productoRepository.save(productoA);
    }

    @Test
    void carritoOperations_IntegrationWithDB() throws Exception {
        // 1. Ver carrito vacío
        mockMvc.perform(get("/api/carrito")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());

        // 2. Agregar ítem al carrito (qty 2)
        CarritoItemRequest addReq = CarritoItemRequest.builder()
                .productoId(productoA.getId())
                .cantidad(2)
                .build();

        String res = mockMvc.perform(post("/api/carrito/items")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(240.00))
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andReturn().getResponse().getContentAsString();

        Long itemId = objectMapper.readTree(res).get("items").get(0).get("id").asLong();

        // 3. Agregar mismo producto nuevamente (qty 3) -> debe sumar cantidad a 5
        CarritoItemRequest addAgainReq = CarritoItemRequest.builder()
                .productoId(productoA.getId())
                .cantidad(3)
                .build();

        mockMvc.perform(post("/api/carrito/items")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addAgainReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(600.00))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].cantidad").value(5));

        // 4. Intentar agregar más de lo disponible en stock (stock=10, already 5, adding 6 -> 11) -> 400
        CarritoItemRequest exceedStockReq = CarritoItemRequest.builder()
                .productoId(productoA.getId())
                .cantidad(6)
                .build();

        mockMvc.perform(post("/api/carrito/items")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exceedStockReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Stock insuficiente"));

        // 5. Editar cantidad del ítem (a 4)
        CarritoItemUpdateRequest updateReq = CarritoItemUpdateRequest.builder()
                .cantidad(4)
                .build();

        mockMvc.perform(put("/api/carrito/items/" + itemId)
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(480.00))
                .andExpect(jsonPath("$.items[0].cantidad").value(4));

        // 6. Eliminar ítem
        mockMvc.perform(delete("/api/carrito/items/" + itemId)
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }
}
