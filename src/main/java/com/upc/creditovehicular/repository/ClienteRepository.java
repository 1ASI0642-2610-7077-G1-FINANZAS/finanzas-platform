package com.upc.creditovehicular.repository;
import com.upc.creditovehicular.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByNumeroDocumento(String numeroDocumento);
    List<Cliente> findByUsuario_IdUsuario(Long idUsuario);
}
