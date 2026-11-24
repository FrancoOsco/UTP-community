# Manipulación de datos.
import pandas as pd
import numpy as np

# TensorFlow y Keras para construir la red neuronal.
import tensorflow as tf

import keras
from keras.models import Sequential
from keras.layers import Dense, Dropout, Input

# Herramientas de Scikit-learn.
from sklearn.model_selection import train_test_split
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix,
    roc_auc_score,
    average_precision_score
)

# Herramientas para guardar y recuperar el vectorizador.
import joblib

# Control de valores aleatorios para reproducibilidad.
import random

# ============================================================
# CONFIGURACIÓN DE SEMILLAS
# ============================================================

SEED = 42

np.random.seed(SEED)
random.seed(SEED)
tf.random.set_seed(SEED)

# ==========================================================
# FASE 1: CARGA DE DATOS
# ==========================================================
print("""\n==========================================================
FASE 1: CARGA DE DATOS
==========================================================""")

# Cargar los tres conjuntos
df_train = pd.read_csv("datasets_consolidados/train.csv")
df_val = pd.read_csv("datasets_consolidados/validation.csv")
df_test = pd.read_csv("datasets_consolidados/test.csv")

# EXTRACCIÓN DE CARACTERÍSTICAS Y ETIQUETAS

X_train = df_train["texto"]
y_train = df_train["etiqueta"]

X_val = df_val["texto"]
y_val = df_val["etiqueta"]

X_test = df_test["texto"]
y_test = df_test["etiqueta"]

# VERIFICACIÓN DE ESTRUCTURA

conjuntos = {
    "Entrenamiento": df_train,
    "Validación": df_val,
    "Prueba": df_test
}

for nombre, df in conjuntos.items():

    print(f"\n=== {nombre.upper()} ===")

    print("Dimensiones:", df.shape)
    print("Columnas:", df.columns.tolist())

    print("Valores nulos:")
    print(df[["texto", "etiqueta"]].isnull().sum())

    print("Tipos de datos:")
    print(df[["texto", "etiqueta"]].dtypes)

# VERIFICACIÓN DE ETIQUETAS

for nombre, df in conjuntos.items():

    print(f"\n=== DISTRIBUCIÓN: {nombre.upper()} ===")

    print(df["etiqueta"].value_counts().sort_index())

    print("\nPorcentajes:")

    print(
        df["etiqueta"]
        .value_counts(normalize=True)
        .sort_index()
        .mul(100)
        .round(2)
    )

    print("Etiquetas únicas:", sorted(df["etiqueta"].dropna().unique()))

# VALIDACIÓN Y CONVERSIÓN DE ETIQUETAS

for nombre, df in conjuntos.items():

    # Comprobar que no existan etiquetas nulas
    if df["etiqueta"].isnull().any():
        raise ValueError(f"Existen etiquetas nulas en {nombre}")

    # Comprobar que las etiquetas sean binarias
    etiquetas = set(df["etiqueta"].unique())

    if not etiquetas.issubset({0, 1}):
        raise ValueError(f"Etiquetas inválidas en {nombre}: {etiquetas}")


# Convertir las etiquetas a enteros para Keras
y_train = y_train.astype("int32")
y_val = y_val.astype("int32")
y_test = y_test.astype("int32")

# NORMALIZACIÓN BÁSICA DE TEXTOS

# Convertir valores nulos en cadenas vacías
# y garantizar que todos los registros sean cadenas de texto.
X_train = X_train.fillna("").astype(str)
X_val = X_val.fillna("").astype(str)
X_test = X_test.fillna("").astype(str)

# COMPROBACIÓN DE INTERSECCIONES

textos_train = set(X_train)
textos_val = set(X_val)
textos_test = set(X_test)

print("\n=== REGISTROS COMPARTIDOS ===")

print("Train - Validation:", len(textos_train & textos_val))
print("Train - Test:", len(textos_train & textos_test))
print("Validation - Test:", len(textos_val & textos_test))

# RESUMEN FINAL

total_registros = len(df_train) + len(df_val) + len(df_test)

print("\n=== DIMENSIONES FINALES ===")

print("X_train:", X_train.shape)
print("y_train:", y_train.shape)

print("X_val:", X_val.shape)
print("y_val:", y_val.shape)

print("X_test:", X_test.shape)
print("y_test:", y_test.shape)

print("\nTotal de registros:", total_registros)
print("Total esperado: 53 726")

if total_registros != 53726:
    print("ADVERTENCIA: El total no coincide con los 53 726 registros esperados.")
else:
    print("Cantidad total de registros verificada.")

print("\n¡Datos cargados y verificados!")

# ==========================================================
# FASE 2 — Representación de texto y entrada de la red
# ==========================================================
print("""\n==========================================================
FASE 2 — Representación de texto y entrada de la red
==========================================================""")

# 1. CONFIGURACIÓN DEL VECTORIZADOR

MAX_TOKENS = 20000
MAX_SEQUENCE_LENGTH = 200

vectorizador = tf.keras.layers.TextVectorization(
    max_tokens=MAX_TOKENS,
    standardize="lower_and_strip_punctuation",
    split="whitespace",
    output_mode="int",
    output_sequence_length=MAX_SEQUENCE_LENGTH
)

print("Nuevo vectorizador TextVectorization creado.")

# 2. ADAPTACIÓN DEL VECTORIZADOR

# El vocabulario se aprende exclusivamente con entrenamiento.
# Esto evita utilizar información de validación o prueba.

dataset_textos = tf.data.Dataset.from_tensor_slices(
    X_train.to_numpy()
    ).batch(256)

vectorizador.adapt(dataset_textos)

print("\n=== VECTORIZACIÓN COMPLETADA ===")

print("Vocabulario máximo:", MAX_TOKENS)
print("Vocabulario aprendido:", vectorizador.vocabulary_size())
print("Longitud máxima de secuencia:", MAX_SEQUENCE_LENGTH)

# 3. TRANSFORMACIÓN DE LOS TEXTOS

# Convertimos los textos en secuencias de índices enteros.
# No reajustamos el vocabulario para validación ni prueba.

X_train_vectorizado = vectorizador(X_train.to_numpy())
X_val_vectorizado = vectorizador(X_val.to_numpy())
X_test_vectorizado = vectorizador(X_test.to_numpy())

print("\n=== DIMENSIONES DE LAS SECUENCIAS ===")

print("Train:", X_train_vectorizado.shape)
print("Validation:", X_val_vectorizado.shape)
print("Test:", X_test_vectorizado.shape)

# 4. VERIFICACIÓN DE LA REPRESENTACIÓN

print("\n=== TIPO DE REPRESENTACIÓN ===")

print("Train:", X_train_vectorizado.dtype)
print("Validation:", X_val_vectorizado.dtype)
print("Test:", X_test_vectorizado.dtype)


# Mostrar un ejemplo de transformación

print("\n=== EJEMPLO DE VECTORIZACIÓN ===")

print("Texto original:")
print(X_train.iloc[0])

print("\nSecuencia de índices:")
print(X_train_vectorizado[0].numpy())

print("\nPalabras del vocabulario:")
print(vectorizador.get_vocabulary()[1000:1050])

# 5. VERIFICACIÓN FINAL

assert X_train_vectorizado.shape[0] == len(y_train)
assert X_val_vectorizado.shape[0] == len(y_val)
assert X_test_vectorizado.shape[0] == len(y_test)

assert X_train_vectorizado.shape[1] == MAX_SEQUENCE_LENGTH
assert X_val_vectorizado.shape[1] == MAX_SEQUENCE_LENGTH
assert X_test_vectorizado.shape[1] == MAX_SEQUENCE_LENGTH

assert X_train_vectorizado.shape[1] == X_val_vectorizado.shape[1]
assert X_train_vectorizado.shape[1] == X_test_vectorizado.shape[1]

print("\n¡Verificación completada!")
print("Vocabulario:", vectorizador.vocabulary_size())
print("Longitud de secuencia:", MAX_SEQUENCE_LENGTH)
print("Las tres representaciones tienen dimensiones compatibles.")
print("Las dimensiones coinciden con sus respectivas etiquetas.")


# ============================================================
# FASE 3 — VERIFICACIÓN DE TENSORES DE ENTRADA
# ============================================================
print("""\n==========================================================
FASE 3 — VERIFICACIÓN DE TENSORES DE ENTRADA
==========================================================""")

print("=== VERIFICACIÓN DE TENSORES ===")

for nombre, tensor in [
    ("Train", X_train_vectorizado),
    ("Validation", X_val_vectorizado),
    ("Test", X_test_vectorizado)
]:

    print(f"\n{nombre}")
    print("Forma:", tensor.shape)
    print("Tipo:", tensor.dtype)
    print("Tipo de objeto:", type(tensor))

# COMPROBACIÓN DE COMPATIBILIDAD CON KERAS

modelo_prueba = keras.Sequential([
    keras.Input(
        shape=(MAX_SEQUENCE_LENGTH,),
        dtype="int64"
    ),
    keras.layers.Embedding(
        input_dim=vectorizador.vocabulary_size(),
        output_dim=64
    ),
    keras.layers.Conv1D(
        filters=128,
        kernel_size=3,
        activation="relu"
    ),
    keras.layers.GlobalMaxPooling1D(),
    keras.layers.Dense(1, activation="sigmoid")
])

# Tomar cuatro registros para probar la arquitectura
lote_prueba = X_train_vectorizado[:4]

salida_prueba = modelo_prueba(
    lote_prueba,
    training=False
)

print("\n=== PRUEBA DE COMPATIBILIDAD ===")

print("Forma de entrada:", lote_prueba.shape)
print("Forma de salida:", salida_prueba.shape)
print("Valores:", salida_prueba.numpy())

print("\n¡Los tensores son compatibles con Keras!")


# ============================================================
# FASE 4 — CONSTRUCCIÓN Y COMPILACIÓN DE EMBEDDING + CONV1D
# ============================================================
print("""\n==========================================================
FASE 4 — CONSTRUCCIÓN Y COMPILACIÓN DE EMBEDDING + CONV1D
==========================================================""")

# 1. DEFINIR LA ARQUITECTURA

modelo = keras.Sequential([
    # ENTRADA:
    # Recibe directamente el texto original.
    # El vectorizador se incorpora dentro del modelo.
    keras.Input(
        shape=(),
        dtype=tf.string
    ),

    # REPRESENTACIÓN DEL TEXTO:
    # Convierte las palabras en secuencias de índices enteros.
    vectorizador,

    # CAPA EMBEDDING:
    # Convierte cada índice en un vector denso de 64 dimensiones.
    # Estos vectores se aprenden durante el entrenamiento.
    keras.layers.Embedding(
        input_dim=vectorizador.vocabulary_size(),
        output_dim=64
    ),

    # CAPA CONVOLUCIONAL 1D:
    # Utiliza 128 filtros para detectar patrones locales.
    # Cada filtro analiza ventanas de 3 palabras consecutivas.
    keras.layers.Conv1D(
        filters=128,
        kernel_size=3,
        activation="relu"
    ),

    # GLOBAL MAX POOLING:
    # Selecciona el valor máximo de cada filtro a lo largo
    # de la secuencia.
    # Resume los patrones más relevantes detectados.
    keras.layers.GlobalMaxPooling1D(),

    # REGULARIZACIÓN:
    # Desactiva aleatoriamente el 50% de las unidades
    # durante el entrenamiento para reducir el sobreajuste.
    keras.layers.Dropout(0.5),

    # CAPA DE SALIDA:
    # Produce una probabilidad entre 0 y 1.
    # 0 = permitido.
    # 1 = requiere intervención.
    keras.layers.Dense(
        1,
        activation="sigmoid"
    )
])

print("Nueva arquitectura Embedding + Conv1D construida.")

# 2. COMPILAR EL MODELO

modelo.compile(
    # Optimizador:
    # Adam actualiza los pesos de la red durante el entrenamiento.
    optimizer=keras.optimizers.Adam(
        learning_rate=0.0003
    ),

    # Función de pérdida:
    # BinaryCrossentropy es adecuada para clasificación binaria.
    loss=keras.losses.BinaryCrossentropy(),

    # Métricas:
    # Permiten evaluar el rendimiento durante el entrenamiento.
    metrics=[
        keras.metrics.BinaryAccuracy(name="accuracy"),
        keras.metrics.Precision(name="precision"),
        keras.metrics.Recall(name="recall"),
        keras.metrics.AUC(name="roc_auc")
    ]
)

print("Modelo compilado correctamente.")

# 3. INSPECCIONAR LA ARQUITECTURA

modelo.summary()

# ============================================================
# FASE 5 — ENTRENAMIENTO DE EMBEDDING + CONV1D
# ============================================================
print("""\n==========================================================
FASE 5 — ENTRENAMIENTO DE EMBEDDING + CONV1D
==========================================================""")

# 1. CALCULAR LOS PESOS DE CLASE

# Calculamos cuántos registros pertenecen a cada clase.
# Esto permite compensar el desequilibrio de etiquetas.
cantidad_0 = np.sum(y_train == 0)
cantidad_1 = np.sum(y_train == 1)

# Calculamos los pesos utilizando el método balanceado.
# La clase minoritaria tendrá un peso mayor durante el entrenamiento.
peso_0 = len(y_train) / (2 * cantidad_0)
peso_1 = len(y_train) / (2 * cantidad_1)

class_weights = {
    0: peso_0,
    1: peso_1
}

print("\nPesos de clase:", class_weights)

# 2. PREPARAR LOS DATASETS

# Convertir etiquetas de Pandas a NumPy float32.
# Utilizamos la forma (n, 1) para que coincida con la salida
# del modelo, que tiene una neurona con activación sigmoid.
y_train_tf = y_train.to_numpy(
    dtype=np.float32
).reshape(-1, 1)

y_val_tf = y_val.to_numpy(
    dtype=np.float32
).reshape(-1, 1)

y_test_tf = y_test.to_numpy(
    dtype=np.float32
).reshape(-1, 1)

# Convertir los textos a arreglos NumPy de cadenas.
# Ya no necesitamos utilizar SparseTensor ni matrices TF-IDF.

X_train_tf = X_train.to_numpy(dtype=str)
X_val_tf = X_val.to_numpy(dtype=str)
X_test_tf = X_test.to_numpy(dtype=str)

print("\n=== DIMENSIONES DE LOS DATOS ===")

print("Train:", X_train_tf.shape, y_train_tf.shape)
print("Validation:", X_val_tf.shape, y_val_tf.shape)
print("Test:", X_test_tf.shape, y_test_tf.shape)

# 3. CONSTRUIR EL DATASET DE ENTRENAMIENTO

# Crear el dataset utilizando los textos originales y sus etiquetas.
dataset_train = tf.data.Dataset.from_tensor_slices(
    (X_train_tf, y_train_tf)
)

# Barajar los registros para evitar que la red aprenda
# a partir de un orden fijo de ejemplos.
dataset_train = dataset_train.shuffle(
    buffer_size=10000,
    seed=SEED,
    reshuffle_each_iteration=True
)

# 4. APLICAR LOS PESOS DE CLASE

# Esta función asigna un peso a cada registro dependiendo
# de su etiqueta:
# Clase 0 -> peso_0
# Clase 1 -> peso_1
def agregar_peso(x, y):

    peso = tf.where(
        tf.equal(y, 0.0),
        tf.constant(peso_0, dtype=tf.float32),
        tf.constant(peso_1, dtype=tf.float32)
    )

    return x, y, peso

# Incorporar los pesos al dataset de entrenamiento.
dataset_train = dataset_train.map(
    agregar_peso,
    num_parallel_calls=tf.data.AUTOTUNE
)

# Agrupar los registros en lotes de 64.
# Esto permite actualizar los pesos de la red utilizando
# pequeños grupos de ejemplos.
dataset_train = dataset_train.batch(64)

# Preparar los siguientes lotes mientras se procesa el actual.
dataset_train = dataset_train.prefetch(tf.data.AUTOTUNE)

# 5. CONSTRUIR EL DATASET DE VALIDACIÓN

# Crear el dataset con textos originales y etiquetas.
# No aplicamos pesos de clase a validación:
# queremos evaluar el rendimiento sobre su distribución real.

dataset_val = tf.data.Dataset.from_tensor_slices(
    (X_val_tf, y_val_tf)
)

dataset_val = dataset_val.batch(64)

dataset_val = dataset_val.prefetch(tf.data.AUTOTUNE)

print("\nDatasets preparados correctamente.")

# 6. CONFIGURAR LOS CALLBACKS

# EARLY STOPPING:
# Detiene el entrenamiento cuando la pérdida de validación
# deja de mejorar durante 4 épocas consecutivas.
# Restaura los pesos correspondientes a la mejor época.
early_stopping = keras.callbacks.EarlyStopping(
    monitor="val_loss",
    patience=4,
    restore_best_weights=True,
    verbose=1
)

# REDUCE LEARNING RATE ON PLATEAU:
# Reduce la tasa de aprendizaje a la mitad cuando
# la pérdida de validación deja de mejorar durante 2 épocas.
# El valor mínimo permitido será 0.000001.
reducir_lr = keras.callbacks.ReduceLROnPlateau(
    monitor="val_loss",
    factor=0.5,
    patience=2,
    min_lr=0.000001,
    verbose=1
)

# 7. ENTRENAR EL MODELO EMBEDDING + CONV1D

print("\nIniciar entrenamiento")

historial = modelo.fit(
    # Dataset de entrenamiento con pesos de clase.
    dataset_train,

    # Dataset de validación sin pesos de clase.
    validation_data=dataset_val,

    # Máximo de épocas permitidas.
    epochs=15,

    # Callbacks para controlar el entrenamiento.
    callbacks=[
        reducir_lr,
        early_stopping
    ],

    verbose=1
)

# 8. INSPECCIONAR EL HISTORIAL DE ENTRENAMIENTO

print("\nÉpocas ejecutadas:", len(historial.history["loss"]))

print("\nHistorial de entrenamiento:")

for metrica in [
        "loss",
        "val_loss",
        "accuracy",
        "val_accuracy",
        "precision",
        "val_precision",
        "recall",
        "val_recall",
        "roc_auc",
        "val_roc_auc"
    ]:

    print(f"{metrica}: {historial.history[metrica]}")

# INSPECCIONAR VECTORES GENERADOS POR LA CAPA EMBEDDING

# 1. Encontrar automáticamente la capa de Embedding dentro del modelo
capa_embedding = None
for layer in modelo.layers:
    if isinstance(layer, keras.layers.Embedding):
        capa_embedding = layer
        break

if capa_embedding is not None:
    # 2. Extraer la matriz de pesos (la tabla con todos los vectores)
    # Forma esperada: (vocab_size, 64)
    matriz_vectores = capa_embedding.get_weights()[0]
    
    print("=== MATRIZ DE EMBEDDING EXTRAÍDA ===")
    print("Dimensiones de la matriz:", matriz_vectores.shape)
    
    # 3. Función para consultar el vector de cualquier palabra
    def consultar_vector(palabra):
        vocabulario = vectorizador.get_vocabulary()
        if palabra in vocabulario:
            indice = vocabulario.index(palabra)
            vector = matriz_vectores[indice]
            return indice, vector
        else:
            return None, f"La palabra '{palabra}' no está en el vocabulario."

    # 4. Ejemplo de consulta con algunas palabras de tu vocabulario
    palabras_prueba = ["economía", "simple", "noticias", "impuestos"]

    print("\n=== EJEMPLOS DE VECTORES (64 DIMENSIONES) ===")
    for palabra in palabras_prueba:
        idx, resultado = consultar_vector(palabra)
        if idx is not None:
            print(f"\nPalabra: '{palabra}' (Índice en vocabulario: {idx})")
            # Mostramos los primeros 5 valores a modo de muestra para no saturar la consola
            print(f"Primeros 5 valores del vector: {resultado[:5]} ... (Total dimensiones: {len(resultado)})")
        else:
            print(f"\n{resultado}")
else:
    print("No se encontró ninguna capa de Embedding en el modelo.")


# ==========================================================
# FASE 6: selección del umbral y evaluación del modelo actual
# ==========================================================
print("""\n==========================================================
FASE 6: selección del umbral y evaluación del modelo actual
==========================================================""")

from sklearn.metrics import classification_report

# El modelo recibe tensores tf.string. Los arreglos NumPy dtype=str pueden
# tener un dtype Unicode fijo (por ejemplo, str150784) que Keras no reconoce.
def predecir_probabilidades(textos, verbose=1):
    textos_tf = tf.constant([str(texto) for texto in textos], dtype=tf.string)
    return modelo.predict(textos_tf, batch_size=64, verbose=verbose).ravel()

print("\nObteniendo probabilidades del conjunto de validación...")
y_val_prob = predecir_probabilidades(X_val_tf)
y_val_real = np.asarray(y_val, dtype=np.int32).ravel()
if len(y_val_prob) != len(y_val_real):
    raise ValueError("El número de predicciones de validación no coincide con sus etiquetas.")

# Seleccionar el umbral usando solo validación: maximiza F1 para la clase
# revisión; los empates se resuelven por precisión y después por umbral alto.
umbrales = np.arange(0.10, 0.71, 0.05)
resultados_umbral = []

for umbral in umbrales:
    y_val_pred = (y_val_prob >= umbral).astype(np.int32)
    tn, fp, fn, tp = confusion_matrix(y_val_real, y_val_pred, labels=[0, 1]).ravel()
    resultados_umbral.append({
        "Umbral": float(umbral),
        "Precision": precision_score(y_val_real, y_val_pred, zero_division=0),
        "Recall": recall_score(y_val_real, y_val_pred, zero_division=0),
        "F1": f1_score(y_val_real, y_val_pred, zero_division=0),
        "TN": int(tn), "FP": int(fp), "FN": int(fn), "TP": int(tp),
        "En_revision": int(tp + fp),
    })

df_resultados_umbral = pd.DataFrame(resultados_umbral)
df_ordenado = df_resultados_umbral.sort_values(
    ["F1", "Precision", "Umbral"], ascending=[False, False, False], kind="stable"
)

# Seleccionar umbral
UMBRAL = 0.4 # o 0.35
print("\n=== Mejores umbrales en validación (ordenados por F1) ===")
print(df_ordenado.head(10).round(4).to_string(index=False))
print(f"\nUmbral seleccionado: {UMBRAL:.2f}")

# ==========================================================
# FASE 7: evaluación final en el conjunto de prueba
# ==========================================================
print("""\n==========================================================
FASE 7: evaluación final en el conjunto de prueba
==========================================================""")

print("\nObteniendo probabilidades del conjunto de prueba...")
y_test_prob = predecir_probabilidades(X_test_tf)
y_test_real = np.asarray(y_test, dtype=np.int32).ravel()
if len(y_test_prob) != len(y_test_real):
    raise ValueError("El número de predicciones de prueba no coincide con sus etiquetas.")
y_test_pred = (y_test_prob >= UMBRAL).astype(np.int32)

print(f"\n=== Evaluación final en test (umbral {UMBRAL:.2f}) ===")
print("Accuracy:", accuracy_score(y_test_real, y_test_pred))
print("Precision:", precision_score(y_test_real, y_test_pred, zero_division=0))
print("Recall:", recall_score(y_test_real, y_test_pred, zero_division=0))
print("F1:", f1_score(y_test_real, y_test_pred, zero_division=0))
print("ROC-AUC:", roc_auc_score(y_test_real, y_test_prob))
print("Average Precision:", average_precision_score(y_test_real, y_test_prob))
matriz = confusion_matrix(y_test_real, y_test_pred, labels=[0, 1])
tn, fp, fn, tp = matriz.ravel()
print("\nMatriz de confusión (filas=real, columnas=predicha):")
print(matriz)
print(f"TN: {tn} | FP: {fp} | FN: {fn} | TP: {tp}")
print("Publicaciones enviadas a revisión:", int(np.sum(y_test_pred == 1)))
print("Porcentaje enviado a revisión:", f"{np.mean(y_test_pred == 1) * 100:.2f}%")
print("\nReporte de clasificación:")
print(classification_report(y_test_real, y_test_pred, zero_division=0))

# Ejemplos del conjunto de prueba: se conserva el orden entre textos y scores.
N_EJEMPLOS = min(30, len(df_test))
tabla_test = pd.DataFrame({
    "texto": df_test["texto"].reset_index(drop=True).astype(str).str.slice(0, 100),
    "real": y_test_real,
    "predicha": y_test_pred,
    "probabilidad_revision": y_test_prob.round(3),
})

tabla_test["resultado"] = np.select(
    [
        (tabla_test["real"] == 1) & (tabla_test["predicha"] == 1),
        (tabla_test["real"] == 0) & (tabla_test["predicha"] == 0),
        (tabla_test["real"] == 0) & (tabla_test["predicha"] == 1),
        (tabla_test["real"] == 1) & (tabla_test["predicha"] == 0),
    ], ["TP", "TN", "FP", "FN"], default="?"
)

pd.set_option("display.width", 250)
pd.set_option("display.max_colwidth", 105)
print(f"\n=== {N_EJEMPLOS} ejemplos de test (umbral {UMBRAL:.2f}) ===")
print(tabla_test.sample(N_EJEMPLOS, random_state=SEED).to_string(index=False))
print("\nResumen de resultados:")
print(tabla_test["resultado"].value_counts().to_string())

# Inferencia de nuevos textos: el modelo recibe cadenas y vectoriza dentro
# de la arquitectura, igual que durante entrenamiento y evaluación.
textos_nuevos = [
    # GRUPO 1: PERMITIDO (0)
    "Qué buena clase la de hoy, gracias profesor",
    "Esta tarea está muy difícil, alguien me ayuda?",
    "Qué mierda de servicio tiene esta universidad",
    "Me encantó la conferencia, muy interesante",
    "El profesor no explicó bien el tema y considero que debería mejorar sus clases",
    "Estoy harto de que la plataforma se caiga durante los exámenes",
    "El término 'puta' aparece en el texto que estamos analizando para lingüística",
    "No compartan sus contraseñas con nadie, podrían robar sus cuentas",
    "El profesor explicó que la palabra 'idiota' puede considerarse un insulto",
    "Qué pésima organización tuvo este evento académico",

    # GRUPO 2: REVISIÓN (1)
    "Eres un completo idiota, no sabes nada",
    "Vete a la mierda, hijo de puta",
    "El profesor tal no sabe nada, es basura y más",
    "Deja de molestarme, ya te dije que no estoy interesado en ti",
    "Si no aceptas salir conmigo les contaré a todos lo libertina que eres",
    "Voy a publicar tus fotos privadas para que todos se burlen de ti",
    "Dame tu contraseña de la universidad o publicaré tus datos personales",
    "Pásame las respuestas del examen para entregarlas como si fueran mías",
    "Te voy a esperar a la salida para golpearte",
    "Voy a vender certificados falsos de la universidad a quien los necesite",

    # GRUPO 3: AMBIGUO (REVISIÓN HUMANA)
    "aquí pueden subir contenido gore",
    "Eres una bestia jajaja, gracias por ayudarme",
    "Qué mierda, otra vez desaprobé el examen",
    "Si vuelves a molestarme, vas a tener problemas",
    "Ese profesor es un animal explicando, no se le entiende nada",
    "No sé si publicar el nombre de quien me está acosando",
    "Alguien sabe dónde conseguir los exámenes filtrados?",
    "Ese comentario fue una amenaza o solamente estaba bromeando?",
    "Voy a hacer que te arrepientas de lo que hiciste",
    "Dicen que venden cuentas institucionales por internet, alguien sabe si es cierto?"
]

prob_nuevo = predecir_probabilidades(textos_nuevos, verbose=0)
pred_nuevo = (prob_nuevo >= UMBRAL).astype(np.int32)
tabla_nuevos = pd.DataFrame({
    "texto": textos_nuevos,
    "probabilidad_revision": prob_nuevo.round(3),
    "predicha": pred_nuevo,
    "decision": np.where(pred_nuevo == 1, "REVISIÓN", "PUBLICAR"),
})
print(f"\n=== Inferencia de textos nuevos (umbral {UMBRAL:.2f}) ===")
print(tabla_nuevos.to_string(index=False))


# ===== IMPORTANCIA DE TOKENS DEL MODELO EMBEDDING + CONV1D =====
# Esta red no tiene coeficientes lineales por término: combina embeddings,
# convoluciones y pooling. Como resumen interpretable, se mide el cambio de
# probabilidad que produce cada token cuando se evalúa como texto aislado,
# comparándolo con la predicción para un texto vacío. Es una señal aproximada,
# sensible al contexto y no equivale a un coeficiente causal del modelo.

print("\nCalculando importancia aproximada por token...")
vocabulario = vectorizador.get_vocabulary()
ids_vocabulario = np.arange(2, len(vocabulario), dtype=np.int32)  # omite padding y OOV
tokens_vocabulario = [vocabulario[token_id] for token_id in ids_vocabulario]

# Contar documentos de entrenamiento que contienen cada token (sin duplicar
# repeticiones dentro de un mismo documento) y cuántos pertenecen a la clase 1.
secuencias_train = X_train_vectorizado.numpy()
y_train_np = np.asarray(y_train, dtype=np.int32).ravel()
docs_por_token = np.zeros(len(vocabulario), dtype=np.int64)
unos_por_token = np.zeros(len(vocabulario), dtype=np.int64)
for secuencia, etiqueta in zip(secuencias_train, y_train_np):
    ids_presentes = np.unique(secuencia[(secuencia > 1) & (secuencia < len(vocabulario))])
    docs_por_token[ids_presentes] += 1
    if etiqueta == 1:
        unos_por_token[ids_presentes] += 1

probabilidad_texto_vacio = float(predecir_probabilidades([""], verbose=0)[0])
probabilidades_token = predecir_probabilidades(tokens_vocabulario, verbose=1)
impacto_tokens = probabilidades_token - probabilidad_texto_vacio

coef = pd.DataFrame({
    "termino": tokens_vocabulario,
    "impacto_aislado_probabilidad": impacto_tokens,
    "probabilidad_aislada_revision": probabilidades_token,
    "docs_train": docs_por_token[ids_vocabulario],
})

coef["%unos_con_termino"] = (
    unos_por_token[ids_vocabulario]
    / np.maximum(coef["docs_train"].to_numpy(), 1)
    * 100
).round(1)

pd.set_option("display.width", 250)
pd.set_option("display.max_rows", 200)
MIN_DOCS = 15
tokens_frecuentes = coef[coef["docs_train"] >= MIN_DOCS]

print(f"\nProbabilidad base para texto vacío: {probabilidad_texto_vacio:.4f}")
print(
    f"\n=== Top 40 tokens con mayor impacto aislado hacia REVISIÓN "
    f"(al menos {MIN_DOCS} documentos) ==="
)
print(
    tokens_frecuentes.nlargest(40, "impacto_aislado_probabilidad")
    .round(4)
    .to_string(index=False)
)
print("\n=== Top 40 tokens con menor impacto aislado hacia REVISIÓN ===")
print(
    tokens_frecuentes.nsmallest(40, "impacto_aislado_probabilidad")
    .round(4)
    .to_string(index=False)
)


# ===== GUARDAR MODELO, VECTORIZADOR Y UMBRAL =====
"""
import json
from pathlib import Path

directorio_artefactos = Path(__file__).resolve().parent / "artefactos"
directorio_artefactos.mkdir(parents=True, exist_ok=True)

ruta_modelo = directorio_artefactos / "modelo_embedding_conv1d.keras"
ruta_vectorizador = directorio_artefactos / "vectorizador_textos.keras"
ruta_configuracion = directorio_artefactos / "configuracion.json"

# Keras guarda por defecto el vocabulario adaptado en un archivo de texto
# usando la codificación local de Windows (cp1252). Si el vocabulario contiene
# emojis, ese guardado falla. Al reconstruir la capa con el vocabulario
# explícito, Keras lo serializa dentro del archivo .keras en formato UTF-8.
vocabulario_completo = vectorizador.get_vocabulary()
vectorizador_persistible = keras.layers.TextVectorization(
    max_tokens=MAX_TOKENS,
    standardize="lower_and_strip_punctuation",
    split="whitespace",
    output_mode="int",
    output_sequence_length=MAX_SEQUENCE_LENGTH,
    vocabulary=vocabulario_completo[2:],  # los índices de padding/OOV se reservan
)

# Reutiliza las capas ya entrenadas, reemplazando únicamente la instancia del
# vectorizador por otra con el mismo vocabulario serializable.
modelo_persistible = keras.Sequential(
    [
        keras.Input(shape=(), dtype=tf.string),
        vectorizador_persistible,
        *modelo.layers[1:],
    ],
    name="modelo_embedding_conv1d",
)

modelo_persistible.save(ruta_modelo)

entrada_vectorizador = keras.Input(shape=(), dtype=tf.string, name="texto")
salida_vectorizador = vectorizador_persistible(entrada_vectorizador)
modelo_vectorizador = keras.Model(
    inputs=entrada_vectorizador,
    outputs=salida_vectorizador,
    name="vectorizador_textos",
)
modelo_vectorizador.save(ruta_vectorizador)

configuracion = {
    "umbral_clasificacion": UMBRAL,
    "criterio_umbral": "F1 máximo en el conjunto de validación",
    "clase_positiva": 1,
    "decision_clase_positiva": "REVISIÓN",
    "decision_clase_negativa": "PUBLICAR",
    "max_tokens": MAX_TOKENS,
    "max_sequence_length": MAX_SEQUENCE_LENGTH,
}
with ruta_configuracion.open("w", encoding="utf-8") as archivo_configuracion:
    json.dump(configuracion, archivo_configuracion, ensure_ascii=False, indent=2)

print("\nArtefactos guardados:")
print("Modelo:", ruta_modelo)
print("Vectorizador:", ruta_vectorizador)
print("Configuración y umbral:", ruta_configuracion)
"""
