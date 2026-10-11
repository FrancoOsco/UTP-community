package com.template.auth.controller;
import com.template.auth.dto.*;
import com.template.auth.entity.ForoCategoria;
import com.template.auth.service.ForoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/foros") @RequiredArgsConstructor
public class ForoController {
    private final ForoService service;
    @GetMapping("/categorias")
    public List<Map<String,String>> categorias(){return List.of(
        Map.of("id","PREGUNTAS","nombre","Preguntas y respuestas"),
        Map.of("id","PROFESORES","nombre","Profesores"),
        Map.of("id","CURSOS","nombre","Cursos"));}
    @GetMapping public Page<Map<String,Object>> listar(Authentication auth,
        @RequestParam(required=false) ForoCategoria categoria,
        @RequestParam(defaultValue="") String buscar,
        @RequestParam(defaultValue="0") int pagina) {return service.listar(auth.getName(),categoria,buscar,pagina);}
    @GetMapping("/{id}") public Map<String,Object> detalle(Authentication auth,@PathVariable Long id){return service.detalle(auth.getName(),id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> crear(Authentication auth,@Valid @RequestBody ForoCrearRequest req){return service.crear(auth.getName(),req);}
    @PostMapping("/{id}/respuestas") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> responder(Authentication auth,@PathVariable Long id,@Valid @RequestBody RespuestaCrearRequest req){return service.responder(auth.getName(),id,req);}
    @PostMapping("/{id}/votos")
    public Map<String,Object> votar(Authentication auth,@PathVariable Long id,@Valid @RequestBody VotoRequest req){return service.votar(auth.getName(),id,req.valor());}
    @PostMapping("/{id}/respuestas/{respuestaId}/votos")
    public Map<String,Object> votarRespuesta(Authentication auth,@PathVariable Long id,@PathVariable Long respuestaId,@Valid @RequestBody VotoRequest req){return service.votarRespuesta(auth.getName(),id,respuestaId,req.valor());}
}
