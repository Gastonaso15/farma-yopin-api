package com.farmayopin.api.dto.direccion;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DireccionResponse {
    private Long id;
    private Long usuarioId;
    private String alias;
    private String calle;
    private String numero;
    private String pisoDepto;
    private String ciudad;
    private String departamento;
    private String codigoPostal;
    private String notas;
    private Boolean esPrincipal;
    private LocalDateTime fechaCreacion;
}
