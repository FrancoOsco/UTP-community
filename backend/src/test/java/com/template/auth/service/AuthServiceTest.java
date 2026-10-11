package com.template.auth.service;

import com.template.auth.dto.AuthTokens;
import com.template.auth.dto.LoginRequest;
import com.template.auth.dto.UsuarioRequest;
import com.template.auth.entity.Usuario;
import com.template.auth.exception.CredencialesInvalidasException;
import com.template.auth.exception.EmailNoVerificadoException;
import com.template.auth.exception.EmailYaRegistradoException;
import com.template.auth.repository.UsuarioRepository;
import com.template.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void debeIniciarSesionCorrectamente() {
        LoginRequest request = new LoginRequest();
        request.setEmail("u12345678@utp.edu.pe");
        request.setPassword("Password123!");

        Usuario usuario = crearUsuarioVerificado();

        when(usuarioRepository.findByEmail("u12345678@utp.edu.pe"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123!", "password-encriptado"))
                .thenReturn(true);
        when(jwtService.generarToken("u12345678@utp.edu.pe"))
                .thenReturn("access-token");
        when(refreshTokenService.crearRefreshToken(usuario))
                .thenReturn("refresh-token");

        AuthTokens resultado = authService.login(request);

        assertNotNull(resultado);
        assertEquals("access-token", resultado.getAccessToken());
        assertEquals("refresh-token", resultado.getRefreshToken());

        verify(jwtService).generarToken("u12345678@utp.edu.pe");
        verify(refreshTokenService).crearRefreshToken(usuario);
    }

    @Test
    void debeRechazarPasswordIncorrecto() {
        LoginRequest request = new LoginRequest();
        request.setEmail("u12345678@utp.edu.pe");
        request.setPassword("incorrecta");

        Usuario usuario = crearUsuarioVerificado();

        when(usuarioRepository.findByEmail("u12345678@utp.edu.pe"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "password-encriptado"))
                .thenReturn(false);

        assertThrows(
                CredencialesInvalidasException.class,
                () -> authService.login(request)
        );

        verify(jwtService, never()).generarToken(anyString());
    }

    @Test
    void debeRechazarEmailNoVerificado() {
        LoginRequest request = new LoginRequest();
        request.setEmail("u12345678@utp.edu.pe");
        request.setPassword("Password123!");

        Usuario usuario = crearUsuarioVerificado();
        usuario.setEmailVerificado(false);

        when(usuarioRepository.findByEmail("u12345678@utp.edu.pe"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123!", "password-encriptado"))
                .thenReturn(true);

        assertThrows(
                EmailNoVerificadoException.class,
                () -> authService.login(request)
        );

        verify(jwtService, never()).generarToken(anyString());
    }

    @Test
    void debeRechazarRegistroConEmailRepetido() {
        UsuarioRequest request = new UsuarioRequest();
        request.setNombre("Franco");
        request.setApellido("Osco");
        request.setEmail("u12345678@utp.edu.pe");
        request.setPassword("Password123!");

        when(usuarioRepository.findByEmail("u12345678@utp.edu.pe"))
                .thenReturn(Optional.of(crearUsuarioVerificado()));

        assertThrows(
                EmailYaRegistradoException.class,
                () -> authService.register(request)
        );

        verify(usuarioRepository, never()).save(any());
        verify(emailVerificationService, never()).crearVerificacion(any());
    }


    @Test
    void debeRechazarRegistroNoInstitucional() {
        UsuarioRequest request = new UsuarioRequest();
        request.setEmail("alumno@gmail.com");
        request.setNombre("Ana");
        request.setApellido("Ruiz");
        request.setPassword("Password123!");

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> authService.register(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void debeRechazarLoginNoInstitucional() {
        LoginRequest request = new LoginRequest();
        request.setEmail("alumno@otrauniversidad.edu.pe");
        request.setPassword("Password123!");
        assertThrows(CredencialesInvalidasException.class,
                () -> authService.login(request));
        verifyNoInteractions(usuarioRepository);
    }

    private Usuario crearUsuarioVerificado() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Usuario");
        usuario.setApellido("Prueba");
        usuario.setEmail("u12345678@utp.edu.pe");
        usuario.setPassword("password-encriptado");
        usuario.setRol("USUARIO");
        usuario.setEmailVerificado(true);
        return usuario;
    }
}
