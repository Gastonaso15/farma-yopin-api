package com.farmayopin.api.service;

import com.farmayopin.api.dto.compra.CompraItemResponse;
import com.farmayopin.api.dto.compra.CompraResponse;
import com.farmayopin.api.exception.ResourceNotFoundException;
import com.farmayopin.api.model.Compra;
import com.farmayopin.api.repository.CompraRepository;
import com.farmayopin.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompraService {

    private final CompraRepository compraRepository;
    private final UsuarioRepository usuarioRepository;

    public CompraService(CompraRepository compraRepository, UsuarioRepository usuarioRepository) {
        this.compraRepository = compraRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> obtenerHistorialComprasUsuario(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId);
        }

        List<Compra> compras = compraRepository.findByUsuarioIdOrderByFechaDesc(usuarioId);

        return compras.stream()
                .map(this::mapToCompraResponse)
                .collect(Collectors.toList());
    }

    private CompraResponse mapToCompraResponse(Compra compra) {
        List<CompraItemResponse> items = compra.getItems().stream()
                .map(i -> CompraItemResponse.builder()
                        .id(i.getId())
                        .productoId(i.getProducto() != null ? i.getProducto().getId() : null)
                        .nombreProducto(i.getNombreProducto())
                        .cantidad(i.getCantidad())
                        .precioUnitario(i.getPrecioUnitario())
                        .subtotal(i.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return CompraResponse.builder()
                .id(compra.getId())
                .fecha(compra.getFecha())
                .total(compra.getTotal())
                .clienteNombre(compra.getUsuario() != null ? compra.getUsuario().getNombre() : null)
                .clienteEmail(compra.getUsuario() != null ? compra.getUsuario().getEmail() : null)
                .items(items)
                .build();
    }
}
