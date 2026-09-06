package com.farmayopin.api.dto.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadImagenResponse {
    private String rutaRelativa;
    private String url;
    private String mensaje;
}
