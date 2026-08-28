package com.farmayopin.api.dto.carrito;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarritoResponse {
    private Long id;
    private List<CarritoItemResponse> items;
    private BigDecimal total;
}
