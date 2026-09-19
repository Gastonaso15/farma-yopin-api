package com.farmayopin.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "direcciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String alias;

    @Column(nullable = false, length = 200)
    private String calle;

    @Column(nullable = false, length = 50)
    private String numero;

    @Column(name = "piso_depto", length = 50)
    private String pisoDepto;

    @Column(nullable = false, length = 100)
    private String ciudad;

    @Column(nullable = false, length = 100)
    private String departamento;

    @Column(name = "codigo_postal", nullable = false, length = 20)
    private String codigoPostal;

    @Column(columnDefinition = "TEXT")
    private String notas;

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
        if (departamento == null || departamento.trim().isEmpty()) {
            departamento = "Maldonado";
        }
        if (ciudad == null || ciudad.trim().isEmpty()) {
            ciudad = "Maldonado";
        }
        if (codigoPostal == null || codigoPostal.trim().isEmpty()) {
            codigoPostal = "20000";
        }
    }
}
