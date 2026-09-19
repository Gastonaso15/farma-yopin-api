package com.farmayopin.api.repository;

import com.farmayopin.api.model.Tarjeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarjetaRepository extends JpaRepository<Tarjeta, Long> {

    List<Tarjeta> findByUsuarioIdOrderByEsPrincipalDescIdAsc(Long usuarioId);

    Optional<Tarjeta> findByIdAndUsuarioId(Long id, Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    @Modifying
    @Query("UPDATE Tarjeta t SET t.esPrincipal = false WHERE t.usuario.id = :usuarioId")
    void resetPrincipalPorUsuario(@Param("usuarioId") Long usuarioId);
}
