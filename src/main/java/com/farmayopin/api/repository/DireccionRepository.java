package com.farmayopin.api.repository;

import com.farmayopin.api.model.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    List<Direccion> findByUsuarioIdOrderByEsPrincipalDescIdAsc(Long usuarioId);

    Optional<Direccion> findByIdAndUsuarioId(Long id, Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    @Modifying
    @Query("UPDATE Direccion d SET d.esPrincipal = false WHERE d.usuario.id = :usuarioId")
    void resetPrincipalPorUsuario(@Param("usuarioId") Long usuarioId);
}
