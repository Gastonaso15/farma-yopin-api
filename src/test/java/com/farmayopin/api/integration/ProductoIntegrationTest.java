package com.farmayopin.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmayopin.api.dto.producto.ProductoRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductoIntegrationTest {

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

    private String adminToken;
    private String clienteToken;

    @BeforeEach
    void setUp() {
        itemCompraRepository.deleteAll();
        compraRepository.deleteAll();
        itemCarritoRepository.deleteAll();
        carritoRepository.deleteAll();
        usuarioRepository.deleteAll();
        productoRepository.deleteAll();

        Usuario admin = Usuario.builder()
                .nombre("Admin Farmacia")
                .email("admin@farmayopin.com")
                .password(passwordEncoder.encode("admin123"))
                .rol(Rol.ADMIN)
                .build();
        admin = usuarioRepository.save(admin);
        adminToken = jwtUtil.generateToken(admin.getId(), admin.getEmail(), admin.getRol(), admin.getNombre());

        Usuario cliente = Usuario.builder()
                .nombre("Cliente Farmacia")
                .email("cliente@farmayopin.com")
                .password(passwordEncoder.encode("cliente123"))
                .rol(Rol.CLIENTE)
                .build();
        cliente = usuarioRepository.save(cliente);
        clienteToken = jwtUtil.generateToken(cliente.getId(), cliente.getEmail(), cliente.getRol(), cliente.getNombre());
    }

    @Test
    void productoCRUD_IntegrationWithDB() throws Exception {
        // 1. Admin creates a product
        ProductoRequest nuevoProd = ProductoRequest.builder()
                .nombre("Omeprazol 20mg")
                .precio(new BigDecimal("210.00"))
                .detalle("Protector gástrico")
                .foto("http://img.com/omeprazol.jpg")
                .stock(35)
                .build();

        String res = mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevoProd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nombre").value("Omeprazol 20mg"))
                .andExpect(jsonPath("$.stock").value(35))
                .andReturn().getResponse().getContentAsString();

        Long prodId = objectMapper.readTree(res).get("id").asLong();

        // 2. Client cannot create product (403 Forbidden)
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevoProd)))
                .andExpect(status().isForbidden());

        // 3. Client can list products (200 OK)
        mockMvc.perform(get("/api/productos")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Omeprazol 20mg"));

        // 4. Admin can view product detail
        mockMvc.perform(get("/api/productos/" + prodId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prodId))
                .andExpect(jsonPath("$.nombre").value("Omeprazol 20mg"));

        // 5. Admin updates product
        ProductoRequest updateProd = ProductoRequest.builder()
                .nombre("Omeprazol 40mg")
                .precio(new BigDecimal("350.00"))
                .detalle("Protector gástrico doble acción")
                .foto("http://img.com/omeprazol40.jpg")
                .stock(50)
                .build();

        mockMvc.perform(put("/api/productos/" + prodId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Omeprazol 40mg"))
                .andExpect(jsonPath("$.stock").value(50));

        // Verify DB update
        Producto updatedInDb = productoRepository.findById(prodId).orElseThrow();
        assertEquals("Omeprazol 40mg", updatedInDb.getNombre());
        assertEquals(50, updatedInDb.getStock());
    }
}
