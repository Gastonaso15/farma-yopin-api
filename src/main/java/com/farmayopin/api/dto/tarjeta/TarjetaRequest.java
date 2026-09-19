package com.farmayopin.api.dto.tarjeta;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarjetaRequest {

    private String tipo;

    @NotBlank(message = "La marca de la tarjeta es obligatoria")
    private String marca;

    private String numeroTarjeta;

    private String ultimosCuatro;

    @NotBlank(message = "El nombre del titular es obligatorio")
    private String titular;

    @NotBlank(message = "La fecha de vencimiento es obligatoria")
    private String vencimiento;

    private Boolean esPrincipal;
}
