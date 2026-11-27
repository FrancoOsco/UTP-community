package com.template.auth.academic;
import java.time.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import static com.template.auth.academic.AcademicDb.*;

@RestController @RequestMapping("/api/calendario")
public class CalendarController {
    private final AcademicDb a;
    public CalendarController(AcademicDb a){this.a=a;}
    public static final List<String> TYPES=List.of("Examen","Exposición","Recuperación","Entrega de proyecto","Importante");
    public static final List<String> COLORS=List.of("#f4b9bc","#f5c8df","#f8d0b4","#f7e2ab","#dce8b4","#c0e4d1","#b9e2e2","#c5d9f5","#dcd0f5","#d9dce2");
    record Activity(LocalDate day,String kind,String course,String description,String color){}
    private void validate(Activity p){if(p.day()==null||p.day().getYear()<1900||p.day().getYear()>2200||p.kind()==null||p.color()==null||!TYPES.contains(p.kind())||!COLORS.contains(p.color()))throw bad("Fecha, categoría o color inválido");str(p.course(),200,true);str(p.description(),2000,false);}
    @GetMapping public Object list(Authentication auth,@RequestParam int mes,@RequestParam int anio){if(mes<1||mes>12||anio<1900||anio>2200)throw bad("Mes inválido");LocalDate start=LocalDate.of(anio,mes,1);return a.db.queryForList("select id,owner_id,day::text as day,kind,course,description,color from academic_activity where owner_id=? and day>=? and day<? order by day,id",a.user(auth),start,start.plusMonths(1));}
    @PostMapping public Object create(Authentication auth,@RequestBody Activity p){validate(p);return a.one("insert into academic_activity(owner_id,day,kind,course,description,color) values(?,?,?,?,?,?) returning *",a.user(auth),p.day(),p.kind(),str(p.course(),200,true),str(p.description(),2000,false),p.color());}
    @PutMapping("/{id}") public Object update(Authentication auth,@PathVariable long id,@RequestBody Activity p){validate(p);return a.one("update academic_activity set day=?,kind=?,course=?,description=?,color=? where id=? and owner_id=? returning *",p.day(),p.kind(),str(p.course(),200,true),str(p.description(),2000,false),p.color(),id,a.user(auth));}
    @DeleteMapping("/{id}") public void delete(Authentication auth,@PathVariable long id){a.changed(a.db.update("delete from academic_activity where id=? and owner_id=?",id,a.user(auth)));}
}
