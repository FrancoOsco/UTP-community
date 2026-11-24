# Documentación del preprocesamiento de texto

## Texto usado durante el entrenamiento

El experimento carga `train.csv`, `validation.csv` y `test.csv`, y utiliza la columna `texto` como entrada. Antes de entregar esos valores al modelo, reemplaza nulos por cadena vacía y asegura que sean cadenas (`fillna("").astype(str)`). La preparación y división del conjunto también recorta espacios al inicio y al final (`str.strip()`) y excluye textos vacíos.

Los datos que alimentan el entrenamiento ya son una sola columna `texto`. El experimento Conv1D no recibe campos separados `titulo` y `descripcion`, no los une y no define un separador entre ellos. Por ello, no hay un separador título/descripción que pueda atribuirse a este entrenamiento. En una integración que tenga ambos campos, la regla para construir una sola cadena debe acordarse explícitamente y validarse con el modelo; no debe suponerse que el artefacto fue entrenado con una concatenación concreta.

## Transformaciones dentro de `TextVectorization`

El modelo incluye una capa `TextVectorization` configurada con `standardize="lower_and_strip_punctuation"` y `split="whitespace"`:

- **Minúsculas:** sí, a través de la estandarización integrada de Keras.
- **Puntuación:** se elimina mediante la estandarización integrada `lower_and_strip_punctuation`.
- **Tildes/acentos:** el código no aplica eliminación ni normalización explícita de acentos. No se debe quitar tildes antes de la inferencia.
- **URLs:** no existe una etapa explícita que detecte o elimine URLs. La puntuación que rodea sus componentes puede verse afectada por la estandarización general, pero no se elimina la URL como entidad.
- **Emojis:** no existe un filtro explícito de emojis. El código de datos y el experimento no declara su eliminación.
- **Espacios repetidos:** no hay una sustitución explícita de espacios consecutivos. La segmentación usa `whitespace`, que separa tokens por espacios en blanco; adicionalmente, la preparación recorta los extremos.
- **Otras limpiezas:** el experimento no contiene una etapa adicional con expresiones regulares para limpiar el texto.

La vectorización limita cada secuencia a 200 tokens y representa palabras desconocidas con el índice OOV. Véase [documentacion_vectorizador.md](documentacion_vectorizador.md).

## Contrato recomendado para el servicio

Al usar `modelo_embedding_conv1d.keras`, pasar una cadena con el texto completo y dejar que el modelo ejecute la capa incorporada. No volver a pasar el texto por un vectorizador si luego se invoca el modelo completo. El servicio debe preservar las tildes, URLs y emojis que reciba y no añadir eliminaciones o normalizaciones que no estén en el flujo documentado.

La etapa previa de entrenamiento convierte nulos a `""` y recorta extremos. Las entradas normales a moderación deben ser texto; si se admiten valores nulos en el servicio, convertirlos a cadena vacía es la política coherente con ese entrenamiento.
