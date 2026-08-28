package com.farmayopin.api.dto.carrito;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarritoItemResponse {
    private Long id;
    private Long productoId;
    private String nombreProducto;
    private String foto;
    private BigDecimal precioUnitario;
    private Integer cantidad;
    private BigDecimal subtotal;
}
