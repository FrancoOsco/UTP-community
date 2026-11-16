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

from scipy.sparse import csr_matrix

from sklearn.feature_extraction.text import TfidfVectorizer

# ============================================================
# 2. CONFIGURACIÓN DE SEMILLAS
# ============================================================

SEED = 42

np.random.seed(SEED)
random.seed(SEED)
tf.random.set_seed(SEED)

# ==========================================================
# FASE 1: CARGA DE DATOS
# ==========================================================

# Cargar los tres conjuntos
df_train = pd.read_csv("entrenamiento consolidado/train.csv")
df_val = pd.read_csv("entrenamiento consolidado/validation.csv")
df_test = pd.read_csv("entrenamiento consolidado/test.csv")

# Extraer las características (texto) y las etiquetas
X_train = df_train["texto"]
y_train = df_train["etiqueta"]

X_val = df_val["texto"]
y_val = df_val["etiqueta"]

X_test = df_test["texto"]
y_test = df_test["etiqueta"]

print("\n=== DATOS CARGADOS ===")
print(f"Entrenamiento: {len(df_train)} registros")
print(f"Validación:    {len(df_val)} registros")
print(f"Prueba:        {len(df_test)} registros")
print(f"Total:         {len(df_train) + len(df_val) + len(df_test)} registros")

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

    print("Etiquetas únicas:", sorted(df["etiqueta"].unique()))

# convertimos las etiquetas a enteros para facilitar su utilización en Keras
y_train = y_train.astype("int32")
y_val = y_val.astype("int32")
y_test = y_test.astype("int32")

# COMPROBACIÓN DE INTERSECCIONES

textos_train = set(X_train)
textos_val = set(X_val)
textos_test = set(X_test)

print("\n=== REGISTROS COMPARTIDOS ===")

print("Train - Validation:", len(textos_train & textos_val))
print("Train - Test:", len(textos_train & textos_test))
print("Validation - Test:", len(textos_val & textos_test))

# RESUMEN FINAL

print("\n=== DIMENSIONES FINALES ===")

print("X_train:", X_train.shape)
print("y_train:", y_train.shape)

print("X_val:", X_val.shape)
print("y_val:", y_val.shape)

print("X_test:", X_test.shape)
print("y_test:", y_test.shape)

print("\n¡Datos cargados y verificados!")

# Fase 2 — Representación de texto y entrada de la red

# 1. RECUPERAR EL VECTORIZADOR

vectorizador_mlp = TfidfVectorizer(
    lowercase=True,
    ngram_range=(1, 2),
    min_df=2,
    max_features=50000,
    dtype=np.float32
)

print("Nuevo vectorizador creado.")

# 2. TRANSFORMACIÓN TF-IDF

# Ajustamos y transformamos únicamente entrenamiento
X_train_tfidf = vectorizador_mlp.fit_transform(X_train)

# Transformamos validación y prueba sin reajustar el vectorizador
X_val_tfidf = vectorizador_mlp.transform(X_val)
X_test_tfidf = vectorizador_mlp.transform(X_test)

print("\n=== TRANSFORMACIÓN TF-IDF COMPLETADA ===")

print("Características:", len(vectorizador_mlp.vocabulary_))

print("\nDimensiones:")
print("Train:", X_train_tfidf.shape)
print("Validation:", X_val_tfidf.shape)
print("Test:", X_test_tfidf.shape)

print("\nTipo de matrices:")
print("Train:", type(X_train_tfidf))
print("Validation:", type(X_val_tfidf))
print("Test:", type(X_test_tfidf))

print("\nDensidad:")
densidad = (
    X_train_tfidf.nnz /
    (X_train_tfidf.shape[0] * X_train_tfidf.shape[1])
) * 100

print(f"{densidad:.4f}%")

# 4. VERIFICACIÓN FINAL

assert X_train_tfidf.shape[0] == len(y_train)
assert X_val_tfidf.shape[0] == len(y_val)
assert X_test_tfidf.shape[0] == len(y_test)

assert X_train_tfidf.shape[1] == X_val_tfidf.shape[1]
assert X_train_tfidf.shape[1] == X_test_tfidf.shape[1]

print("\n¡Verificación completada!")
print("Características TF-IDF:", X_train_tfidf.shape[1])
print("Las tres matrices tienen el mismo número de características.")
print("Las dimensiones coinciden con sus respectivas etiquetas.")

# Guardar el vectorizador

joblib.dump(
    vectorizador_mlp,
    "vectorizador_tfidf_mlp.pkl"
)

print("Vectorizador MLP guardado.")

# Fase 3 — Conversión a tensores dispersos de TensorFlow

# 1. Función de conversión

def convertir_a_sparse_tensor(matriz_csr):

    matriz_coo = matriz_csr.tocoo()

    indices = np.column_stack((
        matriz_coo.row,
        matriz_coo.col
    ))

    tensor = tf.SparseTensor(
        indices=indices,
        values=matriz_coo.data.astype(np.float32),
        dense_shape=matriz_coo.shape
    )

    tensor = tf.sparse.reorder(tensor)

    return tensor

# 2. Convertir los tres conjuntos

X_train_tf = convertir_a_sparse_tensor(X_train_tfidf)
X_val_tf = convertir_a_sparse_tensor(X_val_tfidf)
X_test_tf = convertir_a_sparse_tensor(X_test_tfidf)

print("Conversión completada.")

# 3. Verificar los resultados

print("=== VERIFICACIÓN DE SPARSE TENSORS ===")

for nombre, tensor in [
    ("Train", X_train_tf),
    ("Validation", X_val_tf),
    ("Test", X_test_tf)
]:

    print(f"\n{nombre}")
    print("Forma:", tensor.shape)
    print("Tipo:", tensor.dtype)
    print("Valores no nulos:", tensor.values.shape[0])

# 4. Comprobar compatibilidad con Keras

modelo_prueba = keras.Sequential([
    keras.Input(
        shape=(50000,),
        sparse=True,
        dtype="float32"
    ),
    keras.layers.Dense(8, activation="relu"),
    keras.layers.Dense(1, activation="sigmoid")
])

lote_prueba = tf.sparse.slice(
    X_train_tf,
    start=[0, 0],
    size=[4, 50000]
)

salida_prueba = modelo_prueba(
    lote_prueba,
    training=False
)

print("Forma de salida:", salida_prueba.shape)
print("Valores:", salida_prueba.numpy())

# Fase 4 — Construcción y compilación de la MLP

# 1. Definir la arquitectura

num_caracteristicas = X_train_tf.shape[1]

modelo = keras.Sequential([
    keras.Input(
        shape=(num_caracteristicas,),
        sparse=True,
        dtype="float32"
    ),

    keras.layers.Dense(32, activation="relu"),
    keras.layers.Dropout(0.5),

    keras.layers.Dense(16, activation="relu"),
    keras.layers.Dropout(0.4),

    keras.layers.Dense(1, activation="sigmoid")
])

print("Nueva arquitectura MLP construida.")

# 2. Compilar el modelo

modelo.compile(
    optimizer=keras.optimizers.Adam(
        learning_rate=0.0003
    ),

    loss=keras.losses.BinaryCrossentropy(),

    metrics=[
        keras.metrics.BinaryAccuracy(name="accuracy"),
        keras.metrics.Precision(name="precision"),
        keras.metrics.Recall(name="recall"),
        keras.metrics.AUC(name="roc_auc")
    ]
)

print("Modelo compilado correctamente.")

# 3. Inspeccionar la arquitectura

modelo.summary()

# Fase 5 — Entrenamiento de la MLP

# 1. Calcular los pesos de clase

cantidad_0 = np.sum(y_train == 0)
cantidad_1 = np.sum(y_train == 1)

peso_0 = len(y_train) / (2 * cantidad_0)
peso_1 = len(y_train) / (2 * cantidad_1)

class_weights = {
    0: peso_0,
    1: peso_1
}

print("\nPesos de clase:", class_weights)

# 2. Preparar los datasets

# Convertir etiquetas de Pandas a NumPy float32

y_train_tf = y_train.to_numpy(
    dtype=np.float32
).reshape(-1, 1)

y_val_tf = y_val.to_numpy(
    dtype=np.float32
).reshape(-1, 1)

y_test_tf = y_test.to_numpy(
    dtype=np.float32
).reshape(-1, 1)

print("Etiquetas convertidas correctamente.")

print("Train:", y_train_tf.shape)
print("Validation:", y_val_tf.shape)
print("Test:", y_test_tf.shape)

# Dataset de entrenamiento
dataset_train = tf.data.Dataset.from_tensor_slices(
    (X_train_tf, y_train_tf)
)

dataset_train = dataset_train.shuffle(
    buffer_size=10000,
    seed=42,
    reshuffle_each_iteration=True
)

def agregar_peso(x, y):
    peso = tf.where(
        tf.equal(tf.squeeze(y), 0.0),
        tf.constant(peso_0, dtype=tf.float32),
        tf.constant(peso_1, dtype=tf.float32)
    )

    return x, y, peso

dataset_train = dataset_train.map(
    agregar_peso,
    num_parallel_calls=tf.data.AUTOTUNE
)

dataset_train = dataset_train.batch(64)
dataset_train = dataset_train.prefetch(tf.data.AUTOTUNE)


# Dataset de validación
dataset_val = tf.data.Dataset.from_tensor_slices(
    (X_val_tf, y_val_tf)
)

dataset_val = dataset_val.batch(64)
dataset_val = dataset_val.prefetch(tf.data.AUTOTUNE)

print("Datasets preparados correctamente.")

# 3. Configurar los callbacks

# EarlyStopping
early_stopping = keras.callbacks.EarlyStopping(
    monitor="val_loss",
    patience=4,
    restore_best_weights=True,
    verbose=1
)

# ReduceLROnPlateau
reducir_lr = keras.callbacks.ReduceLROnPlateau(
    monitor="val_loss",
    factor=0.5,
    patience=2,
    min_lr=0.000001,
    verbose=1
)

# 4. Entrenar la nueva MLP

historial = modelo.fit(
    dataset_train,
    validation_data=dataset_val,
    epochs=15,
    callbacks=[
        reducir_lr,
        early_stopping
    ],
    verbose=1
)

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

# Fase 6: ajustar el umbral de clasificación

# 1. Obtener las probabilidades

# Dataset de validación sin etiquetas, para realizar predicciones
dataset_val_pred = tf.data.Dataset.from_tensor_slices(
    X_val_tf
).batch(64)

# Obtener probabilidades
y_val_prob = modelo.predict(
    dataset_val_pred,
    verbose=1
).ravel()

print("\nPredicciones obtenidas:", len(y_val_prob))
print("Primeras probabilidades:", y_val_prob[:10])

# 2. Evaluar diferentes umbrales

from sklearn.metrics import (
    confusion_matrix,
    precision_score,
    recall_score,
    f1_score
)

resultados = []

for umbral in np.arange(0.10, 0.71, 0.05):

    predicciones = (y_val_prob >= umbral).astype(int)

    tn, fp, fn, tp = confusion_matrix(
        y_val,
        predicciones,
        labels=[0, 1]
    ).ravel()

    resultados.append({
        "Umbral": round(umbral, 2),
        "Precision": precision_score(
            y_val, predicciones, zero_division=0
        ),
        "Recall": recall_score(
            y_val, predicciones, zero_division=0
        ),
        "F1": f1_score(
            y_val, predicciones, zero_division=0
        ),
        "TP": tp,
        "FP": fp,
        "FN": fn,
        "TN": tn,
        "Revisiones": int(np.sum(predicciones == 1))
    })

df_resultados = pd.DataFrame(resultados)

print(df_resultados.round(4).to_string(index=False))

# Evaluación final de la MLP

from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    average_precision_score,
    confusion_matrix,
    classification_report
)

# Obtener probabilidades del conjunto de prueba
dataset_test_pred = tf.data.Dataset.from_tensor_slices(
    X_test_tf
).batch(64)

y_test_prob = modelo.predict(
    dataset_test_pred,
    verbose=1
).ravel()

# Aplicar umbral seleccionado en validación
UMBRAL_MLP = 0.20

y_test_pred = (
    y_test_prob >= UMBRAL_MLP
).astype(int)

# Calcular métricas
print("=== EVALUACIÓN FINAL MLP ===")

print("Accuracy:", accuracy_score(y_test, y_test_pred))
print("Precision:", precision_score(y_test, y_test_pred))
print("Recall:", recall_score(y_test, y_test_pred))
print("F1:", f1_score(y_test, y_test_pred))

print("ROC-AUC:", roc_auc_score(y_test, y_test_prob))
print("Average Precision:", average_precision_score(y_test, y_test_prob))

# Matriz de confusión
matriz = confusion_matrix(
    y_test,
    y_test_pred,
    labels=[0, 1]
)

tn, fp, fn, tp = matriz.ravel()

print("\nMatriz de confusión:")
print(matriz)

print("\nTN:", tn)
print("FP:", fp)
print("FN:", fn)
print("TP:", tp)

print("\nPublicaciones enviadas a revisión:", int(np.sum(y_test_pred == 1)))
print("Porcentaje enviado a revisión:", f"{np.mean(y_test_pred == 1) * 100:.2f}%")

print("\nReporte de clasificación:")
print(classification_report(y_test, y_test_pred))