package com.upc.creditovehicular.service;

import com.upc.creditovehicular.dto.response.CronogramaFilaDTO2;
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

@Service
public class MotorFinancieroService {

    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
    private static final int ESCALA_TASA = 8;
    private static final int ESCALA_MONTO = 2;

    public ResultadoCalculoDTO calcular(
            BigDecimal precioVehiculo, BigDecimal cuotaInicial, BigDecimal valorResidual,
            BigDecimal tasaInteres, ConfiguracionCredito.TipoTasa tipoTasa,
            ConfiguracionCredito.FrecuenciaCapitalizacion frecuencia, int plazoMeses,
            ConfiguracionCredito.TipoGracia tipoGracia, int periodoGracia,
            BigDecimal seguroDesgravamen, BigDecimal seguroVehicular, BigDecimal portes,
            LocalDate fechaInicio, BigDecimal tasaDescuento
    ) {
        BigDecimal montoPrestamo = precioVehiculo.subtract(cuotaInicial);
        BigDecimal tea = convertirATEA(tasaInteres, tipoTasa, frecuencia);
        BigDecimal tem = convertirTEAaTEM(tea);
        BigDecimal tasaDesgravamenDecimal = seguroDesgravamen.divide(BigDecimal.valueOf(100), MC);

        List<CronogramaFilaDTO2> filas = new ArrayList<>();
        BigDecimal saldo = montoPrestamo;
        BigDecimal totalIntereses = BigDecimal.ZERO;
        BigDecimal totalPagado = BigDecimal.ZERO;
        List<BigDecimal> flujos = new ArrayList<>();
        flujos.add(montoPrestamo);

        int cuotaNum = 1;

        // --- PERÍODO DE GRACIA ---
        for (int k = 0; k < periodoGracia; k++) {
            BigDecimal saldoInicialPeriodo = saldo;
            BigDecimal interes = saldoInicialPeriodo.multiply(tem, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal segDesgrav = saldoInicialPeriodo.multiply(tasaDesgravamenDecimal, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal segVeh = precioVehiculo.multiply(seguroVehicular.divide(BigDecimal.valueOf(100), MC)).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            BigDecimal cuotaPeriodo = (tipoGracia == ConfiguracionCredito.TipoGracia.TOTAL) ? BigDecimal.ZERO : interes;
            BigDecimal amortizacion = BigDecimal.ZERO;
            if (tipoGracia == ConfiguracionCredito.TipoGracia.TOTAL) saldo = saldo.add(interes);

            BigDecimal cuotaTotal = cuotaPeriodo.add(segDesgrav).add(segVeh).add(portes).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            filas.add(crearFila(cuotaNum++, fechaInicio.plusMonths(k + 1), "G",
                    saldoInicialPeriodo, interes, amortizacion, segDesgrav.add(segVeh),
                    portes, cuotaTotal, saldo, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, cuotaTotal.negate()));

            totalIntereses = totalIntereses.add(interes);
            totalPagado = totalPagado.add(cuotaTotal);
            flujos.add(cuotaTotal.negate());
        }

        // --- CUOTAS ORDINARIAS ---
        int cuotasRestantes = plazoMeses - periodoGracia;
        BigDecimal iTotal = tem.add(tasaDesgravamenDecimal, MC);
        BigDecimal cuotaOrdinaria = calcularCuotaCompraInteligente(saldo, valorResidual, iTotal, cuotasRestantes);

        for (int k = 0; k < cuotasRestantes; k++) {
            boolean esUltimoMes = (k == cuotasRestantes - 1);
            BigDecimal saldoInicialPeriodo = saldo;

            // 1. Interés calculado sobre el saldo
            BigDecimal interes = saldoInicialPeriodo.multiply(tem, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            // 2. Amortización = Cuota Técnica - Interés (¡Aquí está la clave!)
            BigDecimal amortizacion = cuotaOrdinaria.subtract(interes).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            // 3. Costos Adicionales
            BigDecimal segDesgrav = saldoInicialPeriodo.multiply(tasaDesgravamenDecimal, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal segVeh = precioVehiculo.multiply(seguroVehicular.divide(BigDecimal.valueOf(100), MC)).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            // 4. Cuota Total (Técnica + Adicionales)
            BigDecimal cuotaTotal = cuotaOrdinaria.add(segDesgrav).add(segVeh).add(portes).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            // 5. Saldo Final
            BigDecimal saldoFinal = saldoInicialPeriodo.subtract(amortizacion);
            BigDecimal flujoPeriodo = cuotaTotal.negate();

            // Lógica Balloon
            BigDecimal balloonSaldoInicial = BigDecimal.ZERO;
            BigDecimal balloonAmortizacion = BigDecimal.ZERO;
            BigDecimal balloonSaldoFinal = BigDecimal.ZERO;

            if (esUltimoMes) {
                balloonSaldoInicial = saldoFinal;
                balloonAmortizacion = valorResidual;
                balloonSaldoFinal = BigDecimal.ZERO;
                saldoFinal = BigDecimal.ZERO;
                flujoPeriodo = flujoPeriodo.subtract(valorResidual);
                totalPagado = totalPagado.add(valorResidual);
            }

            filas.add(crearFila(cuotaNum++, fechaInicio.plusMonths(periodoGracia + k + 1), "O",
                    saldoInicialPeriodo, interes, amortizacion, segDesgrav.add(segVeh),
                    portes, cuotaTotal, saldoFinal, balloonSaldoInicial, balloonAmortizacion,
                    BigDecimal.ZERO, BigDecimal.ZERO, balloonSaldoFinal, flujoPeriodo));

            totalIntereses = totalIntereses.add(interes);
            totalPagado = totalPagado.add(cuotaTotal);
            flujos.add(flujoPeriodo);
            saldo = saldoFinal;
        }

        // Indicadores (VAN, TIR, TCEA) se mantienen igual...
        BigDecimal cokDecimal = (tasaDescuento != null) ? tasaDescuento.divide(BigDecimal.valueOf(100), MC) : BigDecimal.ZERO;
        return ResultadoCalculoDTO.builder()
                .tea(tea).tem(tem).cuotaOrdinaria(cuotaOrdinaria).cuotaBalloon(valorResidual)
                .totalIntereses(totalIntereses).totalPagado(totalPagado)
                .van(calcularVAN(flujos, convertirTEAaTEM(cokDecimal)))
                .tir(calcularTIR(flujos)).tcea(anualizar(calcularTIR(flujos)))
                .cronograma(filas).build();
    }

    // Método auxiliar para no repetir código largo
    private CronogramaFilaDTO2 crearFila(int num, LocalDate fecha, String tipo,
                                         BigDecimal saldoIni, BigDecimal intes, BigDecimal amort,
                                         BigDecimal seguro, BigDecimal portes, BigDecimal cuotaTotal,
                                         BigDecimal saldoFin, BigDecimal bSaldoIni, BigDecimal bAmort,
                                         BigDecimal bSeg, BigDecimal bInt, BigDecimal bSaldoFin, BigDecimal flujo) {
        return CronogramaFilaDTO2.builder()
                .numeroCuota(num).fechaPago(fecha).tipoPeriodo(tipo)
                .regularSaldoInicial(saldoIni).regularInteres(intes).regularAmortizacion(amort)
                .regularSeguro(seguro).regularPortes(portes).regularCuotaTotal(cuotaTotal)
                .regularSaldoFinal(saldoFin)
                .balloonSaldoInicial(bSaldoIni).balloonAmortizacion(bAmort)
                .balloonSeguro(bSeg).balloonInteres(bInt).balloonSaldoFinal(bSaldoFin)
                .flujo(flujo)
                .build();
    }

    // --- MÉTODOS DE CÁLCULO (Se mantienen iguales) ---
    public BigDecimal convertirATEA(BigDecimal tasa, ConfiguracionCredito.TipoTasa tipo, ConfiguracionCredito.FrecuenciaCapitalizacion frecuencia) {
        if (tipo == ConfiguracionCredito.TipoTasa.EFECTIVA) return tasa.divide(BigDecimal.valueOf(100), MC);
        int m = getFrecuenciaAnual(frecuencia);
        return BigDecimal.ONE.add(tasa.divide(BigDecimal.valueOf(100 * m), MC)).pow(m, MC).subtract(BigDecimal.ONE);
    }

    public BigDecimal convertirTEAaTEM(BigDecimal tea) {
        if (tea.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(Math.pow(1 + tea.doubleValue(), 30.0 / 360.0) - 1).setScale(ESCALA_TASA, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularCuotaCompraInteligente(BigDecimal capital, BigDecimal valorResidual, BigDecimal iTotal, int n) {
        double iTot = iTotal.doubleValue();
        double p = capital.doubleValue();
        double rv = valorResidual.doubleValue();
        double factor = Math.pow(1 + iTot, n);
        return BigDecimal.valueOf((p * iTot - (rv * iTot / factor)) / (1 - (1 / factor))).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
    }

    // ... incluir métodos calcularVAN, calcularTIR, anualizar, getFrecuenciaAnual igual que en tu original
    public BigDecimal calcularVAN(List<BigDecimal> flujos, BigDecimal cokMensual) {
        double cok = cokMensual.doubleValue();
        double van = 0;
        for (int k = 0; k < flujos.size(); k++) van += flujos.get(k).doubleValue() / Math.pow(1 + cok, k);
        return BigDecimal.valueOf(van);
    }

    public BigDecimal calcularTIR(List<BigDecimal> flujos) {
        double r = 0.01;
        for (int iter = 0; iter < 1000; iter++) {
            double f = 0, df = 0;
            for (int k = 0; k < flujos.size(); k++) {
                double fk = flujos.get(k).doubleValue();
                f += fk / Math.pow(1 + r, k);
                df -= k * fk / Math.pow(1 + r, k + 1);
            }
            if (Math.abs(df) < 1e-10) break;
            double rNuevo = r - f / df;
            if (Math.abs(rNuevo - r) < 1e-10) { r = rNuevo; break; }
            r = rNuevo;
        }
        return BigDecimal.valueOf(r);
    }

    public BigDecimal anualizar(BigDecimal tirMensual) {
        return BigDecimal.valueOf(Math.pow(1 + tirMensual.doubleValue(), 12) - 1);
    }

    private int getFrecuenciaAnual(ConfiguracionCredito.FrecuenciaCapitalizacion f) {
        return switch (f) {
            case DIARIA -> 360; case MENSUAL -> 12; case BIMESTRAL -> 6;
            case TRIMESTRAL -> 4; case SEMESTRAL -> 2; case ANUAL -> 1;
        };
    }
}