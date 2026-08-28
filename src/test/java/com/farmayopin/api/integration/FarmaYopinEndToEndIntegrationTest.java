package com.farmayopin.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmayopin.api.dto.auth.LoginRequest;
import com.farmayopin.api.dto.auth.RegisterRequest;
import com.farmayopin.api.dto.carrito.CarritoItemRequest;
import com.farmayopin.api.dto.producto.ProductoRequest;
import com.farmayopin.api.model.Producto;
import com.farmayopin.api.model.Rol;
import com.farmayopin.api.model.Usuario;
import com.farmayopin.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class FarmaYopinEndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

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
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        itemCompraRepository.deleteAll();
        compraRepository.deleteAll();
        itemCarritoRepository.deleteAll();
        carritoRepository.deleteAll();
        usuarioRepository.deleteAll();
        productoRepository.deleteAll();
    }

    @Test
    void fullE2EFlow_Register_CreateProduct_AddToCart_Checkout_ViewHistory() throws Exception {
        // --- 1. Crear usuario Administrador directamente en BD y loguearse ---
        Usuario admin = Usuario.builder()
                .nombre("Admin Central")
                .email("admin@farmacia.com")
                .password(passwordEncoder.encode("adminPass123"))
                .rol(Rol.ADMIN)
                .build();
        usuarioRepository.save(admin);

        LoginRequest adminLogin = LoginRequest.builder()
                .email("admin@farmacia.com")
                .password("adminPass123")
                .build();

        MvcResult adminLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = objectMapper.readTree(adminLoginRes.getResponse().getContentAsString()).get("token").asText();

        // --- 2. Admin crea 2 productos en el catálogo ---
        ProductoRequest prod1 = ProductoRequest.builder()
                .nombre("Aspirina 500mg")
                .precio(new BigDecimal("80.00"))
                .detalle("Ácido acetilsalicílico")
                .foto("http://img.com/aspirina.jpg")
                .stock(20)
                .build();

        MvcResult prod1Res = mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prod1)))
                .andExpect(status().isCreated())
                .andReturn();

        Long prod1Id = objectMapper.readTree(prod1Res.getResponse().getContentAsString()).get("id").asLong();

        ProductoRequest prod2 = ProductoRequest.builder()
                .nombre("Alcohol en Gel 250ml")
                .precio(new BigDecimal("150.00"))
                .detalle("Sanitizante")
                .foto("http://img.com/alcohol.jpg")
                .stock(10)
                .build();

        MvcResult prod2Res = mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prod2)))
                .andExpect(status().isCreated())
                .andReturn();

        Long prod2Id = objectMapper.readTree(prod2Res.getResponse().getContentAsString()).get("id").asLong();

        // --- 3. Cliente se registra a través de la API ---
        RegisterRequest clientRegister = RegisterRequest.builder()
                .nombre("Lucia Ramos")
                .email("lucia@gmail.com")
                .password("luciaPass123")
                .build();

        MvcResult clientRegRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientRegister)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String clientToken = objectMapper.readTree(clientRegRes.getResponse().getContentAsString()).get("token").asText();

        // --- 4. Cliente consulta el catálogo de productos ---
        mockMvc.perform(get("/api/productos")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // --- 5. Cliente agrega ambos productos a su carrito ---
        // Prod 1: 3 unidades ($80 * 3 = $240)
        CarritoItemRequest addProd1 = CarritoItemRequest.builder()
                .productoId(prod1Id)
                .cantidad(3)
                .build();

        mockMvc.perform(post("/api/carrito/items")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addProd1)))
                .andExpect(status().isOk());

        // Prod 2: 2 unidades ($150 * 2 = $300)
        CarritoItemRequest addProd2 = CarritoItemRequest.builder()
                .productoId(prod2Id)
                .cantidad(2)
                .build();

        mockMvc.perform(post("/api/carrito/items")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addProd2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(540.00)) // 240 + 300 = 540
                .andExpect(jsonPath("$.items.length()").value(2));

        // --- 6. Cliente realiza el checkout / pago del carrito (UC-12) ---
        mockMvc.perform(post("/api/carrito/pago")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(540.00))
                .andExpect(jsonPath("$.clienteNombre").value("Lucia Ramos"))
                .andExpect(jsonPath("$.items.length()").value(2));

        // --- 7. Verificación de persistencia en MySQL y stock descontado ---
        Producto p1Db = productoRepository.findById(prod1Id).orElseThrow();
        assertEquals(17, p1Db.getStock()); // 20 - 3 = 17

        Producto p2Db = productoRepository.findById(prod2Id).orElseThrow();
        assertEquals(8, p2Db.getStock()); // 10 - 2 = 8

        // Carrito del cliente debe quedar vacío
        mockMvc.perform(get("/api/carrito")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());

        // --- 8. Cliente consulta su histórico de compras propio (UC-13) ---
        mockMvc.perform(get("/api/compras")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].total").value(540.00))
                .andExpect(jsonPath("$[0].items.length()").value(2));

        // --- 9. Admin consulta el histórico de compras de un producto específico (UC-08) ---
        mockMvc.perform(get("/api/productos/" + prod1Id + "/compras")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cliente").value("Lucia Ramos"))
                .andExpect(jsonPath("$[0].cantidad").value(3))
                .andExpect(jsonPath("$[0].subtotal").value(240.00));
    }
}
