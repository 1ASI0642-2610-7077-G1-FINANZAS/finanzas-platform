package com.upc.creditovehicular.controller;

import com.upc.creditovehicular.entity.Cliente;
import com.upc.creditovehicular.repository.ClienteRepository;
import com.upc.creditovehicular.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "${ip.frontend}",
        allowCredentials = "true",
        exposedHeaders = "Authorization"
)
public class ClienteController {
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<List<Cliente>> listar() {
        return ResponseEntity.ok(clienteRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> porId(@PathVariable Long id) {
        return clienteRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody Cliente cliente,
            @AuthenticationPrincipal UserDetails user) {
        if (clienteRepository.existsByNumeroDocumento(cliente.getNumeroDocumento()))
            return ResponseEntity.badRequest().body(Map.of("error", "Documento ya registrado"));
        usuarioRepository.findByUsername(user.getUsername()).ifPresent(cliente::setUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteRepository.save(cliente));
    }


    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizar(@PathVariable Long id, @RequestBody Cliente datos) {
        return clienteRepository.findById(id).map(c -> {
            c.setNombres(datos.getNombres()); c.setApellidos(datos.getApellidos());
            c.setCorreo(datos.getCorreo()); c.setTelefono(datos.getTelefono());
            c.setDireccion(datos.getDireccion());
            return ResponseEntity.ok(clienteRepository.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }
}
