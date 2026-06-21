package com.upc.creditovehicular.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "CREDITO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Credito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_credito")
    private Long idCredito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vehiculo", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_configuracion", nullable = false)
    private ConfiguracionCredito configuracion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // precio_vehiculo - cuota_inicial
    @Column(name = "monto_prestamo", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPrestamo;

    // Mínimo 10% del precio (BCP)
    @Column(name = "cuota_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal cuotaInicial;

    // 35% - 50% del precio del vehículo (BCP Compra Inteligente)
    @Column(name = "valor_residual", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorResidual;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15)
    private Estado estado;

    @Column(name = "metodo", length = 50)
    private String metodo;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "credito", cascade = CascadeType.ALL)
    private List<CronogramaPago> cronograma;

    @OneToOne(mappedBy = "credito", cascade = CascadeType.ALL)
    private IndicadoresFinancieros indicadores;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estado == null) this.estado = Estado.SIMULADO;
        if (this.metodo == null) this.metodo = "Compra Inteligente";
    }

    public enum Estado { SIMULADO, ACTIVO, CERRADO }
}
