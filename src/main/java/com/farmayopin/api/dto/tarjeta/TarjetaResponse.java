package com.farmayopin.api.dto.tarjeta;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarjetaResponse {
    private Long id;
    private Long usuarioId;
    private String tipo;
    private String marca;
    private String ultimosCuatro;
    private String titular;
    private String vencimiento;
    private Boolean esPrincipal;
    private LocalDateTime fechaCreacion;
}
