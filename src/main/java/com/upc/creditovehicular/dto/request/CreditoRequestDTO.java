package com.upc.creditovehicular.dto.request;

import com.upc.creditovehicular.entity.ConfiguracionCredito;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditoRequestDTO {

    @NotNull
    private Long idCliente;

    @NotNull
    private Long idVehiculo;

    // Configuración financiera
    @NotNull
    private ConfiguracionCredito.Moneda moneda;

    @NotNull
    private ConfiguracionCredito.TipoTasa tipoTasa;

    @NotNull @DecimalMin("0.0001")
    private BigDecimal tasaInteres; // En porcentaje (ej: 12.00 para 12%)

    @NotNull @DecimalMin("0.00")
    private BigDecimal tasaDescuento; // <-- NUEVO: COK (%) para el VAN

    private ConfiguracionCredito.FrecuenciaCapitalizacion frecuenciaCapitalizacion;

    @NotNull @Min(1) @Max(72)
    private Integer plazoMeses;

    @NotNull
    private ConfiguracionCredito.TipoGracia tipoGracia;

    @Min(0)
    private Integer periodoGracia = 0;

    // Montos del crédito
    @NotNull @DecimalMin("0.01")
    private BigDecimal precioVehiculo;

    @NotNull @DecimalMin("0.00")
    private BigDecimal cuotaInicial;

    @NotNull @DecimalMin("0.00")
    private BigDecimal valorResidual;

    // Seguros y portes
    private BigDecimal seguroDesgravamen = new BigDecimal("0.0770");
    private BigDecimal seguroVehicular   = BigDecimal.ZERO;
    private BigDecimal portes            = BigDecimal.ZERO;

    @NotNull
    private LocalDate fechaInicio;
}
