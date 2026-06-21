package com.upc.creditovehicular.repository;
import com.upc.creditovehicular.entity.Credito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CreditoRepository extends JpaRepository<Credito, Long> {
    List<Credito> findByCliente_IdCliente(Long idCliente);
}
