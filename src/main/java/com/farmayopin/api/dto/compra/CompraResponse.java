package com.farmayopin.api.dto.compra;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraResponse {
    private Long id;
    private LocalDateTime fecha;
    private BigDecimal total;
    private String clienteNombre;
    private String clienteEmail;
    private List<CompraItemResponse> items;
}
