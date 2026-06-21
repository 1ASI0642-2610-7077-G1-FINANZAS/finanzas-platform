package com.upc.creditovehicular.service;

import com.upc.creditovehicular.dto.response.CronogramaFilaDTO;
import com.upc.creditovehicular.dto.response.ResultadoCalculoDTO;
import com.upc.creditovehicular.entity.ConfiguracionCredito;
import com.upc.creditovehicular.entity.CronogramaPago;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor Financiero - Sistema Francés Vencido Ordinario con Valor Residual
 * Método Compra Inteligente - BCP
 *
 * Convención: año ordinario 360 días, mes de 30 días (sistema financiero peruano)
 * Precisión intermedia: 8 decimales | Resultados finales: 2 decimales (montos), 8 decimales (tasas)
 */
@Service
public class MotorFinancieroService {

    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
    private static final int ESCALA_TASA = 8;
    private static final int ESCALA_MONTO = 2;

    // =========================================================================
    // MÉTODO PRINCIPAL
    // =========================================================================

    public ResultadoCalculoDTO calcular(
            BigDecimal precioVehiculo,
            BigDecimal cuotaInicial,
            BigDecimal valorResidual,
            BigDecimal tasaInteres,
            ConfiguracionCredito.TipoTasa tipoTasa,
            ConfiguracionCredito.FrecuenciaCapitalizacion frecuencia,
            int plazoMeses,
            ConfiguracionCredito.TipoGracia tipoGracia,
            int periodoGracia,
            BigDecimal seguroDesgravamen,
            BigDecimal seguroVehicular,
            BigDecimal portes,
            LocalDate fechaInicio
    ) {
        // 1. Monto del préstamo y capital amortizable
        BigDecimal montoPrestamo = precioVehiculo.subtract(cuotaInicial);
        BigDecimal capitalAmortizable = montoPrestamo.subtract(valorResidual);

        // 2. Convertir tasa a TEA y TEM
        BigDecimal tea = convertirATEA(tasaInteres, tipoTasa, frecuencia);
        BigDecimal tem = convertirTEAaTEM(tea);

        // 3. Calcular cuota ordinaria inicial sobre capital amortizable
        BigDecimal cuotaOrdinaria = calcularCuotaFrancesa(capitalAmortizable, tem, plazoMeses);

        // 4. Iterar cronograma
        List<CronogramaFilaDTO> filas = new ArrayList<>();
        BigDecimal saldo = montoPrestamo;
        BigDecimal totalIntereses = BigDecimal.ZERO;
        BigDecimal totalPagado = BigDecimal.ZERO;
        List<BigDecimal> flujos = new ArrayList<>();
        flujos.add(montoPrestamo.negate()); // Flujo 0: desembolso

        int cuotaNum = 1;

        // --- Período de gracia ---
        for (int k = 0; k < periodoGracia; k++) {
            BigDecimal interes = saldo.multiply(tem, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal amortizacion = BigDecimal.ZERO;
            BigDecimal cuotaPeriodo;
            CronogramaPago.TipoPeriodo tipo;

            if (tipoGracia == ConfiguracionCredito.TipoGracia.TOTAL) {
                saldo = saldo.add(interes); // capitalización
                cuotaPeriodo = BigDecimal.ZERO;
                interes = BigDecimal.ZERO; // no se paga, se capitaliza
                tipo = CronogramaPago.TipoPeriodo.GRACIA_TOTAL;
            } else {
                cuotaPeriodo = interes;
                tipo = CronogramaPago.TipoPeriodo.GRACIA_PARCIAL;
            }

            BigDecimal segDesgrav = saldo.multiply(seguroDesgravamen.divide(BigDecimal.valueOf(100), MC))
                    .setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal segVeh = precioVehiculo.multiply(seguroVehicular.divide(BigDecimal.valueOf(100), MC))
                    .setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal cuotaTotal = cuotaPeriodo.add(segDesgrav).add(segVeh).add(portes)
                    .setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            BigDecimal saldoFinal = saldo.subtract(amortizacion).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            filas.add(CronogramaFilaDTO.builder()
                    .numeroCuota(cuotaNum++)
                    .fechaPago(fechaInicio.plusMonths(k + 1))
                    .saldoInicial(saldo.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                    .interes(interes.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                    .amortizacion(amortizacion)
                    .seguroDesgravamen(segDesgrav)
                    .seguroVehicular(segVeh)
                    .portes(portes)
                    .cuotaTotal(cuotaTotal)
                    .saldoFinal(saldoFinal)
                    .tipoPeriodo(tipo)
                    .build());

            totalIntereses = totalIntereses.add(interes);
            totalPagado = totalPagado.add(cuotaTotal);
            flujos.add(cuotaTotal.negate());
            saldo = saldoFinal;
        }

        // Recalcular cuota si hubo período de gracia
        if (periodoGracia > 0) {
            int cuotasRestantes = plazoMeses - periodoGracia;
            BigDecimal capitalRestante = saldo.subtract(valorResidual);
            cuotaOrdinaria = calcularCuotaFrancesa(capitalRestante, tem, cuotasRestantes);
        }

        // --- Cuotas ordinarias ---
        int cuotasOrdinarias = plazoMeses - periodoGracia;
        for (int k = 0; k < cuotasOrdinarias; k++) {
            BigDecimal interes = saldo.multiply(tem, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal amortizacion = cuotaOrdinaria.subtract(interes).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal saldoFinal = saldo.subtract(amortizacion).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            BigDecimal segDesgrav = saldo.multiply(seguroDesgravamen.divide(BigDecimal.valueOf(100), MC))
                    .setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal segVeh = precioVehiculo.multiply(seguroVehicular.divide(BigDecimal.valueOf(100), MC))
                    .setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal cuotaTotal = cuotaOrdinaria.add(segDesgrav).add(segVeh).add(portes)
                    .setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            filas.add(CronogramaFilaDTO.builder()
                    .numeroCuota(cuotaNum++)
                    .fechaPago(fechaInicio.plusMonths(periodoGracia + k + 1))
                    .saldoInicial(saldo.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                    .interes(interes)
                    .amortizacion(amortizacion)
                    .seguroDesgravamen(segDesgrav)
                    .seguroVehicular(segVeh)
                    .portes(portes)
                    .cuotaTotal(cuotaTotal)
                    .saldoFinal(saldoFinal)
                    .tipoPeriodo(CronogramaPago.TipoPeriodo.ORDINARIO)
                    .build());

            totalIntereses = totalIntereses.add(interes);
            totalPagado = totalPagado.add(cuotaTotal);
            flujos.add(cuotaTotal.negate());
            saldo = saldoFinal;
        }

        // --- Cuota Balloon (período n+1) ---
        BigDecimal interesBalloon = saldo.multiply(tem, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
        BigDecimal cuotaBalloon = saldo.add(interesBalloon).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
        BigDecimal segDesgravBalloon = saldo.multiply(seguroDesgravamen.divide(BigDecimal.valueOf(100), MC))
                .setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
        BigDecimal segVehBalloon = precioVehiculo.multiply(seguroVehicular.divide(BigDecimal.valueOf(100), MC))
                .setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
        BigDecimal cuotaTotalBalloon = cuotaBalloon.add(segDesgravBalloon).add(segVehBalloon).add(portes)
                .setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

        filas.add(CronogramaFilaDTO.builder()
                .numeroCuota(cuotaNum)
                .fechaPago(fechaInicio.plusMonths(plazoMeses + 1))
                .saldoInicial(saldo.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                .interes(interesBalloon)
                .amortizacion(saldo.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                .seguroDesgravamen(segDesgravBalloon)
                .seguroVehicular(segVehBalloon)
                .portes(portes)
                .cuotaTotal(cuotaTotalBalloon)
                .saldoFinal(BigDecimal.ZERO)
                .tipoPeriodo(CronogramaPago.TipoPeriodo.BALLOON)
                .build());

        totalIntereses = totalIntereses.add(interesBalloon);
        totalPagado = totalPagado.add(cuotaTotalBalloon);
        flujos.add(cuotaTotalBalloon.negate());

        // 5. Indicadores financieros
        BigDecimal van = calcularVAN(flujos, tem);
        BigDecimal tirMensual = calcularTIR(flujos);
        BigDecimal tirAnual = anualizar(tirMensual);
        BigDecimal tcea = tirAnual; // TCEA = TIR del flujo total con todos los costos

        return ResultadoCalculoDTO.builder()
                .tea(tea.setScale(ESCALA_TASA, RoundingMode.HALF_UP))
                .tem(tem.setScale(ESCALA_TASA, RoundingMode.HALF_UP))
                .cuotaOrdinaria(cuotaOrdinaria.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                .cuotaBalloon(cuotaBalloon.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                .totalIntereses(totalIntereses.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                .totalPagado(totalPagado.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                .van(van.setScale(4, RoundingMode.HALF_UP))
                .tir(tirAnual.setScale(ESCALA_TASA, RoundingMode.HALF_UP))
                .tcea(tcea.setScale(ESCALA_TASA, RoundingMode.HALF_UP))
                .cronograma(filas)
                .build();
    }

    // =========================================================================
    // CONVERSIÓN DE TASAS
    // =========================================================================

    /**
     * Convierte TNA a TEA según frecuencia de capitalización
     * TEA = (1 + TNA/m)^m - 1
     * Si ya es efectiva, retorna directamente
     */
    public BigDecimal convertirATEA(BigDecimal tasa, ConfiguracionCredito.TipoTasa tipo,
                                     ConfiguracionCredito.FrecuenciaCapitalizacion frecuencia) {
        if (tipo == ConfiguracionCredito.TipoTasa.EFECTIVA) {
            return tasa.divide(BigDecimal.valueOf(100), MC);
        }
        int m = getFrecuenciaAnual(frecuencia);
        BigDecimal tasaDecimal = tasa.divide(BigDecimal.valueOf(100), MC);
        BigDecimal base = BigDecimal.ONE.add(tasaDecimal.divide(BigDecimal.valueOf(m), MC));
        return base.pow(m, MC).subtract(BigDecimal.ONE);
    }

    /**
     * TEA a TEM: TEM = (1 + TEA)^(1/12) - 1
     * Convención año ordinario 360 días, mes 30 días
     */
    public BigDecimal convertirTEAaTEM(BigDecimal tea) {
        double teaDouble = tea.doubleValue();
        double temDouble = Math.pow(1 + teaDouble, 1.0 / 12.0) - 1;
        return BigDecimal.valueOf(temDouble).setScale(ESCALA_TASA, RoundingMode.HALF_UP);
    }

    // =========================================================================
    // CUOTA FRANCESA
    // =========================================================================

    /**
     * C = (P × TEM × (1+TEM)^n) / ((1+TEM)^n - 1)
     * P = capital amortizable (monto_prestamo - valor_residual)
     */
    public BigDecimal calcularCuotaFrancesa(BigDecimal capital, BigDecimal tem, int n) {
        double temD = tem.doubleValue();
        double capD = capital.doubleValue();
        double factor = Math.pow(1 + temD, n);
        double cuota = (capD * temD * factor) / (factor - 1);
        return BigDecimal.valueOf(cuota).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
    }

    // =========================================================================
    // INDICADORES FINANCIEROS
    // =========================================================================

    /**
     * VAN = -montoPrestamo + Σ [CuotaTotal_k / (1 + TEM)^k]
     */
    public BigDecimal calcularVAN(List<BigDecimal> flujos, BigDecimal tem) {
        double temD = tem.doubleValue();
        double van = 0;
        for (int k = 0; k < flujos.size(); k++) {
            van += flujos.get(k).doubleValue() / Math.pow(1 + temD, k);
        }
        return BigDecimal.valueOf(van);
    }

    /**
     * TIR mensual por Newton-Raphson
     * Monto_Préstamo = Σ [CuotaTotal_k / (1 + r)^k]
     */
    public BigDecimal calcularTIR(List<BigDecimal> flujos) {
        double r = 0.01; // estimación inicial 1% mensual
        int maxIter = 1000;
        double tolerancia = 1e-10;

        for (int iter = 0; iter < maxIter; iter++) {
            double f = 0, df = 0;
            for (int k = 0; k < flujos.size(); k++) {
                double fk = flujos.get(k).doubleValue();
                f  += fk / Math.pow(1 + r, k);
                df -= k * fk / Math.pow(1 + r, k + 1);
            }
            if (Math.abs(df) < tolerancia) break;
            double rNuevo = r - f / df;
            if (Math.abs(rNuevo - r) < tolerancia) {
                r = rNuevo;
                break;
            }
            r = rNuevo;
        }
        return BigDecimal.valueOf(r);
    }

    /**
     * TIR anual = (1 + TIR_mensual)^12 - 1
     */
    public BigDecimal anualizar(BigDecimal tirMensual) {
        double tir = Math.pow(1 + tirMensual.doubleValue(), 12) - 1;
        return BigDecimal.valueOf(tir);
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    private int getFrecuenciaAnual(ConfiguracionCredito.FrecuenciaCapitalizacion f) {
        if (f == null) return 12;
        return switch (f) {
            case DIARIA -> 360;
            case MENSUAL -> 12;
            case BIMESTRAL -> 6;
            case TRIMESTRAL -> 4;
            case SEMESTRAL -> 2;
            case ANUAL -> 1;
        };
    }
}
