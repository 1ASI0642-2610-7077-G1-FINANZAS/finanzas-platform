package com.upc.creditovehicular.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "CRONOGRAMA_PAGO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CronogramaPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cronograma")
    private Long idCronograma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_credito", nullable = false)
    private Credito credito;

    @Column(name = "numero_cuota", nullable = false)
    private Integer numeroCuota;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "saldo_inicial", precision = 12, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name = "interes", precision = 12, scale = 2)
    private BigDecimal interes;

    @Column(name = "amortizacion", precision = 12, scale = 2)
    private BigDecimal amortizacion;

    @Column(name = "seguro_desgravamen", precision = 12, scale = 2)
    private BigDecimal seguroDesgravamen;

    @Column(name = "seguro_vehicular", precision = 12, scale = 2)
    private BigDecimal seguroVehicular;

    @Column(name = "portes", precision = 8, scale = 2)
    private BigDecimal portes;

    @Column(name = "cuota_total", precision = 12, scale = 2)
    private BigDecimal cuotaTotal;

    @Column(name = "saldo_final", precision = 12, scale = 2)
    private BigDecimal saldoFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_periodo", nullable = false, length = 20)
    private TipoPeriodo tipoPeriodo;

    public enum TipoPeriodo { ORDINARIO, GRACIA_PARCIAL, GRACIA_TOTAL, BALLOON }
}
