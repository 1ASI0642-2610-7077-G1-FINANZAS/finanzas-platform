package com.upc.creditovehicular.repository;
import com.upc.creditovehicular.entity.ConfiguracionCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionCreditoRepository extends JpaRepository<ConfiguracionCredito, Long> {}
