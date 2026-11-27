package com.template.auth.academic;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import static com.template.auth.academic.AcademicDb.*;

@RestController @RequestMapping("/api/herramientas")
public class ToolsController {
    record Tool(String id,String name,String category,String license,String url,String winget,List<String> extensions){}
    static Tool t(String id,String name,String cat,String lic,String url,String pkg,String ext){return new Tool(id,name,cat,lic,url,pkg,ext.isEmpty()?List.of():List.of(ext.split(" ")));}
    static final List<Tool> TOOLS=List.of(
      t("excel","Microsoft Excel","Ofimática","Licencia o versión web","https://www.microsoft365.com/","","xlsx xls csv"),
      t("word","Microsoft Word","Ofimática","Licencia o versión web","https://www.microsoft365.com/","","doc docx"),
      t("powerpoint","Microsoft PowerPoint","Ofimática","Licencia o versión web","https://www.microsoft365.com/","","ppt pptx"),
      t("powerbi","Power BI Desktop","Datos y negocios","Gratuito para escritorio","https://learn.microsoft.com/es-es/power-bi/fundamentals/desktop-get-the-desktop","Microsoft.PowerBI","pbix pbit"),
      t("bizagi","Bizagi Modeler","Datos y negocios","Descarga gratuita; puede requerir cuenta","https://www.bizagi.com/en/platform/modeler","","bpm bpmn"),
      t("pdf","Lector PDF / navegador","Documentos","Gratuito; navegador suficiente","https://get.adobe.com/reader/","","pdf"),
      t("libreoffice","LibreOffice","Ofimática","Gratuito","https://www.libreoffice.org/download/download-libreoffice/","TheDocumentFoundation.LibreOffice",""),
      t("autocad","AutoCAD","Arquitectura y diseño","Licencia educativa o comercial","https://www.autodesk.com/education/edu-software/overview","","dwg dxf"),
      t("revit","Revit","Arquitectura y diseño","Licencia educativa o comercial","https://www.autodesk.com/education/edu-software/overview","","rvt"),
      t("sketchup","SketchUp","Arquitectura y diseño","Según licencia","https://www.sketchup.com/","","skp"),
      t("blender","Blender","Arquitectura y diseño","Gratuito","https://www.blender.org/download/","BlenderFoundation.Blender","blend stl"),
      t("photoshop","Adobe Photoshop","Arquitectura y diseño","Licencia","https://www.adobe.com/products/photoshop.html","","psd"),
      t("spss","IBM SPSS","Investigación","Licencia","https://www.ibm.com/products/spss-statistics","","sav sps"),
      t("matlab","MATLAB","Investigación","Licencia","https://www.mathworks.com/products/matlab.html","","m mlx"),
      t("drawio","draw.io","Diagramas","Gratuito","https://www.drawio.com/","","drawio"),
      t("zip","7-Zip","Utilidades","Gratuito; ZIP también en Windows","https://www.7-zip.org/","7zip.7zip","zip rar 7z"),
      t("vscode","Visual Studio Code","Programación","Gratuito","https://code.visualstudio.com/download","Microsoft.VisualStudioCode","java py js ts html css sql json yaml yml md c cpp cs go kt php r"),
      t("idea","IntelliJ IDEA","Programación","Según edición y funciones","https://www.jetbrains.com/idea/download/","","java kt"),
      t("git","Git","Programación","Gratuito","https://git-scm.com/downloads","Git.Git",""),
      t("java21","Java 21 (Temurin)","Programación","Gratuito","https://adoptium.net/","EclipseAdoptium.Temurin.21.JDK","java"),
      t("node22","Node.js 22 LTS","Programación","Gratuito","https://nodejs.org/en/download","","js ts"),
      t("python","Python 3.12","Programación","Gratuito","https://www.python.org/downloads/","Python.Python.3.12","py ipynb"),
      t("pnpm","pnpm","Programación","Gratuito; requiere Node","https://pnpm.io/installation","",""),
      t("maven","Maven","Programación","Gratuito","https://maven.apache.org/download.cgi","",""),
      t("gradle","Gradle","Programación","Gratuito","https://gradle.org/install/","",""),
      t("docker","Docker Desktop","Programación","Según condiciones de licencia","https://www.docker.com/products/docker-desktop/","",""),
      t("postgres","PostgreSQL","Datos y negocios","Gratuito","https://www.postgresql.org/download/","",""),
      t("mysql","MySQL","Datos y negocios","Community gratuita","https://dev.mysql.com/downloads/","",""),
      t("dbeaver","DBeaver Community","Datos y negocios","Gratuito","https://dbeaver.io/download/","dbeaver.dbeaver","sql"),
      t("jupyter","Jupyter","Investigación","Gratuito","https://jupyter.org/install","","ipynb"));
    @GetMapping public List<Tool> list(){return TOOLS;}
    record Setup(List<String> tools){}
    @PostMapping("/setup") public ResponseEntity<byte[]> setup(@RequestBody Setup req){if(req.tools()==null||req.tools().size()>TOOLS.size())throw bad("Selección inválida");Set<String> ids=new HashSet<>(req.tools());if(ids.stream().anyMatch(id->TOOLS.stream().noneMatch(t->t.id().equals(id))))throw bad("Herramienta desconocida");StringBuilder b=new StringBuilder("@echo off\r\nsetlocal DisableDelayedExpansion\r\ntitle UTP Community Setup\r\necho UTP Community - herramientas academicas\r\necho No se ejecutara codigo del proyecto.\r\nwhere winget >nul 2>&1\r\nif errorlevel 1 (\r\n echo WinGet no disponible. Se ofrecera la pagina oficial.\r\n set NO_WINGET=1\r\n)\r\n");for(Tool t:TOOLS){if(!ids.contains(t.id()))continue;b.append("\r\necho.\r\necho ").append(t.id()).append("\r\n");if(!t.winget().isBlank()){b.append("if defined NO_WINGET goto web_").append(t.id()).append("\r\nwinget list --id ").append(t.winget()).append(" -e --accept-source-agreements >nul 2>&1\r\nif not errorlevel 1 (\r\n echo Ya instalado segun WinGet.\r\n goto end_").append(t.id()).append("\r\n)\r\nchoice /C SN /M \"Instalar ").append(t.id()).append(" desde WinGet\"\r\nif errorlevel 2 goto end_").append(t.id()).append("\r\nwinget install --id ").append(t.winget()).append(" -e\r\nif not errorlevel 1 goto end_").append(t.id()).append("\r\n:web_").append(t.id()).append("\r\n");}b.append("choice /C SN /M \"Abrir pagina oficial de ").append(t.id()).append(" (si falta o necesita licencia)\"\r\nif errorlevel 2 goto end_").append(t.id()).append("\r\nstart \"\" \"").append(t.url()).append("\"\r\n:end_").append(t.id()).append("\r\n");}b.append("\r\necho Revisa las versiones requeridas y reinicia si el fabricante lo solicita.\r\npause\r\n");return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=UTP-Community-Setup.bat").contentType(MediaType.APPLICATION_OCTET_STREAM).body(b.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII));}

    record Editor(String url){}
    @PostMapping("/editor") public ResponseEntity<byte[]> editor(@RequestBody Editor req){
        String url=req.url()==null?"":req.url().strip();
        if(!url.isEmpty()&&!url.matches("https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/?"))throw bad("Repositorio GitHub inválido");
        String script="@echo off\r\nsetlocal DisableDelayedExpansion\r\ntitle UTP Community - Abrir proyecto\r\necho No se ejecutara el codigo del proyecto.\r\n";
        if(!url.isEmpty())script+="echo Puedes clonar el repositorio en una carpeta nueva.\r\nchoice /C SN /M \"Clonar desde GitHub\"\r\nif errorlevel 2 goto local\r\nwhere git >nul 2>&1\r\nif errorlevel 1 (\r\n echo Instala Git desde el asistente academico.\r\n pause\r\n exit /b 1\r\n)\r\npowershell -NoProfile -Command \"Add-Type -AssemblyName System.Windows.Forms; $d=New-Object System.Windows.Forms.FolderBrowserDialog; $d.SelectedPath=[Environment]::GetFolderPath('Desktop'); if($d.ShowDialog() -eq 'OK'){ $target=Join-Path $d.SelectedPath ('UTP-Proyecto-'+[guid]::NewGuid().ToString('N').Substring(0,8)); git clone -- '"+url+"' $target; if($LASTEXITCODE -eq 0){Write-Host ('Proyecto guardado en: '+$target)}else{exit 1}}\"\r\n";
        script+=":local\r\necho Selecciona la carpeta del proyecto (extrae el ZIP antes si corresponde).\r\nchoice /C VI /M \"V = VS Code, I = IntelliJ IDEA\"\r\nif errorlevel 2 goto idea\r\nwhere code >nul 2>&1\r\nif errorlevel 1 (\r\n echo VS Code no encontrado en PATH. Utiliza el asistente de instalacion.\r\n pause\r\n exit /b 1\r\n)\r\npowershell -NoProfile -Command \"Add-Type -AssemblyName System.Windows.Forms; $d=New-Object System.Windows.Forms.FolderBrowserDialog; $d.SelectedPath=[Environment]::GetFolderPath('Desktop'); if($d.ShowDialog() -eq 'OK'){code $d.SelectedPath}\"\r\ngoto end\r\n:idea\r\npowershell -NoProfile -Command \"Add-Type -AssemblyName System.Windows.Forms; $d=New-Object System.Windows.Forms.FolderBrowserDialog; $d.SelectedPath=[Environment]::GetFolderPath('Desktop'); if($d.ShowDialog() -ne 'OK'){exit}; $cmd=Get-Command idea64.exe,idea -ErrorAction SilentlyContinue | Select-Object -First 1; if($cmd){Start-Process -FilePath $cmd.Source -ArgumentList ([char]34+$d.SelectedPath+[char]34)}else{ $f=New-Object System.Windows.Forms.OpenFileDialog; $f.Filter='IntelliJ IDEA (idea64.exe)|idea64.exe'; if($f.ShowDialog() -eq 'OK'){Start-Process -FilePath $f.FileName -ArgumentList ([char]34+$d.SelectedPath+[char]34)} }\"\r\n:end\r\npause\r\n";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=Abrir-proyecto-UTP.bat").contentType(MediaType.APPLICATION_OCTET_STREAM).body(script.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }
}
