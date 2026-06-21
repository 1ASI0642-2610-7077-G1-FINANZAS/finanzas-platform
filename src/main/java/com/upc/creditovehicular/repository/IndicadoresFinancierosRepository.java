package com.upc.creditovehicular.repository;
import com.upc.creditovehicular.entity.IndicadoresFinancieros;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IndicadoresFinancierosRepository extends JpaRepository<IndicadoresFinancieros, Long> {
    Optional<IndicadoresFinancieros> findByCredito_IdCredito(Long idCredito);
}
