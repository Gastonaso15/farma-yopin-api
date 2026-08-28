package com.farmayopin.api.controller;

import com.farmayopin.api.dto.compra.CompraResponse;
import com.farmayopin.api.exception.UnauthorizedException;
import com.farmayopin.api.security.CustomUserDetails;
import com.farmayopin.api.service.CompraService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @GetMapping
    public ResponseEntity<List<CompraResponse>> verHistoricoComprasPropio(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        List<CompraResponse> compras = compraService.obtenerHistorialComprasUsuario(userDetails.getId());
        return ResponseEntity.ok(compras);
    }
}
