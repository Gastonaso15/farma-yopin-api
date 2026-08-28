package com.farmayopin.api.dto.producto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoCompraHistorialResponse {
    private Long compraId;
    private LocalDateTime fecha;
    private Integer cantidad;
    private String cliente;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
