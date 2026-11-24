# Documentación de inferencia Conv1D

## Entrada y salida de `predict()`

El modelo persistido es un `keras.Sequential` que recibe cadenas (`tf.string`) y ya contiene la capa `TextVectorization`. `modelo.predict(textos)` devuelve un arreglo NumPy de números de punto flotante, normalmente de forma `(cantidad_de_textos, 1)`. Cada número es la salida de la neurona final con activación sigmoide: una probabilidad entre 0 y 1 para la clase positiva `1` (`REVISIÓN`).

En el experimento se fuerza la entrada a tensores de texto y se aplana el resultado con `.ravel()`, por lo que la función `predecir_probabilidades` devuelve un vector de forma `(cantidad_de_textos,)`. Ese aplanado no cambia los valores ni las probabilidades.

## Umbral y etiqueta final

El umbral configurado es `0.4`. La evaluación y los ejemplos de inferencia aplican esta regla:

```python
etiqueta = (probabilidad >= 0.4).astype("int32")
```

- Si `probabilidad >= 0.4`, la etiqueta es `1` y la decisión es `REVISIÓN`.
- Si `probabilidad < 0.4`, la etiqueta es `0` y la decisión es `PUBLICAR`.

El umbral se seleccionó según F1 en validación y se fijó en `0.4` para la evaluación final en test. La evaluación no aplica una transformación adicional a la salida sigmoide antes de compararla: no hay calibración, redondeo ni segunda activación. El redondeo de probabilidades que aparece en tablas es solo presentación y no participa en la decisión.

## Ejemplo de uso

```python
import numpy as np
import tensorflow as tf
import keras

modelo = keras.models.load_model(
    "programacion/modelos/moderacion/conv1D_keras/modelo_embedding_conv1d.keras"
)

textos = tf.constant(["Texto a moderar"], dtype=tf.string)
probabilidades = modelo.predict(textos, verbose=0).ravel()
etiquetas = (probabilidades >= 0.4).astype(np.int32)
decisiones = np.where(etiquetas == 1, "REVISIÓN", "PUBLICAR")
```

La probabilidad y la etiqueta deben conservarse como valores distintos: `predict()` entrega la probabilidad; el código de umbral obtiene la etiqueta y la decisión operativa.

## Referencias de configuración y evaluación

El umbral y las etiquetas también están registrados en `programacion/modelos/moderacion/conv1D_keras/configuracion.json`. El informe `programacion/experimentos/experimento_embedding-conv1D/resultados_brutos.md` contiene la evaluación final en test con ese umbral.
