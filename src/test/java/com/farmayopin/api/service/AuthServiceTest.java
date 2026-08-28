package com.farmayopin.api.service;

import com.farmayopin.api.dto.auth.AuthResponse;
import com.farmayopin.api.dto.auth.LoginRequest;
import com.farmayopin.api.dto.auth.LogoutResponse;
import com.farmayopin.api.dto.auth.RegisterRequest;
import com.farmayopin.api.exception.BadRequestException;
import com.farmayopin.api.model.Carrito;
import com.farmayopin.api.model.Rol;
import com.farmayopin.api.model.Usuario;
import com.farmayopin.api.repository.CarritoRepository;
import com.farmayopin.api.repository.UsuarioRepository;
import com.farmayopin.api.security.JwtUtil;
import com.farmayopin.api.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CarritoRepository carritoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .nombre("Juan Perez")
                .email("juan@test.com")
                .password("password123")
                .build();

        loginRequest = LoginRequest.builder()
                .email("juan@test.com")
                .password("password123")
                .build();

        usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan Perez")
                .email("juan@test.com")
                .password("encoded_pass")
                .rol(Rol.CLIENTE)
                .build();
    }

    @Test
    void register_Success() {
        when(usuarioRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded_pass");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(carritoRepository.save(any(Carrito.class))).thenReturn(new Carrito());
        when(jwtUtil.generateToken(usuario.getId(), usuario.getEmail(), usuario.getRol(), usuario.getNombre()))
                .thenReturn("mock_jwt_token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("juan@test.com", response.getEmail());
        assertEquals(Rol.CLIENTE, response.getRol());
        assertEquals("mock_jwt_token", response.getToken());
        verify(usuarioRepository).save(any(Usuario.class));
        verify(carritoRepository).save(any(Carrito.class));
    }

    @Test
    void register_EmailAlreadyExists_ThrowsBadRequestException() {
        when(usuarioRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(usuarioRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(usuario));
        when(jwtUtil.generateToken(usuario.getId(), usuario.getEmail(), usuario.getRol(), usuario.getNombre()))
                .thenReturn("mock_jwt_token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("juan@test.com", response.getEmail());
        assertEquals("mock_jwt_token", response.getToken());
    }

    @Test
    void login_InvalidCredentials_ThrowsBadCredentialsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void logout_Success() {
        String authHeader = "Bearer mock_token_123";

        LogoutResponse response = authService.logout(authHeader);

        assertNotNull(response);
        assertEquals("Sesión cerrada correctamente", response.getMensaje());
        verify(tokenBlacklistService).blacklistToken("mock_token_123");
    }
}
