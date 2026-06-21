package com.upc.creditovehicular.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "HISTORIAL_CAMBIOS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistorialCambios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long idHistorial;

    @Column(name = "entidad", nullable = false, length = 50)
    private String entidad;

    @Column(name = "id_entidad", nullable = false)
    private Long idEntidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 10)
    private Accion accion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @PrePersist
    public void prePersist() {
        this.fecha = LocalDateTime.now();
    }

    public enum Accion { INSERT, UPDATE, DELETE }
}
