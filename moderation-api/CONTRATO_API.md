# Contrato de la API de moderación

Servicio FastAPI para clasificar texto de publicaciones y comentarios de UTP Community. La versión declarada por la aplicación es `1.0.0`; la versión devuelta en las respuestas se toma de `metadata.json`.

## Base URL local

```text
http://127.0.0.1:8000
```

## Endpoints

### `GET /health`

Comprueba que el proceso está disponible y reporta el estado de carga del modelo. No requiere API Key, para permitir comprobaciones de disponibilidad.

Respuesta `200 OK`:

```json
{
  "status": "ok",
  "model_loaded": true,
  "model_version": "1.0.0"
}
```

`status` será `ok` si el servicio informa que el modelo está cargado, o `unavailable` si no lo está. En el arranque normal, la aplicación carga y prueba el modelo antes de aceptar peticiones.

### `POST /predict`

Clasifica una publicación o comentario. Ambas clases de contenido usan el mismo contrato. Requiere la cabecera `X-API-Key`.

Cabeceras:

```http
Content-Type: application/json
X-API-Key: <clave compartida entre Spring Boot y moderation-api>
```

Cuerpo —el único campo permitido es `texto`—:

```json
{
  "texto": "Ayuda con algoritmos [SEP] ¿Alguien puede explicarme cómo resolver este ejercicio?"
}
```

El campo debe ser una cadena no vacía después de quitar espacios al inicio y al final. No se aceptan campos adicionales como `titulo` o `descripcion`.

Respuesta `200 OK`:

```json
{
  "label": 0,
  "probability": 0.94,
  "score": 0.06,
  "model_version": "1.0.0",
  "model_name": "embedding-conv1d"
}
```

Campos de respuesta:

| Campo | Significado |
| --- | --- |
| `label` | Etiqueta final: `0` permitido/publicar; `1` requiere intervención/revisión. |
| `probability` | Confianza asociada a la etiqueta predicha. Si `label` es `0`, es `1 - score`; si es `1`, coincide con `score`. |
| `score` | Probabilidad cruda que devuelve el modelo para la clase positiva `1` (requiere revisión). |
| `model_version` | Versión leída de `metadata.json`. |
| `model_name` | Identificador fijo del modelo que realizó la predicción (`embedding-conv1d`). |

La decisión usa el umbral de `configuracion.json` (actualmente `0.4`): `score >= 0.4` produce `label: 1`; en caso contrario produce `label: 0`. El servicio limpia el texto antes de inferirlo según `app/text_cleaning.py` y luego lo entrega al modelo, que incluye su propio vectorizador. La limpieza quita el carácter de reemplazo Unicode `�`, menciones `@user`/`@usuario` y URLs que comiencen con `http://` o `https://` (también los marcadores `LINK`/`nLINK`); reemplaza secuencias de dos o más signos `?`, quita signos `?` iniciales, colapsa espacios en blanco consecutivos y elimina puntuación inicial `,.;:`. Finalmente recorta los extremos. Dentro del modelo, `TextVectorization` aplica minúsculas y eliminación de puntuación.

## Autenticación

`POST /predict` requiere una clave compartida mediante la cabecera `X-API-Key`. El valor esperado se lee de la variable de entorno `MODERATION_API_KEY`; el servicio no lee automáticamente `moderation-api/.env`. La clave debe tener al menos 32 caracteres. Si falta o no cumple esa longitud, el servicio falla al iniciar para no quedar activo con la predicción desprotegida.

La clave enviada se compara con la configurada. Si falta o no coincide, la respuesta es `401 Unauthorized`:

```json
{
  "detail": "Missing or invalid API key."
}
```

El servicio compara claves sin guardarlas en el código fuente. En despliegue, configúralas en el gestor de secretos o en las variables de entorno protegidas del entorno de ejecución. Spring Boot debe usar el mismo secreto y enviarlo en cada petición a `/predict`.

## Códigos HTTP

| Código | Cuándo ocurre |
| --- | --- |
| `200 OK` | `/health` respondió o `/predict` produjo una predicción. |
| `401 Unauthorized` | `/predict` recibió una API Key ausente o incorrecta. |
| `404 Not Found` | La ruta solicitada no existe. |
| `422 Unprocessable Entity` | El cuerpo no es JSON válido, falta `texto`, `texto` no es una cadena válida, queda vacío tras recortar espacios o contiene campos extra. |
| `500 Internal Server Error` | Ocurre un error no manejado durante la inferencia. Por ejemplo, el limpiador puede dejar vacío un texto que sí tenía caracteres antes de limpiarlo. |

Además, la aplicación no llega a iniciar si no puede validar la clave, cargar la configuración/metadatos, cargar el modelo o completar su prueba de humo. En ese caso no hay respuesta HTTP disponible.

## Ejecución local en Windows

Desde PowerShell, en la raíz del repositorio:

```powershell
cd moderation-api
py -V:3.11 -m venv .venv  # omitir si el entorno ya existe
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
$env:MODERATION_API_KEY = py -V:3.11 -c "import secrets; print(secrets.token_urlsafe(32))"
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

La variable creada así solo está disponible en esa sesión de PowerShell y en los procesos lanzados desde ella. Para usar la misma API Key en Spring Boot, configura ese mismo valor en el proceso Java. No copies la clave a este documento, al código ni a un archivo versionado.

El directorio de artefactos puede definirse con `MODERATION_ARTIFACTS_DIR`. Si no se define, `app/config.py` busca primero un conjunto completo en `moderation-api/artifacts/` y luego en `programacion/modelos/moderacion/conv1D_keras/`. En este repositorio los artefactos completos están en la segunda ruta.

Al iniciar, FastAPI carga el modelo y ejecuta la prueba de humo. Después puedes abrir `http://127.0.0.1:8000/docs`, comprobar `GET /health` y probar `/predict` agregando la cabecera `X-API-Key`.

## Ejemplos de comprobación con PowerShell

Health check:

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8000/health"
```

Predicción:

```powershell
$body = @{ texto = "No estoy de acuerdo con la solución propuesta porque existe una alternativa más eficiente." } | ConvertTo-Json
Invoke-RestMethod -Uri "http://127.0.0.1:8000/predict" -Method Post `
  -Headers @{ "X-API-Key" = $env:MODERATION_API_KEY } `
  -ContentType "application/json" -Body $body
```

## Despliegue y comunicación entre servicios

La API Key protege el endpoint de predicción, pero no sustituye el aislamiento de red. En producción, mantén el servicio Python accesible solo desde Spring Boot mediante una red privada o reglas de firewall; no publiques su puerto directamente en Internet. Si la comunicación cruza una red no confiable, usa HTTPS/TLS mediante el proxy o la infraestructura de despliegue. Configura también tiempos máximos de conexión y lectura en el cliente HTTP de Spring Boot.
