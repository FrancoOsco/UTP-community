# UTP Community — actualización académica

Base: ZIP UTP-community-main(1).zip. Se conservan autenticación, foros, servicio de moderación Python y Docker Compose originales. No se requiere eliminar PostgreSQL ni recrear usuarios.

## Inicio
1. Levantar PostgreSQL como en el README original. Realizar copia de seguridad antes de iniciar.
2. Configurar las variables originales de Spring Boot (incluida MODERATION_API_KEY). Iniciar el backend Java 21 con el Maven Wrapper (`mvnw.cmd spring-boot:run` en Windows). Se crean las tablas `academic_*` automáticamente después de Hibernate; la cuenta de BD debe tener permiso de creación. No hay datos ficticios.
3. Iniciar el frontend con las dependencias del proyecto original (`pnpm install`, `pnpm dev`). Usar una versión de Node compatible con Astro del lockfile (al menos 22.23.4 para el lock de npm recibido; comprobar también la versión requerida por pnpm). No se cambiaron las versiones del archivo original.
4. Iniciar la API Python como en su documentación si se utilizará moderación de foros.

## Módulos
- Repositorio: proyectos de todas las carreras asociados al usuario autenticado, autor y portafolio, búsqueda por texto/carrera/curso, paginación, enlaces HTTPS, etiquetas, portada, archivos múltiples, principal, reemplazo y eliminación. Botón Descargar arriba y selector abajo; el ZIP completo agrupa archivos originales, no convierte formatos. Sin GitHub obligatorio.
- Reuniones: Meet/Zoom con enlaces externos, agenda, privacidad, invitaciones y aceptación/rechazo, aviso en campana, historial, exportación ICS. La plataforma no genera el enlace ni realiza la llamada.
- Calendario: cinco categorías, diez colores pastel, doble clic para crear, clic sobre evento para ver detalles, edición, eliminación, varios eventos por día y reuniones aceptadas opcionales. Privado por usuario. En móviles se usa + y desplazamiento horizontal de la cuadrícula.
- Herramientas: catálogo de fuentes oficiales, recomendaciones por extensión y requisitos declarados (`id@versión`), BAT Windows con WinGet para paquetes permitidos y páginas oficiales para el resto. No se ejecuta código del estudiante. Las versiones declaradas son informativas; el asistente no garantiza resolver versiones exactas. GitHub opcional; VS Code protocolo de clonación y BAT de apertura local/IntelliJ. Extraer ZIP antes de abrirlo. No incluye binarios ni licencias de terceros.

## Almacenamiento y seguridad
`STORAGE_ROOT` permite establecer una ubicación absoluta persistente (por defecto `./storage`, relativa al directorio de ejecución del backend). Respaldar esta carpeta junto con PostgreSQL; si se conteneriza el backend, montar un volumen persistente. 100 MiB por archivo, 500 MiB y 50 archivos por proyecto. Los archivos se guardan con identificadores aleatorios y se descargan como adjuntos. La validación de extensión no sustituye un antivirus. No publicar secretos ni archivos privados. El catálogo es controlado en código, no acepta comandos de usuarios. Las opciones de WinGet requieren comprobación en Windows y acceso a internet.

## Verificación pendiente en el entorno real
- Crear dos cuentas UTP verificadas. Probar publicación, descarga cruzada, permisos de edición y eliminación, reemplazo y portada, reinicio del backend conservando STORAGE_ROOT.
- Crear una reunión privada, invitar a otra cuenta, aceptar/rechazar y verificar que una tercera cuenta no puede consultar el enlace. Probar reuniones públicas, historial e ICS.
- Crear actividades en cada categoría, varios colores, fechas distintas y comprobar aislamiento por usuario. Probar nombres largos y dispositivos móviles.
- Probar descargas BAT, WinGet, editor y GitHub en Windows; respetar Workspace Trust y licencias.
- Confirmar que login y foros/moderación originales siguen funcionando.

## Límites de verificación de esta entrega
Se revisó el código estáticamente y se comprobó TypeScript con el compilador disponible. No se pudo compilar Spring Boot ni construir Astro en este entorno: el Maven Wrapper no pudo descargar Maven y npm no pudo descargar las dependencias; Node local 22.16.0 no satisface el Astro original del lockfile. No se ejecutó PostgreSQL, navegador ni Windows. Las funcionalidades de salas de estudio y archivos colaborativos por grupo quedan fuera de esta actualización. No se completa la integración pendiente de moderación de foros; se conserva el módulo original.
