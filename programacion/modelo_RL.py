import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer

from sklearn.linear_model import LogisticRegression

from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix,
    classification_report
)

from sklearn.metrics import roc_auc_score, average_precision_score

# FASE 1. Preparación del dataset

# PASO 1. Cargar el archivo CSV con Pandas.

# Cargar el dataset
df = pd.read_csv("datasets/daset_offendes.csv", sep=";",
                 encoding="utf-8", encoding_errors="replace")

# Mostrar las primeras filas
print(df.head())

# PASO 2. Inspeccionar su estructura.

# Dimensiones del dataset
print("Dimensiones:", df.shape)

# Nombres de las columnas
print("\nColumnas:")
print(df.columns)

# Tipos de datos
print("\nTipos de datos:")
print(df.dtypes)

# Información general
print("\nInformación general:")
df.info()

# PASO 3. Detectar valores nulos.

# Contar valores nulos por columna
print("Valores nulos:")
print(df.isnull().sum())

# Total de valores nulos
print("\nTotal de valores nulos:", df.isnull().sum().sum())

# Detectar textos vacíos o compuestos únicamente por espacios
for columna in ["titulo", "descripcion"]:
    vacios = df[columna].fillna("").str.strip().eq("").sum()
    print(f"Textos vacíos en {columna}: {vacios}")

# PASO 4. Detectar registros duplicados.

# Contar filas duplicadas exactas (sin contar la primera aparición)
duplicados_exactos = df.duplicated().sum()

print("Duplicados exactos:", duplicados_exactos)

# Mostrar todas las filas que pertenecen a grupos duplicados
df_duplicados = df[df.duplicated(keep=False)]

print("\nRegistros duplicados:")
print(df_duplicados.sort_values(
    by=["titulo", "descripcion", "etiqueta"]
))

# Agrupar publicaciones por título y descripción
grupos_texto = df.groupby(
    ["titulo", "descripcion"],
    dropna=False
)["etiqueta"].nunique()

# Identificar textos que tienen más de una etiqueta
textos_inconsistentes = grupos_texto[grupos_texto > 1]

print(
    "\nPublicaciones con etiquetas diferentes:",
    len(textos_inconsistentes)
)

print(textos_inconsistentes)

# PASO 5. Analizar la distribución de etiquetas.

# Contar registros por etiqueta
distribucion = df["etiqueta"].value_counts().sort_index()

print("\nDistribución de etiquetas:")
print(distribucion)

# Calcular porcentajes
porcentajes = df["etiqueta"].value_counts(
    normalize=True
).sort_index() * 100

print("\nPorcentaje por etiqueta:")
print(porcentajes)

distribucion.plot(
    kind="bar",
    xlabel="Etiqueta",
    ylabel="Cantidad de publicaciones",
    title="Distribución de etiquetas del dataset",
    rot=0
)

# plt.xticks([0, 1], ["Permitido (0)", "Requiere revisión (1)"])
# plt.tight_layout()
# plt.show()

print("\nEtiquetas existentes:", sorted(df["etiqueta"].unique()))

# PASO 6. Dividir los datos en entrenamiento, validación y prueba.

# Separar 70 % para entrenamiento y 30 % para división posterior
df_train, df_temporal = train_test_split(
    df,
    test_size=0.30,
    random_state=42,
    stratify=df["etiqueta"]
)

# Separar el 30 % restante en validación y prueba
df_val, df_test = train_test_split(
    df_temporal,
    test_size=0.50,
    random_state=42,
    stratify=df_temporal["etiqueta"]
)

print("\nDimensiones de los conjuntos:")
print("Entrenamiento:", df_train.shape)
print("Validación:", df_val.shape)
print("Prueba:", df_test.shape)

print("\nTotal de registros:",
      len(df_train) + len(df_val) + len(df_test))

for nombre, conjunto in [
    ("Original", df),
    ("Entrenamiento", df_train),
    ("Validación", df_val),
    ("Prueba", df_test)
]:
    print(f"\n{nombre}:")
    print(
        conjunto["etiqueta"]
        .value_counts(normalize=True)
        .sort_index() * 100
    )

# FASE 2. Primer modelo: Machine Learning

# PASO 1. Unificar título y descripción.

# funcion para unificar título y descripción
def unificar_titulo_descripcion(df, columnas_texto=("titulo", "descripcion")):
    """Crea la columna 'texto' combinando titulo y descripcion, tolerando nulos."""
    df = df.copy()
    columnas = [c for c in columnas_texto if c in df.columns]

    for c in columnas:
        df[c] = (
            df[c].astype("string").fillna("").str.strip()
            .replace({"nan": "", "NaN": "", "None": "", "null": ""})
        )

    def combinar(fila):
        partes = []
        for c in columnas:
            if fila[c] and fila[c] not in partes:
                partes.append(fila[c])
        return " ".join(partes)

    df["texto"] = df.apply(combinar, axis=1)
    return df

# función para elimnar filas inválidas
def eliminar_filas_invalidas(df):
    """Elimina filas sin texto o sin etiqueta."""
    antes = len(df)
    df = df.dropna(subset=["etiqueta"])
    df = df[df["texto"] != ""]
    print(f"Filas eliminadas: {antes - len(df)} | Dimensiones finales: {df.shape}")
    return df

df_train = eliminar_filas_invalidas(unificar_titulo_descripcion(df_train))
df_val   = eliminar_filas_invalidas(unificar_titulo_descripcion(df_val))
df_test  = eliminar_filas_invalidas(unificar_titulo_descripcion(df_test))

print(df_train[["titulo", "descripcion", "texto"]].head())

print("\nEntrenamiento:", df_train.shape)
print("Validación:", df_val.shape)
print("Prueba:", df_test.shape)

# PASO 2. Convertir los textos en vectores numéricos mediante TF-IDF.

# Separar características y etiquetas

X_train = df_train["texto"]
y_train = df_train["etiqueta"]

X_val = df_val["texto"]
y_val = df_val["etiqueta"]

X_test = df_test["texto"]
y_test = df_test["etiqueta"]

# Crear el vectorizador TF-IDF
vectorizador = TfidfVectorizer(
    lowercase=True,
    ngram_range=(1, 2)
)

# Aprender el vocabulario y calcular los pesos TF-IDF
X_train_tfidf = vectorizador.fit_transform(X_train)

# Transformar validación y prueba utilizando el mismo vocabulario
X_val_tfidf = vectorizador.transform(X_val)
X_test_tfidf = vectorizador.transform(X_test)

# Dimensiones de las matrices
print("\nDimensiones de las matrices TF-IDF:")

print("Entrenamiento:", X_train_tfidf.shape)
print("Validación:", X_val_tfidf.shape)
print("Prueba:", X_test_tfidf.shape)

# Tamaño del vocabulario
print("\nTamaño del vocabulario:")
print(len(vectorizador.vocabulary_))

# Mostrar algunas palabras del vocabulario
print("\nPrimeras 20 palabras:")
print(vectorizador.get_feature_names_out()[:20])

# PASO 3. Entrenar el modelo de Regresión Logística.

# Crear el modelo de Regresión Logística
modelo_lr = LogisticRegression(
    max_iter=1000,
    random_state=42
)

# Entrenar el modelo con los datos de entrenamiento
modelo_lr.fit(X_train_tfidf, y_train)

print("\nModelo de Regresión Logística entrenado correctamente.")

# PASO 4. Realizar predicciones.

# Predecir las etiquetas del conjunto de validación
y_val_pred = modelo_lr.predict(X_val_tfidf)

# Obtener las probabilidades estimadas
y_val_proba = modelo_lr.predict_proba(X_val_tfidf)

print("Primeras 10 predicciones:")
print(y_val_pred[:10])

print("\nProbabilidades de las primeras 10 publicaciones:")
print(y_val_proba[:10])

# PASO 5. Evaluar los resultados.

# Comparación directa de las primeras 10
print("\nReales:    ", y_val.values[:10])
print("Predichas: ", y_val_pred[:10])
print("¿Acertó?:  ", (y_val.values[:10] == y_val_pred[:10]))

# Métricas globales sobre todo el conjunto de validación
print("Accuracy:", accuracy_score(y_val, y_val_pred))

print("Precision:", precision_score(y_val, y_val_pred))
print("Recall:", recall_score(y_val, y_val_pred))
print("F1-score:", f1_score(y_val, y_val_pred))

print("\nReporte de clasificación:")
print(classification_report(
    y_val,
    y_val_pred,
    labels=[0, 1],
    target_names=["Permitido (0)", "Requiere revisión (1)"],
    zero_division=0
))

matriz = confusion_matrix(y_val, y_val_pred, labels=[0, 1])

print("Matriz de confusión:")
print(matriz)

# FASE 2.5. Diagnosticar problemas

# Paso 1. Examinar la distribución de probabilidades

# Probabilidad de pertenecer a la clase 1
probabilidades_revision = y_val_proba[:, 1]

print("\nEstadísticas de las probabilidades:")
print(pd.Series(probabilidades_revision).describe())

print("\nCantidad de predicciones según el umbral:")
for umbral in [0.5, 0.4, 0.3, 0.2, 0.1]:
    cantidad = (probabilidades_revision >= umbral).sum()
    print(f"Umbral {umbral}: {cantidad} publicaciones")

# Paso 2. Examinar la distribución de probabilidades

resultados = pd.DataFrame({
    "etiqueta_real": y_val.to_numpy(),
    "probabilidad_revision": probabilidades_revision
})

print(
    resultados.groupby("etiqueta_real")["probabilidad_revision"]
    .describe()
)

# Paso 3. evaluar la capacidad de discriminación

roc_auc = roc_auc_score(y_val, probabilidades_revision)
average_precision = average_precision_score(
    y_val,
    probabilidades_revision
)

print("\nROC-AUC:", roc_auc)
print("Average Precision:", average_precision)

print(
    "Proporción de clase positiva:",
    y_val.mean()
)

# FASE 2.6. Experimentar el segundo modelo

# Paso 1 — Entrenar el segundo modelo

modelo_lr_balanced = LogisticRegression(
    max_iter=1000,
    random_state=42,
    class_weight="balanced"
)

modelo_lr_balanced.fit(X_train_tfidf, y_train)

print("\nModelo balanceado entrenado correctamente.")

# Paso 2 — Obtener predicciones

y_val_pred_balanced = modelo_lr_balanced.predict(X_val_tfidf)

y_val_proba_balanced = modelo_lr_balanced.predict_proba(X_val_tfidf)

print("\nPrimeras 10 predicciones:")
print(y_val_pred_balanced[:10])

print("\nPrimeras 10 probabilidades:")
print(y_val_proba_balanced[:10])

# Paso 3 — Evaluar el segundo modelo

print("\nAccuracy:", accuracy_score(y_val, y_val_pred_balanced))

print("Precision:", precision_score(
    y_val, y_val_pred_balanced, zero_division=0
))

print("Recall:", recall_score(
    y_val, y_val_pred_balanced, zero_division=0
))

print("F1-score:", f1_score(
    y_val, y_val_pred_balanced, zero_division=0
))

print("\nReporte de clasificación:")

print(classification_report(
    y_val,
    y_val_pred_balanced,
    labels=[0, 1],
    target_names=["Permitido (0)", "Requiere revisión (1)"],
    zero_division=0
))

print("\nMatriz de confusión:")

print(confusion_matrix(
    y_val,
    y_val_pred_balanced,
    labels=[0, 1]
))

# Paso 4 — Evaluar la capacidad de discriminación

print()

probabilidades_balanced = y_val_proba_balanced[:, 1]

print(
    "ROC-AUC:",
    roc_auc_score(y_val, probabilidades_balanced)
)

print(
    "Average Precision:",
    average_precision_score(y_val, probabilidades_balanced)
)

# FASE 2.7. Experimentar el segundo modelo

# Paso 1 — Crear una función de evaluación

def evaluar_umbral(y_real, probabilidades, umbral):
    predicciones = (probabilidades >= umbral).astype(int)

    tn, fp, fn, tp = confusion_matrix(
        y_real,
        predicciones,
        labels=[0, 1]
    ).ravel()

    return {
        "Umbral": umbral,
        "Accuracy": accuracy_score(y_real, predicciones),
        "Precision": precision_score(
            y_real, predicciones, zero_division=0
        ),
        "Recall": recall_score(
            y_real, predicciones, zero_division=0
        ),
        "F1": f1_score(
            y_real, predicciones, zero_division=0
        ),
        "TP": tp,
        "FP": fp,
        "FN": fn,
        "TN": tn,
        "Revisiones": tp + fp
    }

# Paso 2 — Comparar diferentes umbrales

umbrales = [0.10, 0.15, 0.20, 0.25, 0.30,
            0.35, 0.40, 0.45, 0.50]

resultados_original = []
resultados_balanced = []

for umbral in umbrales:

    resultados_original.append(
        evaluar_umbral(
            y_val,
            y_val_proba[:, 1],
            umbral
        )
    )

    resultados_balanced.append(
        evaluar_umbral(
            y_val,
            y_val_proba_balanced[:, 1],
            umbral
        )
    )

df_original = pd.DataFrame(resultados_original)
df_balanced = pd.DataFrame(resultados_balanced)

print("\nMODELO ORIGINAL")
print(df_original.round(3).to_string(index=False))

print("\nMODELO BALANCEADO")
print(df_balanced.round(3).to_string(index=False))

# FASE 2.8. optimización del modelo balanceado

# Paso 1. Experimentar con umbrales más precisos

probabilidades_balanced = y_val_proba_balanced[:, 1]

umbrales = np.arange(0.20, 0.501, 0.02)

resultados = []

for umbral in umbrales:
    resultado = evaluar_umbral(
        y_val,
        probabilidades_balanced,
        umbral
    )

    resultados.append(resultado)

df_umbrales = pd.DataFrame(resultados)

print(df_umbrales.round(3).to_string())

candidatos = df_umbrales[
    df_umbrales["Recall"] >= 0.90
].sort_values(
    by="Revisiones",
    ascending=True
)

print(candidatos.round(3).to_string())

plt.figure(figsize=(10, 5))

plt.plot(
    df_umbrales["Umbral"],
    df_umbrales["Recall"],
    marker="o",
    label="Recall"
)

plt.xlabel("Umbral")
plt.ylabel("Recall")
plt.title("Recall según el umbral - Modelo balanceado")
plt.grid()
plt.legend()

# plt.show()

plt.figure(figsize=(10, 5))

plt.plot(
    df_umbrales["Umbral"],
    df_umbrales["Revisiones"],
    marker="o"
)

plt.xlabel("Umbral")
plt.ylabel("Cantidad de publicaciones")
plt.title("Publicaciones enviadas a revisión según el umbral")
plt.grid()

# plt.show()

# PASO 2. analizar los errores del modelo balanceado

# Umbral candidato
umbral = 0.30

# Clasificaciones con el modelo balanceado
predicciones = (probabilidades_balanced >= umbral).astype(int)

# Crear DataFrame de análisis
df_errores = pd.DataFrame({
    "texto": X_val.values,
    "etiqueta_real": y_val.values,
    "probabilidad": probabilidades_balanced,
    "prediccion": predicciones
})

# Identificar errores
falsos_positivos = df_errores[
    (df_errores["etiqueta_real"] == 0) &
    (df_errores["prediccion"] == 1)
]

falsos_negativos = df_errores[
    (df_errores["etiqueta_real"] == 1) &
    (df_errores["prediccion"] == 0)
]

print("\nFalsos positivos:", len(falsos_positivos))
print("Falsos negativos:", len(falsos_negativos))

print("\n=== FALSOS POSITIVOS ===")

print(
    falsos_positivos
    .sort_values("probabilidad", ascending=False)
    .head(15).to_string()
)

print("=== FALSOS NEGATIVOS ===")

print(
    falsos_negativos
    .sort_values("probabilidad", ascending=True)
    .head(15).to_string()
)