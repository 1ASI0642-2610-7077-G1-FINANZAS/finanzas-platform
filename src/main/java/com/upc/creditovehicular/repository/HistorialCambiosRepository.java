package com.upc.creditovehicular.repository;
import com.upc.creditovehicular.entity.HistorialCambios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialCambiosRepository extends JpaRepository<HistorialCambios, Long> {}
