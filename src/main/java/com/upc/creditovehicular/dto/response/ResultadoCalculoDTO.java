package com.upc.creditovehicular.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResultadoCalculoDTO {
    private BigDecimal tea;
    private BigDecimal tem;
    private BigDecimal cuotaOrdinaria;
    private BigDecimal cuotaBalloon;
    private BigDecimal totalIntereses;
    private BigDecimal totalPagado;
    private BigDecimal van;
    private BigDecimal tir;
    private BigDecimal tcea;
    private List<CronogramaFilaDTO2> cronograma;
}
