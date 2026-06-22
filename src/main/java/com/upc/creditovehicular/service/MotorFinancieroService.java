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
 * Método Compra Inteligente - Adaptado a modelo BCP/Interbank
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
            LocalDate fechaInicio,
            BigDecimal tasaDescuento // <-- NUEVO: COK (%) para el cálculo correcto del VAN
    ) {
        // 1. Monto del préstamo (Desembolso inicial)
        BigDecimal montoPrestamo = precioVehiculo.subtract(cuotaInicial);

        // 2. Convertir tasas principales
        BigDecimal tea = convertirATEA(tasaInteres, tipoTasa, frecuencia);
        BigDecimal tem = convertirTEAaTEM(tea);
        BigDecimal tasaDesgravamenDecimal = seguroDesgravamen.divide(BigDecimal.valueOf(100), MC);

        // 3. Preparar variables de iteración
        List<CronogramaFilaDTO> filas = new ArrayList<>();
        BigDecimal saldo = montoPrestamo;
        BigDecimal totalIntereses = BigDecimal.ZERO;
        BigDecimal totalPagado = BigDecimal.ZERO;

        // Array de Flujos para TIR/TCEA y VAN. Flujo 0 es POSITIVO.
        List<BigDecimal> flujos = new ArrayList<>();
        flujos.add(montoPrestamo);

        int cuotaNum = 1;

        // ==========================================
        // --- PERÍODO DE GRACIA ---
        // ==========================================
        for (int k = 0; k < periodoGracia; k++) {
            BigDecimal saldoInicialPeriodo = saldo;
            BigDecimal interes = saldoInicialPeriodo.multiply(tem, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal segDesgrav = saldoInicialPeriodo.multiply(tasaDesgravamenDecimal, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal segVeh = precioVehiculo.multiply(seguroVehicular.divide(BigDecimal.valueOf(100), MC)).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            BigDecimal cuotaPeriodo;
            BigDecimal amortizacion = BigDecimal.ZERO;
            CronogramaPago.TipoPeriodo tipo;

            if (tipoGracia == ConfiguracionCredito.TipoGracia.TOTAL) {
                saldo = saldoInicialPeriodo.add(interes); // Capitaliza
                cuotaPeriodo = BigDecimal.ZERO;
                interes = BigDecimal.ZERO; // Visualmente en la cuota no se paga interés
                tipo = CronogramaPago.TipoPeriodo.GRACIA_TOTAL;
            } else { // GRACIA PARCIAL
                cuotaPeriodo = interes;
                tipo = CronogramaPago.TipoPeriodo.GRACIA_PARCIAL;
            }

            BigDecimal cuotaTotal = cuotaPeriodo.add(segDesgrav).add(segVeh).add(portes).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal saldoFinal = saldo;

            filas.add(CronogramaFilaDTO.builder()
                    .numeroCuota(cuotaNum++)
                    .fechaPago(fechaInicio.plusMonths(k + 1))
                    .saldoInicial(saldoInicialPeriodo.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                    .interes(interes)
                    .amortizacion(amortizacion)
                    .seguroDesgravamen(segDesgrav)
                    .seguroVehicular(segVeh)
                    .portes(portes)
                    .cuotaTotal(cuotaTotal)
                    .saldoFinal(saldoFinal.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                    .tipoPeriodo(tipo)
                    .build());

            totalIntereses = totalIntereses.add(interes);
            totalPagado = totalPagado.add(cuotaTotal);
            flujos.add(cuotaTotal.negate()); // Flujo de salida es negativo
        }

        // ==========================================
        // --- CUOTAS ORDINARIAS (Con Valor Residual)
        // ==========================================
        int cuotasRestantes = plazoMeses - periodoGracia;

        // Calcular cuota constante que incluye el seguro de desgravamen y respeta el Valor Residual
        BigDecimal iTotal = tem.add(tasaDesgravamenDecimal, MC);
        BigDecimal cuotaOrdinaria = calcularCuotaCompraInteligente(saldo, valorResidual, iTotal, cuotasRestantes);

        for (int k = 0; k < cuotasRestantes; k++) {
            boolean esUltimoMes = (k == cuotasRestantes - 1);

            BigDecimal saldoInicialPeriodo = saldo;
            BigDecimal interes = saldoInicialPeriodo.multiply(tem, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal segDesgrav = saldoInicialPeriodo.multiply(tasaDesgravamenDecimal, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal segVeh = precioVehiculo.multiply(seguroVehicular.divide(BigDecimal.valueOf(100), MC)).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            // La amortización es la cuota constante menos interés y seguros
            BigDecimal amortizacion = cuotaOrdinaria.subtract(interes).subtract(segDesgrav).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            BigDecimal saldoFinal = saldoInicialPeriodo.subtract(amortizacion).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal cuotaTotal = interes.add(amortizacion).add(segDesgrav).add(segVeh).add(portes).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            BigDecimal flujoPeriodo = cuotaTotal.negate();

            // Lógica para la última cuota (Se paga el Valor Residual / Balloon)
            if (esUltimoMes) {
                saldoFinal = BigDecimal.ZERO;
                flujoPeriodo = flujoPeriodo.subtract(valorResidual); // Flujo salta porque se suma el desembolso del VR
                totalPagado = totalPagado.add(valorResidual);
            }

            filas.add(CronogramaFilaDTO.builder()
                    .numeroCuota(cuotaNum++)
                    .fechaPago(fechaInicio.plusMonths(periodoGracia + k + 1))
                    .saldoInicial(saldoInicialPeriodo.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
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
            flujos.add(flujoPeriodo);
            saldo = saldoFinal;
        }

        // ==========================================
        // --- INDICADORES FINANCIEROS
        // ==========================================

        // Tasa de descuento mensual (COKi) para el VAN
        BigDecimal cokDecimal = (tasaDescuento != null) ? tasaDescuento.divide(BigDecimal.valueOf(100), MC) : BigDecimal.ZERO;
        BigDecimal cokMensual = convertirTEAaTEM(cokDecimal);

        BigDecimal van = calcularVAN(flujos, cokMensual); // <-- Ahora usa el COK mensual
        BigDecimal tirMensual = calcularTIR(flujos);
        BigDecimal tcea = anualizar(tirMensual);

        return ResultadoCalculoDTO.builder()
                .tea(tea.setScale(ESCALA_TASA, RoundingMode.HALF_UP))
                .tem(tem.setScale(ESCALA_TASA, RoundingMode.HALF_UP))
                .cuotaOrdinaria(cuotaOrdinaria.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                .cuotaBalloon(valorResidual.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                .totalIntereses(totalIntereses.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                .totalPagado(totalPagado.setScale(ESCALA_MONTO, RoundingMode.HALF_UP))
                .van(van.setScale(4, RoundingMode.HALF_UP))
                .tir(tirMensual.setScale(ESCALA_TASA, RoundingMode.HALF_UP)) // Convención: devolver la mensual o anual según prefieras
                .tcea(tcea.setScale(ESCALA_TASA, RoundingMode.HALF_UP))
                .cronograma(filas)
                .build();
    }

    // =========================================================================
    // CONVERSIÓN DE TASAS
    // =========================================================================

    public BigDecimal convertirATEA(BigDecimal tasa, ConfiguracionCredito.TipoTasa tipo, ConfiguracionCredito.FrecuenciaCapitalizacion frecuencia) {
        if (tipo == ConfiguracionCredito.TipoTasa.EFECTIVA) {
            return tasa.divide(BigDecimal.valueOf(100), MC);
        }
        int m = getFrecuenciaAnual(frecuencia);
        BigDecimal tasaDecimal = tasa.divide(BigDecimal.valueOf(100), MC);
        BigDecimal base = BigDecimal.ONE.add(tasaDecimal.divide(BigDecimal.valueOf(m), MC));
        return base.pow(m, MC).subtract(BigDecimal.ONE);
    }

    public BigDecimal convertirTEAaTEM(BigDecimal tea) {
        if (tea.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        double teaDouble = tea.doubleValue();
        double temDouble = Math.pow(1 + teaDouble, 30.0 / 360.0) - 1; // Equivalente a 1/12
        return BigDecimal.valueOf(temDouble).setScale(ESCALA_TASA, RoundingMode.HALF_UP);
    }

    // =========================================================================
    // CUOTA COMPRA INTELIGENTE (Equivalente a Excel PAGO)
    // =========================================================================

    /**
     * Fórmula equivalente a la función PAGO de Excel con un Valor Final (-VR).
     * C = [ P * i - RV * i / (1+i)^n ] / [ 1 - 1/(1+i)^n ]
     */
    public BigDecimal calcularCuotaCompraInteligente(BigDecimal capital, BigDecimal valorResidual, BigDecimal iTotal, int n) {
        double iTot = iTotal.doubleValue();
        double p = capital.doubleValue();
        double rv = valorResidual.doubleValue();

        double factor = Math.pow(1 + iTot, n);
        double cuota = (p * iTot - (rv * iTot / factor)) / (1 - (1 / factor));

        return BigDecimal.valueOf(cuota).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
    }

    // =========================================================================
    // INDICADORES FINANCIEROS
    // =========================================================================

    public BigDecimal calcularVAN(List<BigDecimal> flujos, BigDecimal cokMensual) {
        double cok = cokMensual.doubleValue();
        double van = 0;
        for (int k = 0; k < flujos.size(); k++) {
            van += flujos.get(k).doubleValue() / Math.pow(1 + cok, k);
        }
        return BigDecimal.valueOf(van);
    }

    public BigDecimal calcularTIR(List<BigDecimal> flujos) {
        double r = 0.01;
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

    public BigDecimal anualizar(BigDecimal tirMensual) {
        double tir = Math.pow(1 + tirMensual.doubleValue(), 360.0 / 30.0) - 1; // Equivalente a ^12
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