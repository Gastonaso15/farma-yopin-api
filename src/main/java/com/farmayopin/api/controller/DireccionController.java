package com.farmayopin.api.controller;

import com.farmayopin.api.dto.direccion.DireccionRequest;
import com.farmayopin.api.dto.direccion.DireccionResponse;
import com.farmayopin.api.exception.UnauthorizedException;
import com.farmayopin.api.security.CustomUserDetails;
import com.farmayopin.api.service.DireccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/direcciones")
public class DireccionController {

    private final DireccionService direccionService;

    public DireccionController(DireccionService direccionService) {
        this.direccionService = direccionService;
    }

    @GetMapping
    public ResponseEntity<List<DireccionResponse>> listarDirecciones(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = obtenerUsuarioId(userDetails);
        return ResponseEntity.ok(direccionService.listarPorUsuario(usuarioId));
    }

    @PostMapping
    public ResponseEntity<DireccionResponse> crearDireccion(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DireccionRequest request) {
        Long usuarioId = obtenerUsuarioId(userDetails);
        DireccionResponse response = direccionService.crearDireccion(usuarioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DireccionResponse> actualizarDireccion(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody DireccionRequest request) {
        Long usuarioId = obtenerUsuarioId(userDetails);
        return ResponseEntity.ok(direccionService.actualizarDireccion(usuarioId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDireccion(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        Long usuarioId = obtenerUsuarioId(userDetails);
        direccionService.eliminarDireccion(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/principal")
    public ResponseEntity<DireccionResponse> marcarComoPrincipal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        Long usuarioId = obtenerUsuarioId(userDetails);
        return ResponseEntity.ok(direccionService.marcarComoPrincipal(usuarioId, id));
    }

    private Long obtenerUsuarioId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        return userDetails.getId();
    }
}
