
import numpy as np
import pandas as pd
import joblib

from scipy.sparse import load_npz

from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix,
    classification_report,
    roc_auc_score,
    average_precision_score
)

# ============================================================
# 1. CARGAR LA CONFIGURACIÓN
# ============================================================

from configuracion_modelo import UMBRAL

# ============================================================
# 2. CARGAR EL MODELO ENTRENADO
# ============================================================

modelo = joblib.load("LR_balanced.pkl")

# ============================================================
# 3. CARGAR EL CONJUNTO DE PRUEBA
# ============================================================

# Matriz TF-IDF del conjunto de prueba.
X_test_tfidf = load_npz("X_test_tfidf.npz")

# Etiquetas reales.
df_test = pd.read_csv("test.csv")
y_test = df_test["etiqueta"]

# Comprobamos que las dimensiones coincidan.
if X_test_tfidf.shape[0] != len(y_test):
    raise ValueError(
        "La cantidad de registros de X_test y y_test no coincide."
    )

# ============================================================
# 4. OBTENER LAS PROBABILIDADES
# ============================================================

# Calculamos las probabilidades para cada clase.
probabilidades = modelo.predict_proba(X_test_tfidf)

# Identificamos la columna correspondiente a la clase 1.
indice_clase_1 = list(modelo.classes_).index(1)

# Extraemos las probabilidades de la clase 1.
probabilidades_clase_1 = probabilidades[:, indice_clase_1]

# ============================================================
# 5. APLICAR EL UMBRAL SELECCIONADO
# ============================================================

# Si la probabilidad es >= 0.30, enviamos a revisión.
y_pred = (probabilidades_clase_1 >= UMBRAL).astype(int)

# ============================================================
# 6. CALCULAR LAS MÉTRICAS
# ============================================================

accuracy = accuracy_score(y_test, y_pred)

precision = precision_score(
    y_test, y_pred, zero_division=0
)

recall = recall_score(
    y_test, y_pred, zero_division=0
)

f1 = f1_score(
    y_test, y_pred, zero_division=0
)

# Métricas basadas en las probabilidades.
roc_auc = roc_auc_score(y_test, probabilidades_clase_1)

average_precision = average_precision_score(
    y_test, probabilidades_clase_1
)

# ============================================================
# 7. MATRIZ DE CONFUSIÓN
# ============================================================

tn, fp, fn, tp = confusion_matrix(
    y_test,
    y_pred,
    labels=[0, 1]
).ravel()

# Cantidad total de publicaciones enviadas a revisión.
revisiones = tp + fp

# Porcentaje de publicaciones enviadas a revisión.
porcentaje_revision = revisiones / len(y_test) * 100

# ============================================================
# 8. MOSTRAR LOS RESULTADOS
# ============================================================

print("\n============================================")
print(" EVALUACIÓN FINAL DEL MODELO")
print("============================================")

print(f"Modelo: LR_balanced")
print(f"Umbral: {UMBRAL}")

print("\n--- MÉTRICAS GENERALES ---")

print(f"Accuracy:          {accuracy:.4f}")
print(f"Precision:         {precision:.4f}")
print(f"Recall:            {recall:.4f}")
print(f"F1-score:          {f1:.4f}")
print(f"ROC-AUC:           {roc_auc:.4f}")
print(f"Average Precision: {average_precision:.4f}")

print("\n--- MATRIZ DE CONFUSIÓN ---")

print(f"Verdaderos negativos (TN): {tn}")
print(f"Falsos positivos (FP):     {fp}")
print(f"Falsos negativos (FN):     {fn}")
print(f"Verdaderos positivos (TP): {tp}")

print("\n--- CARGA DE MODERACIÓN ---")

print(f"Total de publicaciones: {len(y_test)}")
print(f"Publicaciones a revisar: {revisiones}")
print(f"Porcentaje de revisión: {porcentaje_revision:.2f}%")

print("\n--- REPORTE DE CLASIFICACIÓN ---")

print(
    classification_report(
        y_test,
        y_pred,
        labels=[0, 1],
        target_names=["Permitido", "Requiere revisión"],
        zero_division=0
    )
)

# ============================================================
# 9. GUARDAR LOS RESULTADOS
# ============================================================

resultados = pd.DataFrame([{
    "modelo": "LR_balanced",
    "umbral": UMBRAL,
    "accuracy": accuracy,
    "precision": precision,
    "recall": recall,
    "f1": f1,
    "roc_auc": roc_auc,
    "average_precision": average_precision,
    "TN": tn,
    "FP": fp,
    "FN": fn,
    "TP": tp,
    "revisiones": revisiones,
    "porcentaje_revision": porcentaje_revision
}])

resultados.to_csv(
    "evaluacion_final.csv",
    index=False
)

print("\nResultados guardados en evaluacion_final.csv")
