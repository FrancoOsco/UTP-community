# Documentación técnica del módulo de foros

Esta guía describe la implementación del paquete `com.template.auth.foros` y sus integraciones. Está orientada a desarrolladores que mantengan o amplíen el backend.

## 1. Estructura y responsabilidades

```text
com.template.auth.foros/
├── controller/   # Endpoints REST y validación HTTP
├── dto/          # Contratos de entrada/salida
├── entity/       # Entidades JPA
├── enums/        # Categorías, estados y decisiones
├── repository/   # Consultas Spring Data y SQL específico
└── service/      # Reglas y orquestación de casos de uso
```

El paquete utiliza además `Usuario`, `UsuarioRepository`, DTOs comunes (`RespuestaCrearRequest`, `VotoRequest`) y `com.template.auth.integration.moderation` para llamar a FastAPI.

```text
ForoController ──> ForoService ──> repositorios
                         ├──> ModeracionService ──> ModerationApiClient ──> Python
                         └──> ModeracionWorkflowService
                                      ├──> servicios de estado foro/comentario
                                      └──> NotificacionService

ModeracionController ──> ModeracionService ──> ModeracionRepository
NotificacionController ──> NotificacionService ──> NotificacionRepository
```

Los controladores no deben devolver entidades JPA. Moderación y notificaciones usan DTOs para limitar campos expuestos y evitar problemas de relaciones lazy/ciclos de serialización.

## 2. Modelo de persistencia

| Entidad | Tabla | Función |
|---|---|---|
| `Foro` | `foro` | Publicación, autor, categoría, título, descripción, estados y fechas. |
| `ComentarioForo` | `comentario_foro` | Respuesta asociada a foro y autor. Admite una relación `comentarioPadre`, aunque la API actual no implementa respuestas anidadas. |
| `ForoVoto` | `foro_votos` | Un voto por usuario y foro (restricción única). |
| `ComentarioVoto` | `comentario_votos` | Un voto por usuario y comentario (restricción única). |
| `Moderacion` | `moderacion` | Predicción, decisión vigente, asociación a foro o comentario, asignación temporal y auditoría de resolución humana. |
| `Notificacion` | `notificacion` | Aviso por usuario sobre una moderación que requiere revisión. Restricción única por usuario y moderación. |

`Moderacion` debe referirse a exactamente un objetivo: `foroId` o `comentarioId`. Guarda `etiquetaPredicha`, `moderationScore`, `predictionConfidence`, nombre/versión del modelo y `decision`. En revisión humana, `decision` se sobrescribe con la decisión final; `etiquetaConfirmada`, `revisadoPor`, `fechaRevision` y `motivoDecision` guardan los datos de la resolución.

Los campos `atendidoPor`, `fechaAsignacion` y `fechaExpiracionAsignacion` representan una reserva activa. Al liberar o resolver se limpian. La persona y fecha de resolución permanecen en `revisadoPor` y `fechaRevision`; la fecha en que se tomó la reserva no se conserva después de resolverla.

## 3. Estados, decisiones y visibilidad

Los estados de publicación/moderación de `Foro` y `ComentarioForo` están mapeados como `String`; evitar valores distintos de los enums existentes.

| Decisión vigente | Estado de moderación del contenido | Estado de publicación | Público |
|---|---|---|---|
| `APROBADO` | `APROBADO` | `PUBLICADO` | Sí |
| `REQUIERE_REVISION` | `REQUIERE_REVISION` | `OCULTO` | No |
| `RECHAZADO` | `RECHAZADO` | `OCULTO` | No |
| `PENDIENTE` | `PENDIENTE` | `BORRADOR` | No |

`ForoRepository.buscarPorCategoriaYTexto` y la validación de acceso a detalle/voto filtran foros por `estadoPublicacion = PUBLICADO`. Las respuestas y sus conteos públicos requieren `estadoPublicacion = PUBLICADO` **y** `estadoModeracion = APROBADO`. Los votos de respuestas aplican el mismo filtro; los comentarios no visibles se tratan como no encontrados.

Los enums `EstadoPublicacion`, `EstadoModeracion` y `DecisionModeracion` documentan los valores. Las entidades guardan los dos primeros como texto, no como `@Enumerated`.

## 4. Flujo de creación y moderación automática

### Foro

1. `ForoService.crear` valida/recorta la entrada y guarda primero el foro como `BORRADOR`/`PENDIENTE` para obtener su ID.
2. Prepara el texto como `titulo + " [SEP] " + descripcion` y solicita predicción a `ModeracionService.predecir`.
3. `ModeracionWorkflowService.procesarForo` registra la predicción, calcula la decisión, actualiza el estado del foro y crea notificaciones si requiere revisión.
4. `ForoService.crear` devuelve el mapa de publicación existente. Este mapa no incluye el estado de moderación.

**Nota de mantenimiento:** `ForoService.crear` todavía vuelve a invocar la actualización de estado y la notificación después de `procesarForo`. El workflow ya realizó esas operaciones; antes de reorganizar el flujo, conservar el estado transaccional y la notificación única.

### Comentario

1. `ForoService.responder` exige que el foro esté publicado y guarda el comentario explícitamente como `BORRADOR`/`PENDIENTE`.
2. Envía únicamente `contenidoTexto` a Python; no agrega `[SEP]`.
3. `ModeracionWorkflowService.procesarComentario` guarda la predicción, actualiza estado y crea notificaciones cuando corresponde.
4. Devuelve el mapa de comentario existente, sin campos de estado.

Las llamadas HTTP a Python ocurren antes de la transacción local del workflow. Si falla la inferencia, el contenido ya guardado permanece oculto/pending; no existe una operación automática de reintento en este módulo. Para comentarios, `ModerationApiException` se convierte en `503`; verificar el manejo de errores al modificar el flujo de foro.

### Umbrales

`ModeracionService` usa `ModerationResponse.score`:

| Score | Decisión automática |
|---|---|
| `< 0.40` | `APROBADO` |
| `>= 0.40` y `< 0.90` | `REQUIERE_REVISION` |
| `>= 0.90` | `RECHAZADO` |

La respuesta Python requiere `label`, `probability`, `score`, `model_version` y `model_name`; el servicio comprueba presencia y rangos. Mantener el preprocesamiento de acuerdo con los artefactos y documentación de `moderation-api`.

## 5. API de foros

Base: `/api/foros`. Todas las rutas requieren autenticación según `SecurityConfig`. Las operaciones que reciben `Authentication` usan `auth.getName()` como email del usuario.

| Método y ruta | Entrada | Resultado |
|---|---|---|
| `GET /api/foros/categorias` | — | Lista de `{id,nombre}` para `PREGUNTAS`, `PROFESORES`, `CURSOS`. |
| `GET /api/foros?categoria=&buscar=&pagina=0` | Filtros opcionales | `Page` de foros publicados; tamaño 10, orden descendente por creación. |
| `GET /api/foros/{id}` | ID | Foro publicado y `listaRespuestas` aprobadas/publicadas. |
| `POST /api/foros` | `ForoCrearRequest` | `201`; crea y modera un foro. |
| `POST /api/foros/{id}/respuestas` | `{"contenido":"..."}` | `201`; crea y modera una respuesta. |
| `POST /api/foros/{id}/votos` | `{"valor":-1|0|1}` | Puntos agregados y voto propio. `0` elimina el voto. |
| `POST /api/foros/{id}/respuestas/{respuestaId}/votos` | `{"valor":-1|0|1}` | Igual para una respuesta visible que pertenezca al foro indicado. |

Validaciones: foro: categoría obligatoria, título 8–150 caracteres, contenido 15–5000; respuesta: contenido 2–3000; voto: entero de -1 a 1. El servicio recorta título y contenido. El listado devuelve la estructura paginada de Spring Data (`content`, `totalElements`, `totalPages`, `number`, etc.).

Los mapas de foro tienen `id`, `categoria`, `titulo`, `contenido`, `autor`, `autorId`, `creadoEn`, `respuestas`, `puntos` y `miVoto`. El detalle añade `listaRespuestas`. Cada respuesta tiene `id`, `publicacionId`, `contenido`, `autor`, `autorId`, `creadoEn`, `puntos` y `miVoto`. Los contratos de creación no indican si el contenido quedó oculto por moderación: el cliente no debe inferir que se publicó solo por recibir `201`.

## 6. API de moderación humana

Base: `/api/moderacion`. `SecurityConfig` limita el prefijo a `MODERADOR` o `ADMIN`; `ModeracionService` resuelve el email autenticado y vuelve a comprobar esos roles.

| Método y ruta | Comportamiento |
|---|---|
| `GET /api/moderacion/bandeja` | Últimas moderaciones vigentes en `REQUIERE_REVISION`; incluye disponibles, asignadas y vencidas. |
| `GET /api/moderacion/mis-revisiones` | Reservas activas del usuario autenticado, no vencidas. |
| `POST /api/moderacion/revisiones/{moderacionId}/tomar` | Reserva atómicamente la revisión durante 30 minutos. |
| `DELETE /api/moderacion/revisiones/{moderacionId}/reserva` | Libera la reserva propia; `204`. |
| `PATCH /api/moderacion/revisiones/{moderacionId}/decision` | Resuelve la revisión como `APROBADO` o `RECHAZADO`. |

El DTO de bandeja incluye ID/tipo/contenido/autor, predicción y datos de reserva. La respuesta de tomar es `{ "mensaje": "...", "revision": { ... } }`. La resolución requiere:

```json
{
  "decision": "APROBADO",
  "motivo": "Contenido revisado y permitido."
}
```

`decision` solo admite `APROBADO` o `RECHAZADO`; `motivo` es opcional y acepta hasta 2000 caracteres. Resolver exige ser dueño de una reserva vigente y que el registro aún sea la moderación más reciente del contenido. La actualización condicional devuelve `409` si la revisión venció, cambió de dueño, ya se resolvió o dejó de ser vigente.

Al resolver, `Moderacion.decision` se sobrescribe con la decisión humana. Se persisten `etiquetaConfirmada` (0 para aprobado, 1 para rechazado), `revisadoPor`, `fechaRevision` y `motivoDecision`. Se actualiza el contenido en la misma transacción: aprobado => publicado; rechazado => oculto. La reserva se limpia y la revisión desaparece de las consultas activas.

Errores de servicio previstos: `404` si no existe el ID, `403` si el usuario no tiene rol, `409` si no se puede tomar/liberar/resolver por el estado o dueño de la reserva, `400` si el cuerpo no cumple contrato. Las respuestas de seguridad pueden no compartir el mismo cuerpo JSON que `GlobalExceptionHandler`.

## 7. API de notificaciones

Base: `/api/notificaciones`. El acceso está limitado a roles `MODERADOR` y `ADMIN`; las consultas se filtran por el usuario autenticado.

| Método y ruta | Resultado |
|---|---|
| `GET /api/notificaciones` | Notificaciones propias, más recientes primero. |
| `GET /api/notificaciones/no-leidas` | Solo notificaciones propias no leídas. |
| `GET /api/notificaciones/no-leidas/count` | Número JSON, por ejemplo `3` (no un objeto envolvente). |
| `PATCH /api/notificaciones/{id}/leer` | Marca una notificación propia como leída; `204`. Repetirla es idempotente. |

La respuesta de cada notificación incluye `notificacionId`, `moderacionId`, `tipoContenido`, `contenidoId`, `mensaje`, `leida`, `fechaCreacion` y `fechaLectura`. Se genera una notificación por cada usuario con rol `MODERADOR` o `ADMIN` cuando una decisión automática requiere revisión. La restricción única evita duplicar avisos por usuario y moderación. Intentar marcar una notificación ajena se responde como `404`.

## 8. Persistencia, transacciones y extensibilidad

- `ModeracionWorkflowService` agrupa el registro de una predicción, el cambio de estado del contenido y la generación de notificación en una transacción local. La llamada Python debe permanecer fuera de esa transacción.
- Las reservas usan SQL condicional y actualización atómica en `ModeracionRepository`; no sustituirlas por un ciclo leer-modificar-guardar sin bloqueo/condición equivalente.
- Resolver combina actualización condicional de moderación y cambio de estado de foro/comentario en una transacción, para no dejar una decisión final desacoplada de la visibilidad.
- Las notificaciones tienen métodos de lectura/escritura separados y una actualización condicionada por destinatario. Mantener el filtro por usuario para evitar IDOR.
- Al añadir rutas, validar entrada en DTOs, revisar `SecurityConfig`, no devolver entidades y cubrir permisos, estados ocultos, expiración y concurrencia.
- Antes de cambiar enums, nombres de propiedades o columnas, revisar JPQL, métodos derivados y SQL nativo del repositorio.

## 9. Referencias de código

- API: `controller/ForoController.java`, `controller/ModeracionController.java`, `controller/NotificacionController.java`
- Orquestación: `service/ForoService.java`, `service/ModeracionService.java`, `service/ModeracionWorkflowService.java`, `service/NotificacionService.java`
- Estados: `service/ForoModeracionEstadoService.java`, `service/ComentarioModeracionEstadoService.java`
- Entidades y DTOs: `entity/`, `dto/`
- Persistencia: `repository/`
- Autorización: `backend/src/main/java/com/template/auth/config/SecurityConfig.java`
- Integración Python: `com.template.auth.integration.moderation` y `backend/src/main/resources/application.properties`
