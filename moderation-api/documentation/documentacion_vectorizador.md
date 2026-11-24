# Documentación del vectorizador Conv1D

## Capa utilizada

El experimento crea una capa `tf.keras.layers.TextVectorization` e incorpora esa misma transformación en el modelo secuencial. La configuración relevante es:

```python
TextVectorization(
    max_tokens=20000,
    standardize="lower_and_strip_punctuation",
    split="whitespace",
    output_mode="int",
    output_sequence_length=200,
)
```

Por tanto, `output_mode` es `"int"`: cada token se representa con un índice entero. El resultado de la vectorización tiene longitud fija de 200 tokens por texto; las secuencias largas se truncan y las cortas se rellenan.

## Vocabulario y adaptación

La capa se adapta con `adapt()` usando únicamente el conjunto de entrenamiento (`X_train`). No se adapta con validación ni con prueba. El registro del experimento muestra un vocabulario de 20 000 entradas, que es el límite configurado (`max_tokens`). Keras reserva índices especiales para padding y fuera de vocabulario; el tamaño reportado incluye esos índices.

Las palabras que no están en el vocabulario se convierten al índice OOV (fuera de vocabulario), en vez de ampliar el vocabulario durante la inferencia.

El vocabulario aprendido se puede recuperar de la capa guardada mediante `get_vocabulary()`. No hace falta copiarlo a mano en este documento. El artefacto independiente es `programacion/modelos/moderacion/conv1D_keras/vectorizador_textos.keras`; además, el modelo completo incorpora la capa adaptada. El script de entrenamiento documenta que serializa el vocabulario aprendido al guardar los artefactos.

## Uso en inferencia

El modelo completo recibe cadenas y ejecuta internamente la vectorización. Para inferencia normal, pasar el texto directamente a `modelo.predict(...)`; no se debe aplicar también el vectorizador independiente antes de llamar al modelo, pues se estaría duplicando la transformación.

Si se necesita inspeccionar la salida numérica del vectorizador, puede cargarse `vectorizador_textos.keras` por separado. Su salida es una secuencia entera de 200 posiciones por entrada.

## Estandarización

La opción integrada `lower_and_strip_punctuation` convierte el texto a minúsculas y elimina puntuación según la implementación de Keras. La separación de tokens está configurada como `whitespace`. No hay una función personalizada de estandarización en el experimento.

Para los límites del preprocesamiento y su equivalencia con los datos de entrenamiento, véase [documentacion_preprocesamiento.md](documentacion_preprocesamiento.md).
