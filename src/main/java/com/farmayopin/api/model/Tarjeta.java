package com.farmayopin.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tarjetas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTarjeta tipo;

    @Column(nullable = false, length = 50)
    private String marca;

    @Column(name = "ultimos_cuatro", nullable = false, length = 4)
    private String ultimosCuatro;

    @Column(nullable = false, length = 150)
    private String titular;

    @Column(nullable = false, length = 10)
    private String vencimiento;

    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (esPrincipal == null) {
            esPrincipal = false;
        }
        if (tipo == null) {
            tipo = TipoTarjeta.CREDITO;
        }
    }
}
