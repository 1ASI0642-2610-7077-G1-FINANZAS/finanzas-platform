package com.upc.creditovehicular.dto.response;

import com.upc.creditovehicular.entity.CronogramaPago;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CronogramaFilaDTO2 {
    private int numeroCuota;
    private String tipoPeriodo; // 'T' o lo que requiera tu vista
    private LocalDate fechaPago;

    // --- SECCIÓN BALLOON (CUOTA FINAL) ---
    private BigDecimal balloonSaldoInicial;
    private BigDecimal balloonInteres;
    private BigDecimal balloonAmortizacion;
    private BigDecimal balloonSeguro;
    private BigDecimal balloonSaldoFinal;

    // --- SECCIÓN REGULAR ---
    private BigDecimal regularSaldoInicial;
    private BigDecimal regularInteres;
    private BigDecimal regularCuotaTotal; // "Cuota (inc Seg Des)"
    private BigDecimal regularAmortizacion;
    private BigDecimal regularSeguro;
    private BigDecimal regularRiesgo;     // Nuevo
    private BigDecimal regularGps;        // Nuevo
    private BigDecimal regularPortes;
    private BigDecimal regularGastos;     // Nuevo
    private BigDecimal regularSaldoFinal;

    private BigDecimal flujo;
}
