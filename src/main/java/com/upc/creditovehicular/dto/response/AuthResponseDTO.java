package com.upc.creditovehicular.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponseDTO {
    private String token;
    private String username;
    private String rol;
    private String mensaje;
}
