package com.farmayopin.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmayopin.api.dto.auth.AuthResponse;
import com.farmayopin.api.dto.auth.LoginRequest;
import com.farmayopin.api.dto.auth.LogoutResponse;
import com.farmayopin.api.dto.auth.RegisterRequest;
import com.farmayopin.api.exception.BadRequestException;
import com.farmayopin.api.exception.GlobalExceptionHandler;
import com.farmayopin.api.model.Rol;
import com.farmayopin.api.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void register_Success_Returns201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .nombre("Juan Perez")
                .email("juan@test.com")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .id(1L)
                .nombre("Juan Perez")
                .email("juan@test.com")
                .rol(Rol.CLIENTE)
                .token("jwt_token_example")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("juan@test.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.token").value("jwt_token_example"));
    }

    @Test
    void register_InvalidEmail_Returns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .nombre("Juan")
                .email("invalid-email")
                .password("1234")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_EmailAlreadyExists_Returns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .nombre("Juan Perez")
                .email("duplicate@test.com")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BadRequestException("El email ya se encuentra registrado"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El email ya se encuentra registrado"));
    }

    @Test
    void login_Success_Returns200() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("juan@test.com")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .id(1L)
                .nombre("Juan Perez")
                .email("juan@test.com")
                .rol(Rol.CLIENTE)
                .token("jwt_token_example")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_token_example"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void logout_Success_Returns200() throws Exception {
        LogoutResponse response = LogoutResponse.builder()
                .mensaje("Sesión cerrada correctamente")
                .build();

        when(authService.logout(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer mock_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sesión cerrada correctamente"));
    }
}
