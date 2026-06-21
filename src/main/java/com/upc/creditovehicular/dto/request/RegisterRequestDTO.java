package com.upc.creditovehicular.dto.request;

import com.upc.creditovehicular.entity.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegisterRequestDTO {
    @NotBlank @Size(min = 4, max = 50) private String username;
    @NotBlank @Size(min = 6) private String password;
    private Usuario.Rol rol = Usuario.Rol.OPERADOR;
}
