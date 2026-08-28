package com.farmayopin.api.repository;

import com.farmayopin.api.model.Compra;
import com.farmayopin.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByUsuarioOrderByFechaDesc(Usuario usuario);
    List<Compra> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
}
