package com.upc.creditovehicular.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "CONFIGURACION_CREDITO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConfiguracionCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    private Long idConfiguracion;

    @Enumerated(EnumType.STRING)
    @Column(name = "moneda", nullable = false, length = 5)
    private Moneda moneda;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_tasa", nullable = false, length = 15)
    private TipoTasa tipoTasa;

    // TEA: 8.49% - 20.26% en soles | 8.49% - 20.22% en dólares (BCP)
    @Column(name = "tasa_interes", nullable = false, precision = 10, scale = 8)
    private BigDecimal tasaInteres;

    // Solo requerido si tipo_tasa = NOMINAL
    @Enumerated(EnumType.STRING)
    @Column(name = "frecuencia_capitalizacion", length = 15)
    private FrecuenciaCapitalizacion frecuenciaCapitalizacion;

    // 24 o 36 meses para Compra Inteligente; hasta 72 para convencional
    @Column(name = "plazo_meses", nullable = false)
    private Integer plazoMeses;

    // Fijo = 30 (año ordinario 360 días, convención peruana)
    @Column(name = "dias_por_mes")
    private Integer diasPorMes;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_gracia", length = 15)
    private TipoGracia tipoGracia;

    @Column(name = "periodo_gracia")
    private Integer periodoGracia;

    // 0.077% mensual sobre saldo (obligatorio SBS)
    @Column(name = "seguro_desgravamen", precision = 6, scale = 4)
    private BigDecimal seguroDesgravamen;

    // % mensual sobre precio vehículo (obligatorio BCP)
    @Column(name = "seguro_vehicular", precision = 6, scale = 4)
    private BigDecimal seguroVehicular;

    @Column(name = "portes", precision = 8, scale = 2)
    private BigDecimal portes;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "configuracion")
    private List<Credito> creditos;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.diasPorMes == null) this.diasPorMes = 30;
        if (this.tipoGracia == null) this.tipoGracia = TipoGracia.SIN_GRACIA;
        if (this.periodoGracia == null) this.periodoGracia = 0;
        if (this.seguroDesgravamen == null) this.seguroDesgravamen = new BigDecimal("0.0770");
        if (this.seguroVehicular == null) this.seguroVehicular = BigDecimal.ZERO;
        if (this.portes == null) this.portes = BigDecimal.ZERO;
    }

    public enum Moneda { PEN, USD }
    public enum TipoTasa { NOMINAL, EFECTIVA }
    public enum FrecuenciaCapitalizacion { DIARIA, MENSUAL, BIMESTRAL, TRIMESTRAL, SEMESTRAL, ANUAL }
    public enum TipoGracia { SIN_GRACIA, PARCIAL, TOTAL }
}
