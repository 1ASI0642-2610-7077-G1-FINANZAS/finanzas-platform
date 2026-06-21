package com.upc.creditovehicular.service.impl;

import com.upc.creditovehicular.dto.request.CreditoRequestDTO;
import com.upc.creditovehicular.dto.response.ResultadoCalculoDTO;
import com.upc.creditovehicular.entity.*;
import com.upc.creditovehicular.repository.*;
import com.upc.creditovehicular.service.MotorFinancieroService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditoService {

    private final CreditoRepository creditoRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ConfiguracionCreditoRepository configRepository;
    private final CronogramaPagoRepository cronogramaRepository;
    private final IndicadoresFinancierosRepository indicadoresRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialCambiosRepository historialRepository;
    private final MotorFinancieroService motorFinanciero;

    @Transactional
    public ResultadoCalculoDTO crearCredito(CreditoRequestDTO dto, String username) {
        Cliente cliente = clienteRepository.findById(dto.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        Vehiculo vehiculo = vehiculoRepository.findById(dto.getIdVehiculo())
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        ConfiguracionCredito config = ConfiguracionCredito.builder()
                .moneda(ConfiguracionCredito.Moneda.valueOf(dto.getMoneda().name()))
                .tipoTasa(dto.getTipoTasa())
                .tasaInteres(dto.getTasaInteres())
                .frecuenciaCapitalizacion(dto.getFrecuenciaCapitalizacion())
                .plazoMeses(dto.getPlazoMeses())
                .tipoGracia(dto.getTipoGracia())
                .periodoGracia(dto.getPeriodoGracia())
                .seguroDesgravamen(dto.getSeguroDesgravamen())
                .seguroVehicular(dto.getSeguroVehicular())
                .portes(dto.getPortes())
                .build();
        configRepository.save(config);

        ResultadoCalculoDTO resultado = motorFinanciero.calcular(
                dto.getPrecioVehiculo(), dto.getCuotaInicial(), dto.getValorResidual(),
                dto.getTasaInteres(), dto.getTipoTasa(), dto.getFrecuenciaCapitalizacion(),
                dto.getPlazoMeses(), dto.getTipoGracia(), dto.getPeriodoGracia(),
                dto.getSeguroDesgravamen(), dto.getSeguroVehicular(), dto.getPortes(),
                dto.getFechaInicio()
        );

        Credito credito = Credito.builder()
                .cliente(cliente).vehiculo(vehiculo).configuracion(config).usuario(usuario)
                .montoPrestamo(dto.getPrecioVehiculo().subtract(dto.getCuotaInicial()))
                .cuotaInicial(dto.getCuotaInicial())
                .valorResidual(dto.getValorResidual())
                .fechaInicio(dto.getFechaInicio())
                .estado(Credito.Estado.SIMULADO)
                .build();
        creditoRepository.save(credito);

        List<CronogramaPago> filas = resultado.getCronograma().stream()
                .map(f -> CronogramaPago.builder()
                        .credito(credito).numeroCuota(f.getNumeroCuota()).fechaPago(f.getFechaPago())
                        .saldoInicial(f.getSaldoInicial()).interes(f.getInteres())
                        .amortizacion(f.getAmortizacion()).seguroDesgravamen(f.getSeguroDesgravamen())
                        .seguroVehicular(f.getSeguroVehicular()).portes(f.getPortes())
                        .cuotaTotal(f.getCuotaTotal()).saldoFinal(f.getSaldoFinal())
                        .tipoPeriodo(f.getTipoPeriodo()).build())
                .toList();
        cronogramaRepository.saveAll(filas);

        indicadoresRepository.save(IndicadoresFinancieros.builder()
                .credito(credito).van(resultado.getVan()).tir(resultado.getTir())
                .tcea(resultado.getTcea()).cuotaOrdinaria(resultado.getCuotaOrdinaria())
                .cuotaBalloon(resultado.getCuotaBalloon()).totalIntereses(resultado.getTotalIntereses())
                .totalPagado(resultado.getTotalPagado()).build());

        historialRepository.save(HistorialCambios.builder()
                .entidad("CREDITO").idEntidad(credito.getIdCredito())
                .accion(HistorialCambios.Accion.INSERT).usuario(usuario)
                .descripcion("Crédito Compra Inteligente - cliente " + cliente.getNumeroDocumento())
                .build());

        return resultado;
    }

    public ResultadoCalculoDTO simular(CreditoRequestDTO dto) {
        return motorFinanciero.calcular(
                dto.getPrecioVehiculo(), dto.getCuotaInicial(), dto.getValorResidual(),
                dto.getTasaInteres(), dto.getTipoTasa(), dto.getFrecuenciaCapitalizacion(),
                dto.getPlazoMeses(), dto.getTipoGracia(), dto.getPeriodoGracia(),
                dto.getSeguroDesgravamen(), dto.getSeguroVehicular(), dto.getPortes(),
                dto.getFechaInicio()
        );
    }

    public List<Credito> listarPorCliente(Long idCliente) {
        return creditoRepository.findByCliente_IdCliente(idCliente);
    }
}
