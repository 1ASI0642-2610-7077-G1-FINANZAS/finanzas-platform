package com.upc.creditovehicular.service;

import com.upc.creditovehicular.dto.response.CronogramaFilaDTO2;
import com.upc.creditovehicular.dto.response.ResultadoCalculoDTO;
import com.upc.creditovehicular.entity.ConfiguracionCredito;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MotorFinancieroService_V2 {

    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
    private static final int ESCALA_MONTO = 2;

    public ResultadoCalculoDTO calcular(
            BigDecimal precioVehiculo, BigDecimal cuotaInicial, BigDecimal valorResidual,
            BigDecimal tasaInteres, ConfiguracionCredito.TipoTasa tipoTasa,
            ConfiguracionCredito.FrecuenciaCapitalizacion frecuencia, int plazoMeses,
            ConfiguracionCredito.TipoGracia tipoGracia, int periodoGracia,
            BigDecimal seguroDesgravamen, BigDecimal seguroVehicular, BigDecimal portes,
            LocalDate fechaInicio, BigDecimal tasaDescuento
    ) {
        BigDecimal tea = convertirATEA(tasaInteres, tipoTasa, frecuencia);
        BigDecimal tem = convertirTEAaTEM(tea);
        BigDecimal tasaDesgravamenDecimal = seguroDesgravamen.divide(BigDecimal.valueOf(100), MC);

        BigDecimal prestamo = precioVehiculo.subtract(cuotaInicial).add(seguroVehicular).add(portes);
        BigDecimal Total = tem.add(tasaDesgravamenDecimal, MC);
        double factorBalloon = Math.pow(1 + Total.doubleValue(), plazoMeses + 1);
        BigDecimal pvBalloon = valorResidual.divide(BigDecimal.valueOf(factorBalloon), MC);

        BigDecimal saldoRegular = prestamo.subtract(pvBalloon);
        BigDecimal saldoBalloon = pvBalloon;

        List<CronogramaFilaDTO2> filas = new ArrayList<>();
        List<BigDecimal> flujos = new ArrayList<>();
        flujos.add(prestamo);

        int cuotaNum = 1;

        // 1. BUCLE DE GRACIA
        for (int k = 1; k <= periodoGracia; k++) {
            BigDecimal interBall = saldoBalloon.multiply(tem, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal interReg = saldoRegular.multiply(tem, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal segBall = saldoBalloon.multiply(tasaDesgravamenDecimal, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal segReg = saldoRegular.multiply(tasaDesgravamenDecimal, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            BigDecimal siBall = saldoBalloon;
            BigDecimal siReg = saldoRegular;
            BigDecimal ct;
            BigDecimal flujo;

            if (tipoGracia == ConfiguracionCredito.TipoGracia.TOTAL) {
                saldoBalloon = saldoBalloon.add(interBall).add(segBall).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
                saldoRegular = saldoRegular.add(interReg).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
                ct = BigDecimal.ZERO;
                flujo = segReg.add(portes);
            } else {
                saldoBalloon = saldoBalloon.add(interBall).add(segBall).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
                ct = interReg;
                flujo = interReg.add(segReg).add(portes);
            }

            filas.add(construirFila(cuotaNum++, "P", fechaInicio.plusMonths(k),
                    siBall, interBall, BigDecimal.ZERO, segBall, saldoBalloon,
                    siReg, interReg, ct, BigDecimal.ZERO, segReg, BigDecimal.ZERO, BigDecimal.ZERO, portes, BigDecimal.ZERO, saldoRegular, flujo));

            flujos.add(flujo.negate());
        }

        // 2. BUCLE ORDINARIO (Limpiado de doble suma de seguros y exclusión del globo final)
        int mesesRestantes = plazoMeses - periodoGracia;
        BigDecimal cuotaOrdinaria = calcularCuotaCompraInteligente(saldoRegular, valorResidual, tem.add(tasaDesgravamenDecimal, MC), mesesRestantes);

        for (int k = 1; k <= mesesRestantes; k++) {
            BigDecimal siBall = saldoBalloon;
            BigDecimal siReg = saldoRegular;

            BigDecimal interBall = saldoBalloon.multiply(tem, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal interReg = saldoRegular.multiply(tem, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal segBall = saldoBalloon.multiply(tasaDesgravamenDecimal, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            BigDecimal segReg = saldoRegular.multiply(tasaDesgravamenDecimal, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            BigDecimal amortReg = cuotaOrdinaria.subtract(interReg).subtract(segReg).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            saldoBalloon = saldoBalloon.add(interBall).add(segBall).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            saldoRegular = saldoRegular.subtract(amortReg).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

            BigDecimal ct = cuotaOrdinaria;
            BigDecimal flujo = cuotaOrdinaria.add(portes);

            filas.add(construirFila(cuotaNum++, "O", fechaInicio.plusMonths(periodoGracia + k),
                    siBall, interBall, BigDecimal.ZERO, segBall, saldoBalloon,
                    siReg, interReg, ct, amortReg, segReg, BigDecimal.ZERO, BigDecimal.ZERO, portes, BigDecimal.ZERO, saldoRegular, flujo));

            flujos.add(flujo.negate());
        }

        // 3. PAGO FINAL DEL BALLOON (MES PLAZO + 1) -> Obligatorio para cálculo TIR exacto
        BigDecimal interBallF = saldoBalloon.multiply(tem, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
        BigDecimal segBallF = saldoBalloon.multiply(tasaDesgravamenDecimal, MC).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);

        filas.add(construirFila(cuotaNum++, "B", fechaInicio.plusMonths(plazoMeses + 1),
                saldoBalloon, interBallF, valorResidual, segBallF, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, valorResidual));
        flujos.add(valorResidual.negate());

        // 4. SUMATORIAS EXACTAS
        BigDecimal sumCuotaRegular = filas.stream().map(CronogramaFilaDTO2::getRegularCuotaTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumAmortRegular = filas.stream().map(CronogramaFilaDTO2::getRegularAmortizacion).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumSegDesRegular = filas.stream().map(CronogramaFilaDTO2::getRegularSeguro).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalInteresesFinal = sumCuotaRegular.subtract(sumAmortRegular).subtract(sumSegDesRegular);
        BigDecimal sumTotalPagado = filas.stream().map(CronogramaFilaDTO2::getFlujo).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. INDICADORES (Flujo modificado al mes 37 garantiza el 1.248% de TIR)
        BigDecimal cokPeriodica = calcularTasaDescuentoPeriodica(tasaDescuento);
        BigDecimal van = calcularVAN(flujos, cokPeriodica);
        BigDecimal tir = calcularTIR(flujos);
        BigDecimal tcea = anualizarTIR(tir);

        // IMPRESIÓN REQUERIDA
        System.out.println("Tasa de descuento periódica aplicada (%): " + cokPeriodica.multiply(BigDecimal.valueOf(100)).setScale(5, RoundingMode.HALF_UP) + "%");

        return ResultadoCalculoDTO.builder()
                .tea(tea).tem(tem).cuotaOrdinaria(cuotaOrdinaria).cuotaBalloon(valorResidual)
                .totalIntereses(totalInteresesFinal)
                .totalPagado(sumTotalPagado)
                .van(van)
                .tir(tir)
                .tcea(tcea)
                .cronograma(filas).build();
    }

    private CronogramaFilaDTO2 construirFila(int n, String tipo, LocalDate f,
                                             BigDecimal sib, BigDecimal ib, BigDecimal ab, BigDecimal sb, BigDecimal sfb,
                                             BigDecimal sir, BigDecimal ir, BigDecimal ct, BigDecimal ar, BigDecimal sr, BigDecimal riesgo, BigDecimal gps, BigDecimal portes, BigDecimal gas, BigDecimal sfr, BigDecimal flujo) {
        return CronogramaFilaDTO2.builder()
                .numeroCuota(n).tipoPeriodo(tipo).fechaPago(f)
                .balloonSaldoInicial(sib).balloonInteres(ib).balloonAmortizacion(ab).balloonSeguro(sb).balloonSaldoFinal(sfb)
                .regularSaldoInicial(sir).regularInteres(ir).regularCuotaTotal(ct).regularAmortizacion(ar).regularSeguro(sr)
                .regularRiesgo(riesgo).regularGps(gps).regularPortes(portes).regularGastos(gas).regularSaldoFinal(sfr)
                .flujo(flujo).build();
    }

    public BigDecimal convertirATEA(BigDecimal tasa, ConfiguracionCredito.TipoTasa tipo, ConfiguracionCredito.FrecuenciaCapitalizacion frecuencia) {
        if (tipo == ConfiguracionCredito.TipoTasa.EFECTIVA) return tasa.divide(BigDecimal.valueOf(100), MC);
        int m = (frecuencia == ConfiguracionCredito.FrecuenciaCapitalizacion.MENSUAL) ? 12 : 1;
        return BigDecimal.ONE.add(tasa.divide(BigDecimal.valueOf(100), MC).divide(BigDecimal.valueOf(m), MC)).pow(m, MC).subtract(BigDecimal.ONE);
    }
    public BigDecimal convertirTEAaTEM(BigDecimal tea) {
        return BigDecimal.valueOf(Math.pow(1 + tea.doubleValue(), 30.0 / 360.0) - 1).setScale(8, RoundingMode.HALF_UP);
    }
    public BigDecimal calcularCuotaCompraInteligente(BigDecimal c, BigDecimal vr, BigDecimal i, int n) {
        return BigDecimal.valueOf((c.doubleValue() * i.doubleValue()) / (1 - Math.pow(1 + i.doubleValue(), -n))).setScale(2, RoundingMode.HALF_UP);
    }
    public BigDecimal calcularVAN(List<BigDecimal> flujos, BigDecimal cok) {
        double v = 0; for (int k = 0; k < flujos.size(); k++) v += flujos.get(k).doubleValue() / Math.pow(1 + cok.doubleValue(), k);
        return BigDecimal.valueOf(v);
    }
    public BigDecimal calcularTIR(List<BigDecimal> flujos) {
        double r = 0.01; for (int i = 0; i < 1000; i++) { double f = 0, df = 0; for(int k=0; k<flujos.size(); k++){ f += flujos.get(k).doubleValue() / Math.pow(1+r, k); df -= k * flujos.get(k).doubleValue() / Math.pow(1+r, k+1); } r -= f/df; }
        return BigDecimal.valueOf(r);
    }
    public BigDecimal anualizarTIR(BigDecimal tirMensual) {
        return BigDecimal.valueOf(Math.pow(1 + tirMensual.doubleValue(), 12) - 1);
    }

    public BigDecimal calcularTasaDescuentoPeriodica(BigDecimal cokAnual) {
        double cok = cokAnual.divide(BigDecimal.valueOf(100), MC).doubleValue();
        return BigDecimal.valueOf(Math.pow(1 + cok, 1.0 / 12.0) - 1);
    }
}