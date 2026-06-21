package com.upc.creditovehicular.controller;

import com.upc.creditovehicular.dto.request.CreditoRequestDTO;
import com.upc.creditovehicular.dto.response.ResultadoCalculoDTO;
import com.upc.creditovehicular.entity.Credito;
import com.upc.creditovehicular.service.impl.CreditoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/creditos")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "${ip.frontend}",
        allowCredentials = "true",
        exposedHeaders = "Authorization"
)
public class CreditoController {
    private final CreditoService creditoService;

    @PostMapping("/simular")
    public ResponseEntity<ResultadoCalculoDTO> simular(@Valid @RequestBody CreditoRequestDTO dto) {
        return ResponseEntity.ok(creditoService.simular(dto));
    }

    @PostMapping
    public ResponseEntity<ResultadoCalculoDTO> crear(@Valid @RequestBody CreditoRequestDTO dto,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditoService.crearCredito(dto, user.getUsername()));
    }

    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<Credito>> porCliente(@PathVariable Long idCliente) {
        return ResponseEntity.ok(creditoService.listarPorCliente(idCliente));
    }
}
