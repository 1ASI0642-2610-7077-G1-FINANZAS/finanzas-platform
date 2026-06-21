package com.upc.creditovehicular.controller;

import com.upc.creditovehicular.entity.Vehiculo;
import com.upc.creditovehicular.repository.VehiculoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "${ip.frontend}",
        allowCredentials = "true",
        exposedHeaders = "Authorization"
)
public class VehiculoController {
    private final VehiculoRepository vehiculoRepository;

    @GetMapping
    public ResponseEntity<List<Vehiculo>> listar() {
        return ResponseEntity.ok(vehiculoRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehiculo> porId(@PathVariable Long id) {
        return vehiculoRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Vehiculo> crear(@Valid @RequestBody Vehiculo vehiculo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehiculoRepository.save(vehiculo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehiculo> actualizar(@PathVariable Long id, @RequestBody Vehiculo datos) {
        return vehiculoRepository.findById(id).map(v -> {
            v.setMarca(datos.getMarca()); v.setModelo(datos.getModelo());
            v.setAnio(datos.getAnio()); v.setPrecio(datos.getPrecio());
            v.setMoneda(datos.getMoneda()); v.setDescripcion(datos.getDescripcion());
            return ResponseEntity.ok(vehiculoRepository.save(v));
        }).orElse(ResponseEntity.notFound().build());
    }
}
