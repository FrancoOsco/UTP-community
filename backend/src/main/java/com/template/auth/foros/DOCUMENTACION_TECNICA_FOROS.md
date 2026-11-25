# Módulo de foros: documentación técnica

Esta guía describe la implementación Java del módulo `com.template.auth.foros`: publicaciones, comentarios/respuestas, votos y moderación. Está dirigida a quienes mantengan o amplíen el backend.

## 1. Alcance y estructura

```text
com.template.auth.foros/
├── controller/   # Rutas REST y validación de entrada
├── dto/          # Contratos propios del módulo
├── entity/       # Entidades JPA
├── enums/        # Categorías y estados
├── repository/   # Persistencia y consultas Spring Data JPA
└── service/      # Reglas de negocio y coordinación
```

El módulo usa además tipos compartidos de `com.template.auth.dto` (por ejemplo, `RespuestaCrearRequest` y `VotoRequest`), la entidad `Usuario`, y el cliente de moderación ubicado en `com.template.auth.integration.moderation`.

Flujo general:

```text
ForoController → ForoService → repositories/JPA
                         ├── ModeracionService → ModerationApiClient → API Python
                         ├── ForoModeracionEstadoService
                         └── ComentarioModeracionEstadoService
```

El controlador traduce HTTP a llamadas de servicio. `ForoService` concentra las operaciones y las consultas de salida; los repositorios persisten entidades. La llamada al servicio Python y la decisión de moderación se coordinan desde `ModeracionService`.

## 2. API REST

La ruta base es `/api/foros`. Las operaciones que reciben `Authentication` usan `auth.getName()` como correo del usuario; `ForoService` lo resuelve con `UsuarioRepository`. La autorización general de estas rutas depende también de la configuración de seguridad de la aplicación.

| Método y ruta | Función | Entrada principal |
|---|---|---|
| `GET /api/foros/categorias` | Devuelve las categorías configuradas (`PREGUNTAS`, `PROFESORES`, `CURSOS`). | Ninguna |
| `GET /api/foros` | Lista publicaciones visibles, con paginación y filtros. | `categoria` opcional, `buscar` (por defecto vacío), `pagina` (por defecto `0`) |
| `GET /api/foros/{id}` | Devuelve el detalle de una publicación visible y sus comentarios visibles. | ID de publicación |
| `POST /api/foros` | Crea una publicación y la envía a moderación. Responde `201`. | `ForoCrearRequest` |
| `POST /api/foros/{id}/respuestas` | Crea y modera un comentario/respuesta. Responde `201`. | `{"contenido":"..."}` |
| `POST /api/foros/{id}/votos` | Crea, cambia o elimina el voto del usuario en el foro. | `{"valor":-1}`, `{"valor":0}` o `{"valor":1}` |
| `POST /api/foros/{id}/respuestas/{respuestaId}/votos` | Crea, cambia o elimina el voto sobre un comentario visible. | `{"valor":-1}`, `{"valor":0}` o `{"valor":1}` |

`ForoCrearRequest` valida `categoria` como obligatoria, `titulo` no vacío (8–150 caracteres) y `contenido` no vacío (15–5000 caracteres). El contenido del request se guarda en `Foro.descripcion`. `RespuestaCrearRequest` valida `contenido` no vacío (2–3000 caracteres). `VotoRequest` limita `valor` al rango `[-1, 1]`.

Las respuestas de publicaciones y comentarios se construyen actualmente como mapas, no como DTOs dedicados. La publicación expone `id`, `categoria`, `titulo`, `contenido`, `autor`, `autorId`, `creadoEn`, `respuestas`, `puntos` y `miVoto`. El detalle agrega `listaRespuestas`. Cada comentario expone `id`, `publicacionId`, `contenido`, `autor`, `autorId`, `creadoEn`, `puntos` y `miVoto`. El listado devuelve un `Page` de Spring Data.

Los errores de negocio usan `ResponseStatusException`; `GlobalExceptionHandler` los serializa como JSON con `status` y `message`. Los errores de validación de los DTOs se responden como `400`.

## 3. Modelo de datos

| Entidad | Tabla | Responsabilidad y relaciones |
|---|---|---|
| `Foro` | `foro` | Publicación con usuario autor, categoría, título, descripción, estados y fechas. |
| `ComentarioForo` | `comentario_foro` | Respuesta ligada a un foro y a un usuario; incluye `comentarioPadre` opcional para jerarquía, contenido, estados y fechas. |
| `ForoVoto` | `foro_votos` | Voto de un usuario sobre un foro. La entidad declara unicidad por foro y usuario. |
| `ComentarioVoto` | `comentario_votos` | Voto de un usuario sobre un comentario. La entidad declara unicidad por comentario y usuario. |
| `Moderacion` | `moderacion` | Auditoría de inferencia: foro o comentario asociado, nombre/versión del modelo, etiqueta, score, confianza, decisión y datos de revisión. |

Las asociaciones a `Usuario`, `Foro` y comentario usan carga `LAZY` donde está declarada. No se deben serializar entidades JPA directamente: el servicio arma las respuestas HTTP para controlar el contrato y evitar ciclos de relaciones.

`Moderacion` relaciona el registro con el contenido mediante IDs escalares (`foroId` o `comentarioId`); su validación de servicio requiere que exactamente uno de ellos esté presente.

## 4. Estados y visibilidad

Los campos de estado de `Foro` y `ComentarioForo` se almacenan como `String`, aunque existen enums equivalentes en `enums/`. Los valores usados por los servicios son:

| Decisión (`DecisionModeracion`) | `estadoModeracion` | `estadoPublicacion` | Visible en consultas públicas |
|---|---|---|---|
| `APROBADO` | `APROBADO` | `PUBLICADO` | Sí |
| `REQUIERE_REVISION` | `REQUIERE_REVISION` | `OCULTO` | No |
| `RECHAZADO` | `RECHAZADO` | `OCULTO` | No |
| `PENDIENTE` | `PENDIENTE` | `BORRADOR` | No |

La lista y el acceso al detalle de foro filtran publicaciones por `estadoPublicacion = PUBLICADO`. Para comentarios, las consultas públicas requieren simultáneamente `estadoPublicacion = PUBLICADO` y `estadoModeracion = APROBADO`. El contador de respuestas también cuenta solo comentarios con ambos estados. El voto de respuesta vuelve a comprobar esos estados; un comentario no visible se trata como no encontrado (`404`).

Los estados iniciales de `Foro` son `BORRADOR`/`PENDIENTE`. `ComentarioForo.@PrePersist` tiene como valor por defecto `PUBLICADO`/`PENDIENTE`, por lo que el flujo de creación establece explícitamente `BORRADOR`/`PENDIENTE` antes de persistir. Mantener este paso es esencial: evita que un comentario aparezca antes de terminar la moderación.

## 5. Creación y moderación

### Publicación

1. `ForoService.crear` resuelve el autor, normaliza con `trim()` título y contenido, y guarda el foro como `BORRADOR`/`PENDIENTE`.
2. Forma el texto de inferencia como `titulo + " [SEP] " + descripcion`.
3. Llama a `ModeracionService.evaluarForo`, que envía el texto al cliente Python y persiste la predicción en `moderacion`.
4. `ForoModeracionEstadoService` actualiza los estados. La decisión se aplica también al objeto de respuesta usado por `ForoService`.

### Comentario/respuesta

1. `ForoService.responder` exige que el foro padre esté publicado y resuelve al usuario.
2. Recorta el contenido con `trim()`, asigna `BORRADOR`/`PENDIENTE` explícitamente y guarda el comentario para obtener su ID.
3. Envía solamente `contenidoTexto` a `ModeracionService.evaluarComentario`; no concatena `[SEP]` para el comentario.
4. `ModeracionService` persiste la inferencia y su decisión. `ComentarioModeracionEstadoService` aplica los estados definitivos.
5. Si el cliente Python lanza `ModerationApiException`, el endpoint traduce el problema a `503 Service Unavailable`. El registro inicial permanece pendiente y, por tanto, fuera de las consultas públicas.

La creación de comentario no mantiene una transacción de servicio abierta alrededor de la llamada HTTP. Los cambios de estado se aplican en una transacción local aparte. Si el proceso falla después de persistir el contenido, este puede quedar como borrador pendiente; un mecanismo de reintento o revisión manual no está implementado por este módulo.

### Política de decisión

`ModeracionService` usa `ModerationResponse.score` para clasificar:

| Rango de `score` | Decisión |
|---|---|
| Menor que `0.40` | `APROBADO` |
| Desde `0.40` hasta menor que `0.90` | `REQUIERE_REVISION` |
| Desde `0.90` hasta `1.0` | `RECHAZADO` |

La respuesta del API de moderación también debe contener `label`, `probability`, `model_version` y `model_name`. El servicio valida presencia, rango de etiqueta/valores y nombres no vacíos antes de guardar. El significado exacto de `score` depende del contrato del servicio Python; antes de cambiar umbrales, contrastarlo con la documentación de `moderation-api` y los artefactos del modelo.

## 6. Persistencia y responsabilidades de clases

### Controlador

`ForoController` declara rutas, extrae el usuario autenticado cuando aplica, activa validación con `@Valid` y delega el comportamiento. No colocar lógica de persistencia o de inferencia aquí.

### Servicio de aplicación

`ForoService` orquesta listar, detalle, crear, responder y votar. Centraliza la validación de foro visible y crea los mapas de salida. Las operaciones de voto están dentro de `@Transactional`; las consultas de listado/detalle son de solo lectura. Las creaciones guardan antes de llamar al servicio externo para tener el ID que se registra en moderación.

`ModeracionService` adapta la respuesta de Python a la entidad de auditoría `Moderacion`, calcula la decisión y verifica la validez de la predicción.

`ForoModeracionEstadoService` y `ComentarioModeracionEstadoService` actualizan estado de publicación y moderación dentro de transacciones locales. Al añadir nuevas decisiones o estados, mantener ambos mapeos coherentes.

### Repositorios

- `ForoRepository.buscarPorCategoriaYTexto` pagina y filtra por publicación visible, categoría opcional y texto en título/descripción.
- `ComentarioForoRepository` ofrece consultas y conteos filtrados por ambos estados visibles, además del conteo de comentarios por usuario.
- `ForoVotoRepository` y `ComentarioVotoRepository` buscan el voto del usuario y calculan suma de valores.
- `ModeracionRepository` consulta el historial más reciente por ID de foro o comentario.

Los nombres de métodos derivados dependen de los nombres de propiedades Java y sus relaciones (por ejemplo, `Foro_ForoId`); cambiar una propiedad requiere revisar también dichos métodos.

## 7. Integración con el servicio Python

El paquete `com.template.auth.integration.moderation` contiene el cliente HTTP y sus DTOs/excepciones. `ModeracionService` llama `ModerationApiClient.predict(texto)`. La URL base, API Key y configuración del cliente viven fuera de `foros`, en la configuración de integración (`ModerationApiConfig` y `application.properties`). No poner secretos en este módulo ni en el repositorio; usar la configuración segura del entorno.

El formato de texto debe coincidir con el entrenamiento. Para foro se usa el separador literal `[SEP]` entre título y descripción; para comentario se envía únicamente el contenido. Si el preprocesamiento o contrato Python cambia, coordinar el cambio con `moderation-api` y sus pruebas/documentación.

## 8. Guía para extender el módulo

Antes de añadir una función:

1. Definir su contrato HTTP en `ForoController` y validar las entradas con DTOs y Jakarta Validation.
2. Implementar reglas y autorización contextual en `ForoService` o en un servicio específico; no confiar solo en que el frontend oculte acciones.
3. Para cualquier lectura pública, filtrar por los estados visibles. Revisar también búsquedas, conteos, votos, respuestas y relaciones anidadas para evitar filtraciones indirectas.
4. Si se cambia el ciclo de moderación, conservar el estado no visible durante la inferencia y actualizar el historial de `Moderacion` correctamente.
5. Crear métodos de repositorio acordes a los nombres reales de propiedades y respetar límites transaccionales, especialmente alrededor de llamadas HTTP.
6. Mantener estables las claves JSON que consume el frontend o versionar/documentar cualquier cambio de contrato.
7. Añadir pruebas unitarias e integración para decisiones aprobadas, revisión/rechazo, API Python no disponible, visibilidad, conteos, permisos y votos.

## 9. Consideraciones conocidas

- La entidad permite `comentarioPadre`, pero las rutas actuales no reciben un ID de comentario padre ni exponen un flujo de respuesta anidada.
- La respuesta de creación de comentario conserva el mapa de comentario existente y no incluye los estados de moderación. Si el cliente debe distinguir entre publicado y pendiente, acordar primero un DTO/contrato explícito.
- El conteo por autor (`countByUsuario_Id`) no filtra estados; el perfil puede contar comentarios no visibles. Si el producto requiere estadísticas solo públicas, crear una consulta específica con ambos estados.
- Los estados se persisten como cadenas. Los enums existentes documentan los valores, pero no están conectados por `@Enumerated` a los campos de las entidades. Evitar introducir valores escritos de forma inconsistente; una futura migración a enums debe coordinarse con el esquema y datos existentes.
- Las consultas públicas de foro filtran por `estadoPublicacion`; el módulo no verifica adicionalmente `estadoModeracion` en `ForoRepository`. La creación aplica los dos campos juntos, pero cualquier herramienta administrativa que modifique estados debe conservar esa correspondencia.

## 10. Archivos de referencia

- Controlador: `controller/ForoController.java`
- Orquestación: `service/ForoService.java`
- Inferencia y auditoría: `service/ModeracionService.java`, `entity/Moderacion.java`
- Estados: `service/ForoModeracionEstadoService.java`, `service/ComentarioModeracionEstadoService.java`
- Persistencia: `repository/`
- Entidades: `entity/`
- Categorías y estados: `enums/`
- Configuración del servicio Python: `com.template.auth.integration.moderation` y `backend/src/main/resources/application.properties`
