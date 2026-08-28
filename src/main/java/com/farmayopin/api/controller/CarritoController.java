package com.farmayopin.api.controller;

import com.farmayopin.api.dto.carrito.CarritoItemRequest;
import com.farmayopin.api.dto.carrito.CarritoItemUpdateRequest;
import com.farmayopin.api.dto.carrito.CarritoResponse;
import com.farmayopin.api.dto.compra.CompraResponse;
import com.farmayopin.api.exception.UnauthorizedException;
import com.farmayopin.api.security.CustomUserDetails;
import com.farmayopin.api.service.CarritoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @GetMapping
    public ResponseEntity<CarritoResponse> verCarrito(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = getUserId(userDetails);
        CarritoResponse response = carritoService.obtenerCarrito(usuarioId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    public ResponseEntity<CarritoResponse> agregarAlCarrito(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CarritoItemRequest request) {
        Long usuarioId = getUserId(userDetails);
        CarritoResponse response = carritoService.agregarItem(usuarioId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<CarritoResponse> editarCantidadItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody CarritoItemUpdateRequest request) {
        Long usuarioId = getUserId(userDetails);
        CarritoResponse response = carritoService.actualizarCantidadItem(usuarioId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<CarritoResponse> eliminarItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        Long usuarioId = getUserId(userDetails);
        CarritoResponse response = carritoService.eliminarItem(usuarioId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pago")
    public ResponseEntity<CompraResponse> pagarCarrito(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = getUserId(userDetails);
        CompraResponse response = carritoService.pagarCarrito(usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Long getUserId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        return userDetails.getId();
    }
}
