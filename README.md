# UTP Community · Prototipo web académico

**Tecnologías:** Astro 7 · Spring Boot 4.1.1 / Java 21 · PostgreSQL 15 · JWT. Proyecto elaborado sobre el ZIP original del equipo; conserva la base de autenticación, verificación de correo y recuperación de contraseñas.

## Alcance de este prototipo

**Implementado como código**
- Sitio de bienvenida web responsive, login y registro UTP.
- Registro y acceso **limitado a `@utp.edu.pe`**, normalización de correo y verificación mediante enlace enviado por SMTP antes del login.
- JWT de acceso, refresh token mediante cookie, logout, validaciones del backend y protección de API.
- Inicio, directorio de comunidad (navegación a categorías de foros) y perfil consultable/editable.
- Foros persistentes con categorías **PREGUNTAS, PROFESORES y CURSOS**. Permiten publicar, buscar, filtrar, responder, votar +1/-1 y retirar voto, y ver conteos.

**Representación visual / pendiente**
- El directorio de comunidad no crea ni administra grupos separados; dirige a categorías reales del foro.
- No se implementaron parking, SOS, horarios, repositorio de archivos, reuniones Zoom, notificaciones reales ni recomendaciones automatizadas. El ícono de campana es visual.
- Las tarjetas editoriales de la portada son **demostrativas**, no datos reales de alumnos.
- No está integrado con el SSO oficial ni con el padrón universitario. La exclusividad se verifica por **dominio y posesión del buzón**, no por una API oficial UTP.

## Requisitos

1. Java **21** + Maven compatible (o el wrapper `mvnw.cmd` incluido).
2. Node.js **22.12+**, Corepack / pnpm o npm.
3. Docker Desktop (para PostgreSQL / pgAdmin) o un PostgreSQL local.
4. Acceso real a SMTP para enviar correos de verificación, con credenciales válidas.

## Configuración local (Windows PowerShell)

### 1. Base de datos

```powershell
cd database
Copy-Item .env.example .env
# Edita .env y cambia las contraseñas de ejemplo
# Arranca Docker Desktop
 docker compose up -d
```

Por defecto: PostgreSQL `localhost:5432` y pgAdmin `http://localhost:5050`.

### 2. Backend

Configura las variables de entorno **en la terminal del proceso Java**:

```powershell
cd ..\backend
$env:DB_URL="jdbc:postgresql://localhost:5432/utp_community_db"
$env:DB_USER="alumno"
$env:DB_PASSWORD="TU_PASSWORD_DEL_ENV_DE_DOCKER"
$env:MAIL_USERNAME="tu-smtp@gmail.com"
$env:MAIL_PASSWORD="TU_CLAVE_SMTP"
$env:JWT_SECRET="UN_SECRETO_ALEATORIO_LARGO_DE_MINIMO_32_BYTES"
$env:FRONTEND_URL="http://localhost:4321"
$env:COOKIE_SECURE="false"
.\mvnw.cmd spring-boot:run
```

**IMPORTANTE:** El archivo `backend/.env.example` es una guía, Spring Boot no importa archivos `.env` automáticamente. No subas claves reales a GitHub. Usa un `JWT_SECRET` aleatorio de al menos 32 bytes, adecuado para HS256. Para enviar correo mediante Gmail normalmente se necesita contraseña de aplicación y cuenta configurada para SMTP. El esquema se genera en PostgreSQL por JPA (`ddl-auto=update`) para desarrollo, no para producción.

### 3. Frontend

En otra terminal:

```powershell
cd frontend
Copy-Item .env.example .env
corepack enable
corepack pnpm install
corepack pnpm dev
```

Accede a `http://localhost:4321`. Alternativa: `npm install` y `npm run dev` si pnpm no está disponible. Mantén **el mismo hostname** (`localhost`) y puertos en ambos procesos, porque el refresh requiere el origen definido por `FRONTEND_URL`.

### 4. Flujo de prueba manual

1. Abre `/register`. Prueba registrar `alumno@gmail.com`: debe ser rechazado.
2. Registra un correo `@utp.edu.pe` que realmente puedas recibir, con contraseña de al menos 8 caracteres, incluyendo mayúscula, minúscula, número y símbolo.
3. Abre el correo y visita el enlace `/verify-email?token=...`; solo entonces entra por `/login`.
4. Ve a `/perfil`, edita tu carrera, ciclo y biografía.
5. Abre `/foros/nuevo`, crea una pregunta con título y cuerpo adecuados; desde `/foro?id=...` responde (con otra cuenta) y vota.
6. Usa categorías, búsqueda y paginación en `/foros`.

## API agregada

Todas salvo login, registro, verificación y recuperación requieren `Authorization: Bearer <JWT>`.

| Verbo | Ruta | Acción |
|---|---|---|
| GET | `/api/me` | Mi identidad, perfil y conteos |
| PUT | `/api/me` | Editar nombre, apellido, carrera, ciclo y biografía |
| GET | `/api/foros/categorias` | Categorías disponibles |
| GET | `/api/foros?categoria=CURSOS&buscar=java&pagina=0` | Listar, filtrar, paginar |
| POST | `/api/foros` | Crear (categoria, titulo, contenido) |
| GET | `/api/foros/{id}` | Ver detalle y respuestas |
| POST | `/api/foros/{id}/respuestas` | Responder (contenido) |
| POST | `/api/foros/{id}/votos` | Votar (valor: -1, 0, 1) |
| POST | `/api/foros/{id}/respuestas/{respuestaId}/votos` | Votar una respuesta |

Un estudiante no puede votar sus propias publicaciones ni respuestas; voto `0` retira el voto previo. Los votos tienen unicidad por usuario y publicación/respuesta.

## Estructura

- `frontend/src/pages/`: rutas Astro; `dashboard`, `comunidad`, `foros`, `foros/nuevo`, `foro`, `perfil`, `login`, `register`.
- `frontend/src/components/AppShell.astro`: layout interno con menú, cabecera y barra lateral.
- `frontend/src/styles/app.css`: sistema visual responsive.
- `backend/.../entity/Foro*.java` y `RespuestaVoto.java`: modelo relacional.
- `backend/.../repository/`: repositorios JPA; consultas de filtro, votos y conteos.
- `backend/.../service/ForoService.java`: validaciones, contenido, respuestas, votos.
- `backend/.../controller/{ForoController,PerfilController}.java`: endpoints HTTP.
- `backend/.../validation/CorreoInstitucional.java`: política de dominio.

## Advertencias y decisiones

No se activó un modo de login falso ni se almacenaron contraseñas en texto plano. El prototipo depende de PostgreSQL y SMTP. Las publicaciones de ejemplo de la landing no aparecen como datos reales al entrar: los foros se alimentan de la base de datos. Cualquier despliegue real requiere auditoría de seguridad, migraciones versionadas, mayor moderación y controles contra abuso, HTTPS, pruebas integrales y aprobación institucional si se pretende usar identidad/marca UTP de forma oficial.
