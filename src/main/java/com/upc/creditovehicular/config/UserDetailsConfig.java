package com.upc.creditovehicular.config;

import com.upc.creditovehicular.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class UserDetailsConfig {
    private final UsuarioRepository usuarioRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usuarioRepository.findByUsername(username)
                .map(u -> new User(
                        u.getUsername(),
                        u.getPasswordHash(),
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }
}
