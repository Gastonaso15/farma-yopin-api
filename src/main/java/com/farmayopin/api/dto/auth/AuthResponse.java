package com.farmayopin.api.dto.auth;

import com.farmayopin.api.model.Rol;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private Long id;
    private String nombre;
    private String email;
    private Rol rol;
    private String token;
}
