package com.farmayopin.api.controller;

import com.farmayopin.api.dto.tarjeta.TarjetaRequest;
import com.farmayopin.api.dto.tarjeta.TarjetaResponse;
import com.farmayopin.api.exception.UnauthorizedException;
import com.farmayopin.api.security.CustomUserDetails;
import com.farmayopin.api.service.TarjetaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tarjetas")
public class TarjetaController {

    private final TarjetaService tarjetaService;

    public TarjetaController(TarjetaService tarjetaService) {
        this.tarjetaService = tarjetaService;
    }

    @GetMapping
    public ResponseEntity<List<TarjetaResponse>> listarTarjetas(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = obtenerUsuarioId(userDetails);
        return ResponseEntity.ok(tarjetaService.listarPorUsuario(usuarioId));
    }

    @PostMapping
    public ResponseEntity<TarjetaResponse> crearTarjeta(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TarjetaRequest request) {
        Long usuarioId = obtenerUsuarioId(userDetails);
        TarjetaResponse response = tarjetaService.crearTarjeta(usuarioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarTarjeta(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        Long usuarioId = obtenerUsuarioId(userDetails);
        tarjetaService.eliminarTarjeta(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/principal")
    public ResponseEntity<TarjetaResponse> marcarComoPrincipal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        Long usuarioId = obtenerUsuarioId(userDetails);
        return ResponseEntity.ok(tarjetaService.marcarComoPrincipal(usuarioId, id));
    }

    private Long obtenerUsuarioId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        return userDetails.getId();
    }
}
