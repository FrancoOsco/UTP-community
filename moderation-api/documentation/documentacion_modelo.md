# Documentación del modelo Conv1D de moderación

## Alcance

Esta documentación describe el artefacto `programacion/modelos/moderacion/conv1D_keras/modelo_embedding_conv1d.keras`, contrastándolo con el experimento `programacion/experimentos/experimento_embedding-conv1D/experimento.py` y su informe de resultados.

## Tipo y arquitectura

El modelo es una red neuronal secuencial de Keras (`keras.Sequential`) para clasificación binaria. La entrada es texto y la red realiza tanto la vectorización como la clasificación. Su secuencia de capas es:

1. `Input(shape=(), dtype=tf.string)`: recibe una cadena por ejemplo.
2. `TextVectorization`: convierte el texto en una secuencia de índices.
3. `Embedding(input_dim=20000, output_dim=64)`: representa cada índice con un vector de 64 componentes.
4. `Conv1D(filters=128, kernel_size=3, activation="relu")`: busca patrones locales en ventanas de tres tokens.
5. `GlobalMaxPooling1D`: conserva la activación máxima de cada filtro.
6. `Dropout(0.5)`: regularización usada durante entrenamiento.
7. `Dense(1, activation="sigmoid")`: produce la probabilidad de la clase positiva.

El resumen guardado del experimento reporta 1 304 833 parámetros entrenables.

## Forma de entrada y salida

- **Entrada del modelo completo:** un lote de cadenas (`tf.string`), de forma `(lote,)`; cada elemento es un texto sin vectorizar.
- **Representación interna:** secuencias de longitud fija 200 y tensores de índices enteros.
- **Salida:** un valor por texto, normalmente con forma `(lote, 1)`, en el intervalo `[0, 1]`.
- **Interpretación:** la salida es la probabilidad estimada de la clase `1`, denominada `REVISIÓN` en la configuración del artefacto.

La salida sigmoide no es todavía la etiqueta final. La decisión se obtiene comparando la probabilidad con el umbral `0.4`; véase [documentacion_inferencia.md](documentacion_inferencia.md).

## Compilación registrada

El experimento compila la red con Adam (`learning_rate=0.0003`) y `BinaryCrossentropy`, apropiados para la clasificación binaria. El tratamiento de clases durante el entrenamiento utiliza pesos de clase.

## Artefactos relacionados

- Modelo: `programacion/modelos/moderacion/conv1D_keras/modelo_embedding_conv1d.keras`
- Vectorizador independiente: `programacion/modelos/moderacion/conv1D_keras/vectorizador_textos.keras`
- Configuración de inferencia: `programacion/modelos/moderacion/conv1D_keras/configuracion.json`
- Evaluación del experimento: `programacion/experimentos/experimento_embedding-conv1D/resultados_brutos.md`

El modelo completo contiene su propio `TextVectorization`. El vectorizador independiente sirve para inspeccionar o reutilizar la transformación, pero no es necesario pasarlo por separado cuando se invoca el modelo completo.

## Evaluación registrada

Con umbral `0.4`, el informe del experimento registra en el conjunto de prueba: accuracy `0.8347`, precision `0.8039`, recall `0.8417`, F1 `0.8224`, ROC-AUC `0.9200` y average precision `0.9255`. Son resultados de esa evaluación y no una garantía de rendimiento sobre datos nuevos.
