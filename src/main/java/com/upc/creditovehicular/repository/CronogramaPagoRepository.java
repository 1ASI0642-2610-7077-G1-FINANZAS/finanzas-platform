package com.upc.creditovehicular.repository;
import com.upc.creditovehicular.entity.CronogramaPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CronogramaPagoRepository extends JpaRepository<CronogramaPago, Long> {
    List<CronogramaPago> findByCredito_IdCreditoOrderByNumeroCuotaAsc(Long idCredito);
}
