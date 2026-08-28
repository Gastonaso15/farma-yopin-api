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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final CarritoRepository carritoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            CarritoRepository carritoRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authenticationManager,
            TokenBlacklistService tokenBlacklistService) {
        this.usuarioRepository = usuarioRepository;
        this.carritoRepository = carritoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya se encuentra registrado");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(Rol.CLIENTE)
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // Crear carrito vacío para el cliente
        Carrito carrito = Carrito.builder()
                .usuario(usuarioGuardado)
                .build();
        carritoRepository.save(carrito);

        String token = jwtUtil.generateToken(
                usuarioGuardado.getId(),
                usuarioGuardado.getEmail(),
                usuarioGuardado.getRol(),
                usuarioGuardado.getNombre()
        );

        return AuthResponse.builder()
                .id(usuarioGuardado.getId())
                .nombre(usuarioGuardado.getNombre())
                .email(usuarioGuardado.getEmail())
                .rol(usuarioGuardado.getRol())
                .token(token)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        String token = jwtUtil.generateToken(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getNombre()
        );

        return AuthResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .token(token)
                .build();
    }

    public LogoutResponse logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenBlacklistService.blacklistToken(token);
        }
        return LogoutResponse.builder()
                .mensaje("Sesión cerrada correctamente")
                .build();
    }
}
