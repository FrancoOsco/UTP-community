# Integración frontend con foros, moderación y notificaciones

Guía de consumo de los endpoints implementados por `com.template.auth.foros`. Describe contratos observados en los controladores/servicios actuales; el cliente debe tratar las claves JSON documentadas como el contrato público y no depender de entidades JPA.

## 1. URL base y autenticación

En desarrollo, la base suele ser `http://localhost:8080`; reemplázala por la URL configurada para cada entorno. Todas las rutas mostradas requieren JWT. Enviar:

```http
Authorization: Bearer <token>
Accept: application/json
Content-Type: application/json
```

`Content-Type` es necesario en requests con cuerpo. El login es `POST /api/auth/login`; su respuesta contiene `token`. El token representa al usuario por email y debe estar vigente/verificado.

Roles:

- Rutas `/api/foros/**`: usuario autenticado.
- Rutas `/api/moderacion/**` y `/api/notificaciones/**`: rol `MODERADOR` o `ADMIN`.

Una cuenta registrada con rol normal no puede usar las pantallas de moderación. Manejar `401` como sesión no válida/no autenticada y `403` como permisos insuficientes. Las respuestas de seguridad pueden llegar sin cuerpo JSON.

## 2. Foros

### Categorías

```http
GET /api/foros/categorias
```

Devuelve opciones con `id` para enviar y `nombre` para mostrar:

```json
[
  { "id": "PREGUNTAS", "nombre": "Preguntas y respuestas" },
  { "id": "PROFESORES", "nombre": "Profesores" },
  { "id": "CURSOS", "nombre": "Cursos" }
]
```

### Listado y filtros

```http
GET /api/foros?categoria=PREGUNTAS&buscar=algoritmos&pagina=0
```

Todos los parámetros son opcionales. `categoria` acepta `PREGUNTAS`, `PROFESORES` o `CURSOS`; `buscar` busca en título/descripción; `pagina` empieza en cero. El tamaño de página es 10 y el orden es descendente por fecha de creación.

La respuesta es una página de Spring Data. Su forma incluye `content` (la lista de foros) y metadatos como `totalElements`, `totalPages`, `number`, `size` y `first`/`last`.

Cada elemento de `content` contiene:

```json
{
  "id": 15,
  "categoria": "PREGUNTAS",
  "titulo": "Ayuda con algoritmos",
  "contenido": "Descripción de la pregunta",
  "autor": "Nombre Apellido",
  "autorId": 1,
  "creadoEn": "2026-11-26T04:30:23Z",
  "respuestas": 2,
  "puntos": 4,
  "miVoto": 1
}
```

El backend lista únicamente foros publicados. `miVoto` es el voto del usuario actual (`-1`, `0` sin voto o `1`).

### Crear foro

```http
POST /api/foros
```

```json
{
  "categoria": "PREGUNTAS",
  "titulo": "Ayuda con algoritmos",
  "contenido": "¿Cómo puedo resolver este ejercicio?"
}
```

Reglas: título 8–150 caracteres, contenido 15–5000; ambos no vacíos. La respuesta es `201 Created` y usa la misma forma de foro del listado.

El backend evalúa el texto con moderación automática. La respuesta actual no expone la decisión ni los estados: **`201` confirma que se creó el registro, no que sea visible para todos**. Si queda pendiente de revisión o rechazado, el foro no aparecerá en el listado público. No agregues automáticamente la respuesta al feed público solo por haber recibido `201`.

### Detalle de foro

```http
GET /api/foros/{id}
```

Devuelve los campos del foro más `listaRespuestas`. Cada elemento de esa lista contiene `id`, `publicacionId`, `contenido`, `autor`, `autorId`, `creadoEn`, `puntos` y `miVoto`. Solo se incluyen comentarios publicados y aprobados. Un foro no visible se trata como inexistente (`404`).

### Crear comentario/respuesta

```http
POST /api/foros/{id}/respuestas
```

```json
{
  "contenido": "Gracias, esta explicación me ayudó."
}
```

Reglas: contenido no vacío, 2–3000 caracteres. La respuesta es `201 Created` y contiene los campos de un comentario (`id`, `publicacionId`, `contenido`, `autor`, `autorId`, `creadoEn`, `puntos`, `miVoto`). El comentario pasa por moderación y el cuerpo no incluye el estado resultante. Por eso, presenta el resultado como **enviado** y vuelve a consultar el detalle para comprobar si aparece públicamente; no asumas que `201` significa aprobado.

El comentario solo puede crearse en un foro publicado. El endpoint no ofrece creación de respuestas anidadas aunque la entidad tenga `comentarioPadre`.

### Votar

Voto de foro:

```http
POST /api/foros/{id}/votos
```

Voto de respuesta:

```http
POST /api/foros/{id}/respuestas/{respuestaId}/votos
```

En ambos casos:

```json
{ "valor": 1 }
```

`valor` acepta `1` (positivo), `-1` (negativo) y `0` (retirar voto). La respuesta contiene `puntos` y `miVoto`. El usuario no puede votar su propio contenido (`400`); una respuesta oculta/no aprobada no se puede votar (`404`).

## 3. Bandeja y revisión humana

Todos estos endpoints requieren un token con rol `MODERADOR` o `ADMIN`.

### Consultar bandeja compartida

```http
GET /api/moderacion/bandeja
```

Devuelve una lista de revisiones vigentes que requieren intervención; puede incluir revisiones libres, reservadas y vencidas. Cada elemento incluye `moderacionId`, `tipoContenido`, IDs del foro/comentario, `categoria`, `titulo`, `contenido`, `autorId`, `autorNombre`, campos de predicción (`etiquetaPredicha`, `moderationScore`, `predictionConfidence`, `modeloNombre`, `modeloVersion`), `decision` y campos de asignación.

Ejemplo abreviado:

```json
{
  "moderacionId": 93,
  "tipoContenido": "FORO",
  "foroId": 15,
  "comentarioId": null,
  "categoria": "PREGUNTAS",
  "titulo": "Ayuda con el curso",
  "contenido": "Texto en revisión",
  "decision": "REQUIERE_REVISION",
  "atendidoPor": null,
  "fechaAsignacion": null,
  "fechaExpiracionAsignacion": null
}
```

`tipoContenido` es `FORO` o `COMENTARIO`. En el tipo contrario, los campos específicos pueden ser `null`.

### Tomar una revisión

```http
POST /api/moderacion/revisiones/{moderacionId}/tomar
```

No lleva cuerpo. Devuelve `200` con `{ "mensaje", "revision" }`. La reserva dura 30 minutos; tras tomarla, aparecen el ID del moderador, su nombre y las fechas de asignación/vencimiento. Si otro moderador la reservó, expiró o ya no es la revisión vigente, se devuelve `409 Conflict`; refresca la bandeja antes de continuar.

### Mis revisiones

```http
GET /api/moderacion/mis-revisiones
```

Lista las reservas vigentes del usuario autenticado. No incluye reservas vencidas, liberadas o ya resueltas.

### Liberar una revisión

```http
DELETE /api/moderacion/revisiones/{moderacionId}/reserva
```

No lleva cuerpo. Responde `204 No Content`. Solo el moderador asignado puede liberarla; si ya no le pertenece o dejó de estar disponible, responde `409`.

### Resolver una revisión

```http
PATCH /api/moderacion/revisiones/{moderacionId}/decision
Content-Type: application/json
```

```json
{
  "decision": "APROBADO",
  "motivo": "Revisado; se permite la publicación."
}
```

`decision` es obligatorio y solo acepta `APROBADO` o `RECHAZADO`. `motivo` es opcional, máximo 2000 caracteres. Responde `200` con `mensaje`, IDs del contenido, decisión final y auditoría (`etiquetaConfirmada`, `revisadoPor`, `fechaRevision`, `motivoDecision`).

La decisión final reemplaza `decision` en el registro de moderación. Aprobar publica el contenido; rechazar lo mantiene oculto. La reserva se libera y la revisión deja de aparecer en la bandeja. Solo puede resolverla el moderador que conserva la reserva activa; ante vencimiento, cambio de dueño o resolución previa, se devuelve `409`.

## 4. Notificaciones

Estas rutas también requieren rol `MODERADOR` o `ADMIN`. Devuelven únicamente notificaciones del usuario autenticado.

| Solicitud | Respuesta |
|---|---|
| `GET /api/notificaciones` | Lista completa, más recientes primero. |
| `GET /api/notificaciones/no-leidas` | Lista de no leídas. |
| `GET /api/notificaciones/no-leidas/count` | Un número JSON, por ejemplo `3`. |
| `PATCH /api/notificaciones/{id}/leer` | Marca como leída; responde `204 No Content`. |

Cada objeto contiene `notificacionId`, `moderacionId`, `tipoContenido`, `contenidoId`, `mensaje`, `leida`, `fechaCreacion` y `fechaLectura`. Marcar dos veces es válido/idempotente. Un ID ajeno o inexistente responde `404`.

## 5. Errores y comportamiento recomendado del cliente

| Código | Interpretación para la interfaz |
|---|---|
| `400` | Datos de request inválidos o intento de votar el propio contenido. Mostrar validación adecuada. |
| `401` o `403` | Token ausente, inválido o vencido; reautenticar. La respuesta exacta depende de la configuración de Spring Security. |
| `403` | Rol insuficiente para moderación/notificaciones. |
| `404` | Recurso inexistente o no visible para esa operación. |
| `409` | La reserva cambió de estado o dejó de estar disponible; actualizar la bandeja. |
| `503` | Servicio de moderación Python no disponible (en el flujo de respuestas). Mantener el contenido como enviado/pendiente y evitar reintentar ciegamente el POST. |

Los errores de negocio manejados por `GlobalExceptionHandler` tienen forma `{ "status": 409, "message": "..." }`. Los filtros de seguridad pueden responder sin cuerpo, así que el cliente debe basarse primero en el código HTTP.

## 6. Flujos UI recomendados

### Usuario del foro

1. Cargar `/api/foros` y mostrar solo lo que devuelve el backend.
2. Abrir `/api/foros/{id}` para detalle y respuestas visibles.
3. Enviar creación de foro/respuesta y mostrar confirmación de envío, no de publicación; volver a consultar la vista pública.
4. Para votar, enviar el valor actual deseado. Enviar `0` para retirar el voto.

### Moderador

1. Cargar `/api/moderacion/bandeja` y `/api/notificaciones/no-leidas/count`.
2. Tomar una revisión antes de abrir el flujo de evaluación; guardar el ID y fecha de vencimiento.
3. Al detectar `409`, refrescar la bandeja y pedir que el moderador seleccione otra revisión.
4. Resolver mediante el endpoint decision, o liberar con DELETE si abandona la tarea.
5. Actualizar bandeja y contador de notificaciones. El historial de notificaciones es independiente de la reserva.

El backend es la fuente de verdad para visibilidad, votos, estados de moderación y permisos. No mostrar contenido oculto basándose solo en una respuesta anterior de creación.
