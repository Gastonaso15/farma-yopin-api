package com.farmayopin.api.dto.direccion;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DireccionRequest {

    @NotBlank(message = "El alias de la dirección es obligatorio")
    private String alias;

    @NotBlank(message = "La calle es obligatoria")
    private String calle;

    @NotBlank(message = "El número es obligatorio")
    private String numero;

    private String pisoDepto;

    private String ciudad;

    private String departamento;

    private String codigoPostal;

    private String notas;

    private Boolean esPrincipal;
}
