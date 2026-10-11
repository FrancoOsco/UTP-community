package com.template.auth.controller;
import com.template.auth.dto.PerfilActualizarRequest;
import com.template.auth.entity.Usuario;
import com.template.auth.repository.UsuarioRepository;
import com.template.auth.repository.ForoPublicacionRepository;
import com.template.auth.repository.ForoRespuestaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;
import java.util.LinkedHashMap;
@RestController @RequestMapping("/api/me") @RequiredArgsConstructor
public class PerfilController {
    private final UsuarioRepository usuarios;
    private final ForoPublicacionRepository publicaciones;
    private final ForoRespuestaRepository respuestas;
    private Usuario actual(Authentication auth) {
        return usuarios.findByEmail(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    private Map<String,Object> dto(Usuario u) {
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("id",u.getId());r.put("nombre",u.getNombre());r.put("apellido",u.getApellido());
        r.put("email",u.getEmail());r.put("rol",u.getRol());r.put("verificado",u.isEmailVerificado());
        r.put("biografia",u.getBiografia());r.put("carrera",u.getCarrera());r.put("ciclo",u.getCiclo());
        r.put("publicaciones",publicaciones.countByAutorId(u.getId()));
        r.put("respuestas",respuestas.countByAutorId(u.getId()));
        return r;
    }
    @GetMapping @Transactional(readOnly=true)
    public Map<String,Object> obtener(Authentication auth) {return dto(actual(auth));}
    @PutMapping @Transactional
    public Map<String,Object> actualizar(Authentication auth,@Valid @RequestBody PerfilActualizarRequest req) {
        Usuario u=actual(auth);
        u.setNombre(req.nombre().trim());u.setApellido(req.apellido().trim());
        u.setCarrera(req.carrera()==null?null:req.carrera().trim());
        u.setCiclo(req.ciclo());u.setBiografia(req.biografia()==null?null:req.biografia().trim());
        return dto(usuarios.save(u));
    }
}
