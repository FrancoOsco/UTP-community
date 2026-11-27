package com.template.auth.academic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;
import static com.template.auth.academic.AcademicDb.*;

@Service
public class StorageService {
    private final Path root;
    public static final long MAX_FILE=100L*1024*1024, MAX_PROJECT=500L*1024*1024;
    public static final Set<String> EXT=Set.of("pdf","doc","docx","xls","xlsx","csv","ppt","pptx","txt","zip","rar","7z","png","jpg","jpeg","webp","dwg","dxf","stl","rvt","skp","blend","sav","sps","m","mlx","psd","fig","drawio","bpm","bpmn","pbix","pbit","java","py","sql","html","css","js","ts","json","xml","ipynb","mp3","wav","mp4","md","svg","c","cpp","cs","r","go","kt","php","yaml","yml");
    public StorageService(@Value("${app.storage.root:./storage}") String path){root=Path.of(path).toAbsolutePath().normalize();}
    static String name(MultipartFile file){String n=file.getOriginalFilename();if(n==null||n.isBlank()||n.length()>255||n.contains("/")||n.contains("\\")||n.contains(":" )||n.endsWith(".")||n.endsWith(" ")||n.matches("(?i)^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?$")||n.chars().anyMatch(c->"<>|?*\"".indexOf(c)>=0)||n.chars().anyMatch(c->c<32||c==127)||n.equals(".")||n.equals(".."))throw bad("Nombre de archivo inválido");String ext=extension(n);if(!EXT.contains(ext)||file.isEmpty()||file.getSize()>MAX_FILE)throw bad("Formato no permitido o archivo superior a 100 MB");return n;}
    static String extension(String n){int i=n.lastIndexOf('.');return i<0?"":n.substring(i+1).toLowerCase(Locale.ROOT);}
    public Path path(String key){if(!key.matches("[0-9a-f-]{36}"))throw new IllegalArgumentException("Clave inválida");return root.resolve(key);}
    public String save(MultipartFile file) throws IOException {String n=name(file);Files.createDirectories(root);String key=UUID.randomUUID().toString();Path target=path(key);try(InputStream in=file.getInputStream()){Files.copy(in,target);}catch(IOException e){remove(key);throw e;}try{if(Set.of("png","jpg","jpeg","webp").contains(extension(n))){try(InputStream in=Files.newInputStream(target)){byte[] h=in.readNBytes(12);String ext=extension(n);boolean valid=h.length>=12&&switch(ext){case "png"->(h[0]&255)==137&&h[1]==80&&h[2]==78&&h[3]==71&&h[4]==13&&h[5]==10&&h[6]==26&&h[7]==10;case "jpg","jpeg"->(h[0]&255)==255&&(h[1]&255)==216&&(h[2]&255)==255;case "webp"->new String(h,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(h,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");default->false;};if(!valid)throw bad("Imagen inválida");}}if(Set.of("zip","docx","xlsx","pptx").contains(extension(n))){try(ZipFile z=new ZipFile(target.toFile())){if(z.size()==0)throw bad("Archivo comprimido vacío");}}}catch(IOException|RuntimeException ex){remove(key);if(ex instanceof IOException)throw bad("El archivo comprimido no es válido");throw ex;}if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCompletion(int status){if(status!=STATUS_COMMITTED)remove(key);}});return key;}
    public void deleteUserFilesAfterCommit(org.springframework.jdbc.core.JdbcTemplate db,long userId){for(var f:db.queryForList("select f.storage_key from academic_file f join academic_project p on p.id=f.project_id where p.owner_id=?",userId))deleteAfterCommit((String)f.get("storage_key"));}
    public void deleteAfterCommit(String key){if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCommit(){remove(key);}});else remove(key);}
    private void remove(String key){try{Files.deleteIfExists(path(key));}catch(IOException e){System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING,"No se pudo limpiar un archivo",e);}}
}
