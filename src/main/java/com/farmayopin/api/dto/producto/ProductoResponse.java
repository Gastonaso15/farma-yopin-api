package com.farmayopin.api.dto.producto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoResponse {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private String detalle;
    private String foto;
    private Integer stock;
}
