package com.template.auth.service;

import com.template.auth.entity.*;
import com.template.auth.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForoServiceTest {
    @Mock UsuarioRepository usuarios;
    @Mock ForoPublicacionRepository publicaciones;
    @Mock ForoRespuestaRepository respuestas;
    @Mock ForoVotoRepository votos;
    @Mock RespuestaVotoRepository votosRespuesta;
    @InjectMocks ForoService service;

    @Test void noPermiteVotarLaPropiaPublicacion() {
        Usuario u = new Usuario();
        u.setId(10L);
        u.setEmail("u12345678@utp.edu.pe");
        ForoPublicacion p = new ForoPublicacion();
        p.setId(8L);
        p.setAutor(u);
        when(usuarios.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(publicaciones.findById(8L)).thenReturn(Optional.of(p));
        assertThrows(ResponseStatusException.class, () -> service.votar(u.getEmail(), 8L, 1));
        verifyNoInteractions(votos);
    }
}
