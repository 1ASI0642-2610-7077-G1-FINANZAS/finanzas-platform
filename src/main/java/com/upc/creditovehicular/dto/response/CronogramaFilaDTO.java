package com.upc.creditovehicular.dto.response;

import com.upc.creditovehicular.entity.CronogramaPago;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CronogramaFilaDTO {
    private Integer numeroCuota;
    private LocalDate fechaPago;
    private BigDecimal saldoInicial;
    private BigDecimal interes;
    private BigDecimal amortizacion;
    private BigDecimal seguroDesgravamen;
    private BigDecimal seguroVehicular;
    private BigDecimal portes;
    private BigDecimal cuotaTotal;
    private BigDecimal saldoFinal;
    private CronogramaPago.TipoPeriodo tipoPeriodo;
}
