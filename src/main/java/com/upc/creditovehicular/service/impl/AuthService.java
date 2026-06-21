package com.upc.creditovehicular.service.impl;

import com.upc.creditovehicular.dto.request.LoginRequestDTO;
import com.upc.creditovehicular.dto.request.RegisterRequestDTO;
import com.upc.creditovehicular.dto.response.AuthResponseDTO;
import com.upc.creditovehicular.entity.Usuario;
import com.upc.creditovehicular.repository.UsuarioRepository;
import com.upc.creditovehicular.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponseDTO registrar(RegisterRequestDTO dto) {
        if (usuarioRepository.existsByUsername(dto.getUsername()))
            throw new RuntimeException("El username ya está en uso");
        Usuario usuario = Usuario.builder()
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .rol(dto.getRol() != null ? dto.getRol() : Usuario.Rol.OPERADOR)
                .estado(Usuario.Estado.ACTIVO)
                .build();
        usuarioRepository.save(usuario);
        String token = jwtUtil.generateToken(usuario.getUsername(), usuario.getRol().name());
        return AuthResponseDTO.builder().token(token).username(usuario.getUsername())
                .rol(usuario.getRol().name()).mensaje("Usuario registrado correctamente").build();
    }

    public AuthResponseDTO login(LoginRequestDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        Usuario usuario = usuarioRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        String token = jwtUtil.generateToken(usuario.getUsername(), usuario.getRol().name());
        return AuthResponseDTO.builder().token(token).username(usuario.getUsername())
                .rol(usuario.getRol().name()).mensaje("Login exitoso").build();
    }
}
