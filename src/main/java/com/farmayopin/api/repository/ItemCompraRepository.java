package com.farmayopin.api.repository;

import com.farmayopin.api.model.ItemCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemCompraRepository extends JpaRepository<ItemCompra, Long> {
    List<ItemCompra> findByProductoIdOrderByCompraFechaDesc(Long productoId);
}
