package com.farmayopin.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmayopin.api.dto.auth.LoginRequest;
import com.farmayopin.api.dto.auth.RegisterRequest;
import com.farmayopin.api.model.Usuario;
import com.farmayopin.api.repository.CarritoRepository;
import com.farmayopin.api.repository.CompraRepository;
import com.farmayopin.api.repository.ItemCompraRepository;
import com.farmayopin.api.repository.ItemCarritoRepository;
import com.farmayopin.api.repository.ProductoRepository;
import com.farmayopin.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private ItemCarritoRepository itemCarritoRepository;

    @Autowired
    private ItemCompraRepository itemCompraRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        itemCompraRepository.deleteAll();
        compraRepository.deleteAll();
        itemCarritoRepository.deleteAll();
        carritoRepository.deleteAll();
        usuarioRepository.deleteAll();
        productoRepository.deleteAll();
    }

    @Test
    void registerAndLogin_IntegrationWithDB() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .nombre("Maria Gomez")
                .email("maria@farmayopin.com")
                .password("securePass123")
                .build();

        // 1. Register
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("maria@farmayopin.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        // Verify DB persistence
        assertTrue(usuarioRepository.existsByEmail("maria@farmayopin.com"));
        Usuario savedUser = usuarioRepository.findByEmail("maria@farmayopin.com").orElseThrow();
        assertTrue(carritoRepository.findByUsuarioId(savedUser.getId()).isPresent());

        // 2. Duplicate registration should fail
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El email ya se encuentra registrado"));

        // 3. Login with correct credentials
        LoginRequest loginReq = LoginRequest.builder()
                .email("maria@farmayopin.com")
                .password("securePass123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andReturn();

        // 4. Login with invalid password
        LoginRequest wrongLoginReq = LoginRequest.builder()
                .email("maria@farmayopin.com")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLoginReq)))
                .andExpect(status().isUnauthorized());

        // 5. Logout
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sesión cerrada correctamente"));
    }
}
