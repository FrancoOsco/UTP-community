package com.template.auth.academic;
import java.time.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.*;
import static com.template.auth.academic.AcademicDb.*;

@RestController @RequestMapping("/api/reuniones")
public class MeetingController {
    private final AcademicDb a;
    public MeetingController(AcademicDb a){this.a=a;}
    record Meeting(String title,String course,String description,String platform,String url,OffsetDateTime starts,OffsetDateTime ends,boolean isPublic,List<Long> participants){}
    record Decision(String status){}
    private void validate(Meeting p){str(p.title(),200,true);str(p.course(),200,true);str(p.description(),2000,false);String host=https(p.url()).getHost().toLowerCase(Locale.ROOT);if(!("MEET".equals(p.platform())&&host.equals("meet.google.com")||"ZOOM".equals(p.platform())&&(host.equals("zoom.us")||host.endsWith(".zoom.us"))))throw bad("Enlace de Meet o Zoom inválido");if(p.starts()==null||p.ends()==null||!p.ends().isAfter(p.starts()))throw bad("La hora final debe ser posterior a la inicial");if(p.participants()!=null&&(p.participants().size()>50||p.participants().stream().anyMatch(Objects::isNull)))throw bad("Máximo 50 participantes");}
    private Map<String,Object> detail(long id,long me){Map<String,Object> m=a.one("""
       select m.id,m.owner_id,m.title,m.course,m.description,m.platform,m.url,m.is_public, to_char(m.starts at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS"Z"') as starts, to_char(m.ends at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS"Z"') as ends,u.nombre||' '||u.apellido as organizer, i.status as invitation_status
       from academic_meeting m join usuario u on u.id=m.owner_id
       left join academic_invitation i on i.meeting_id=m.id and i.user_id=?
       where m.id=? and (m.owner_id=? or m.is_public or i.user_id is not null)
       """,me,id,me);boolean owner=((Number)m.get("owner_id")).longValue()==me;
       m.put("mine",owner);if(!owner&&!Boolean.TRUE.equals(m.get("is_public"))&&!"ACCEPTED".equals(m.get("invitation_status")))m.remove("url");
       m.put("participants",owner?a.db.queryForList("select u.id,u.nombre||' '||u.apellido as name,i.status from academic_invitation i join usuario u on u.id=i.user_id where i.meeting_id=? order by name",id):List.of());return m;}
    @GetMapping public Object list(Authentication auth){long me=a.user(auth);return a.db.queryForList("select distinct m.id,m.starts from academic_meeting m left join academic_invitation i on i.meeting_id=m.id where m.owner_id=? or m.is_public or i.user_id=? order by m.starts",me,me).stream().map(m->detail(((Number)m.get("id")).longValue(),me)).toList();}
    @GetMapping("/pendientes") public Object pending(Authentication auth){return Map.of("count",a.db.queryForObject("select count(*) from academic_invitation where user_id=? and status='PENDING'",Long.class,a.user(auth)));}
    @GetMapping("/{id}") public Object get(Authentication auth,@PathVariable long id){return detail(id,a.user(auth));}
    @GetMapping("/estudiantes") public Object students(Authentication auth,@RequestParam(defaultValue="") String q){if(q.strip().length()<2)return List.of();return a.db.queryForList("select id,nombre||' '||apellido as name, carrera from usuario where id<>? and email_verificado=true and (nombre||' '||apellido ilike ? or lower(email)=lower(?)) order by nombre limit 20",a.user(auth),"%"+str(q,150,true)+"%",q.strip());}
    private void invitations(long id,long me,List<Long> participants){Set<Long> ids=new HashSet<>(participants==null?List.of():participants);ids.remove(me);for(var row:a.db.queryForList("select user_id from academic_invitation where meeting_id=?",id)){long uid=((Number)row.get("user_id")).longValue();if(!ids.contains(uid))a.db.update("delete from academic_invitation where meeting_id=? and user_id=?",id,uid);}for(long uid:ids){int n=a.db.update("insert into academic_invitation(meeting_id,user_id) select ?,id from usuario where id=? and email_verificado=true on conflict do nothing",id,uid);if(n==0&&a.db.queryForObject("select count(*) from academic_invitation where meeting_id=? and user_id=?",Long.class,id,uid)==0)throw bad("Participante no disponible");}}
    @PostMapping @Transactional public Object create(Authentication auth,@RequestBody Meeting p){validate(p);long me=a.user(auth);long id=((Number)a.one("insert into academic_meeting(owner_id,title,course,description,platform,url,starts,ends,is_public) values(?,?,?,?,?,?,?,?,?) returning id",me,str(p.title(),200,true),str(p.course(),200,true),str(p.description(),2000,false),p.platform(),p.url(),p.starts(),p.ends(),p.isPublic()).get("id")).longValue();invitations(id,me,p.participants());return detail(id,me);}
    @PutMapping("/{id}") @Transactional public Object update(Authentication auth,@PathVariable long id,@RequestBody Meeting p){validate(p);long me=a.user(auth);a.changed(a.db.update("update academic_meeting set title=?,course=?,description=?,platform=?,url=?,starts=?,ends=?,is_public=? where id=? and owner_id=?",str(p.title(),200,true),str(p.course(),200,true),str(p.description(),2000,false),p.platform(),p.url(),p.starts(),p.ends(),p.isPublic(),id,me));invitations(id,me,p.participants());return detail(id,me);}
    @PutMapping("/{id}/invitacion") public Object respond(Authentication auth,@PathVariable long id,@RequestBody Decision p){if(p.status()==null||!List.of("ACCEPTED","REJECTED").contains(p.status()))throw bad("Estado inválido");long me=a.user(auth);a.changed(a.db.update("update academic_invitation set status=? where meeting_id=? and user_id=?",p.status(),id,me));return detail(id,me);}
    @DeleteMapping("/{id}") public void delete(Authentication auth,@PathVariable long id){a.changed(a.db.update("delete from academic_meeting where id=? and owner_id=?",id,a.user(auth)));}
}
