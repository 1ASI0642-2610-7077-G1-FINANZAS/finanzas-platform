package com.upc.creditovehicular.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "INDICADORES_FINANCIEROS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IndicadoresFinancieros {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_indicador")
    private Long idIndicador;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_credito", nullable = false, unique = true)
    private Credito credito;

    @Column(name = "van", precision = 14, scale = 4)
    private BigDecimal van;

    @Column(name = "tir", precision = 10, scale = 8)
    private BigDecimal tir;

    @Column(name = "tcea", precision = 10, scale = 8)
    private BigDecimal tcea;

    @Column(name = "cuota_ordinaria", precision = 12, scale = 2)
    private BigDecimal cuotaOrdinaria;

    @Column(name = "cuota_balloon", precision = 12, scale = 2)
    private BigDecimal cuotaBalloon;

    @Column(name = "total_intereses", precision = 14, scale = 2)
    private BigDecimal totalIntereses;

    @Column(name = "total_pagado", precision = 14, scale = 2)
    private BigDecimal totalPagado;

    @Column(name = "fecha_calculo")
    private LocalDateTime fechaCalculo;

    @PrePersist
    public void prePersist() {
        this.fechaCalculo = LocalDateTime.now();
    }
}
