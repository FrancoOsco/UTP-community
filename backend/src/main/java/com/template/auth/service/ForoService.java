package com.template.auth.service;

import com.template.auth.dto.*;
import com.template.auth.entity.*;
import com.template.auth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ForoService {
    private final UsuarioRepository usuarios;
    private final ForoPublicacionRepository publicaciones;
    private final ForoRespuestaRepository respuestas;
    private final ForoVotoRepository votos;
    private final RespuestaVotoRepository votosRespuesta;

    private Usuario usuario(String email) {
        return usuarios.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private ForoPublicacion publicacion(Long id) {
        return publicaciones.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));
    }

    private ForoRespuesta respuesta(Long id) {
        return respuestas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Respuesta no encontrada"));
    }

    private Map<String, Object> salidaPublicacion(ForoPublicacion p, Long usuarioId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("categoria", p.getCategoria().name());
        m.put("titulo", p.getTitulo());
        m.put("contenido", p.getContenido());
        m.put("autor", p.getAutor().getNombre() + " " + p.getAutor().getApellido());
        m.put("autorId", p.getAutor().getId());
        m.put("creadoEn", p.getCreadoEn());
        m.put("respuestas", respuestas.countByPublicacionId(p.getId()));
        m.put("puntos", votos.puntaje(p.getId()));
        m.put("miVoto", votos.findByPublicacionIdAndUsuarioId(p.getId(), usuarioId).map(ForoVoto::getValor).orElse(0));
        return m;
    }

    private Map<String, Object> salidaRespuesta(ForoRespuesta r, Long usuarioId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("publicacionId", r.getPublicacion().getId());
        m.put("contenido", r.getContenido());
        m.put("autor", r.getAutor().getNombre() + " " + r.getAutor().getApellido());
        m.put("autorId", r.getAutor().getId());
        m.put("creadoEn", r.getCreadoEn());
        m.put("puntos", votosRespuesta.puntaje(r.getId()));
        m.put("miVoto", votosRespuesta.findByRespuestaIdAndUsuarioId(r.getId(), usuarioId).map(RespuestaVoto::getValor).orElse(0));
        return m;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listar(String email, ForoCategoria categoria, String query, int pagina) {
        Long userId = usuario(email).getId();
        String texto = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        return publicaciones.buscar(categoria, texto, PageRequest.of(Math.max(0, pagina), 10, Sort.by(Sort.Direction.DESC, "creadoEn")))
                .map(p -> salidaPublicacion(p, userId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detalle(String email, Long id) {
        Long userId = usuario(email).getId();
        ForoPublicacion p = publicacion(id);
        Map<String, Object> resultado = new LinkedHashMap<>(salidaPublicacion(p, userId));
        resultado.put("listaRespuestas", respuestas.findByPublicacionIdOrderByCreadoEnAsc(id).stream().map(r -> salidaRespuesta(r, userId)).toList());
        return resultado;
    }

    @Transactional
    public Map<String, Object> crear(String email, ForoCrearRequest req) {
        ForoPublicacion p = new ForoPublicacion();
        p.setAutor(usuario(email));
        p.setCategoria(req.categoria());
        p.setTitulo(req.titulo().trim());
        p.setContenido(req.contenido().trim());
        return salidaPublicacion(publicaciones.saveAndFlush(p), p.getAutor().getId());
    }

    @Transactional
    public Map<String, Object> responder(String email, Long id, RespuestaCrearRequest req) {
        ForoPublicacion p = publicacion(id);
        Usuario autor = usuario(email);
        ForoRespuesta r = new ForoRespuesta();
        r.setPublicacion(p);
        r.setAutor(autor);
        r.setContenido(req.contenido().trim());
        return salidaRespuesta(respuestas.saveAndFlush(r), autor.getId());
    }

    @Transactional
    public Map<String, Object> votar(String email, Long id, int valor) {
        Usuario u = usuario(email);
        ForoPublicacion p = publicacion(id);
        if (p.getAutor().getId().equals(u.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes votar tu propia publicación.");
        ForoVoto v = votos.findByPublicacionIdAndUsuarioId(id, u.getId()).orElse(null);
        if (valor == 0) {
            if (v != null) votos.delete(v);
        } else {
            if (v == null) {
                v = new ForoVoto();
                v.setPublicacion(p);
                v.setUsuario(u);
            }
            v.setValor(valor);
            votos.save(v);
        }
        votos.flush();
        return Map.of("puntos", votos.puntaje(id), "miVoto", valor);
    }

    @Transactional
    public Map<String, Object> votarRespuesta(String email, Long publicacionId, Long respuestaId, int valor) {
        Usuario u = usuario(email);
        ForoRespuesta r = respuesta(respuestaId);
        if (!r.getPublicacion().getId().equals(publicacionId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (r.getAutor().getId().equals(u.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes votar tu propia respuesta.");
        RespuestaVoto v = votosRespuesta.findByRespuestaIdAndUsuarioId(respuestaId, u.getId()).orElse(null);
        if (valor == 0) {
            if (v != null) votosRespuesta.delete(v);
        } else {
            if (v == null) {
                v = new RespuestaVoto();
                v.setRespuesta(r);
                v.setUsuario(u);
            }
            v.setValor(valor);
            votosRespuesta.save(v);
        }
        votosRespuesta.flush();
        return Map.of("puntos", votosRespuesta.puntaje(respuestaId), "miVoto", valor);
    }
}
