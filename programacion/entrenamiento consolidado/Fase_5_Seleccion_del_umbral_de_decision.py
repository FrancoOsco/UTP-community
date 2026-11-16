
import numpy as np
import pandas as pd
import joblib

from scipy.sparse import load_npz
from sklearn.metrics import (
    confusion_matrix,
    accuracy_score,
    precision_score,
    recall_score,
    f1_score
)

# ============================================================
# 1. CARGAR EL MODELO Y LOS DATOS DE VALIDACIÓN
# ============================================================

# Cargamos el modelo balanceado que seleccionamos.
modelo = joblib.load("LR_balanced.pkl")

# Cargamos la matriz TF-IDF de validación.
X_val_tfidf = load_npz("X_val_tfidf.npz")

# Cargamos las etiquetas reales.
df_val = pd.read_csv("validation.csv")
y_val = df_val["etiqueta"]

# ============================================================
# 2. OBTENER LAS PROBABILIDADES
# ============================================================

# Obtenemos la probabilidad que el modelo asigna
# a cada clase para cada publicación.
probabilidades = modelo.predict_proba(X_val_tfidf)

# Identificamos la columna correspondiente a la clase 1.
indice_clase_1 = list(modelo.classes_).index(1)

# Extraemos las probabilidades de la clase 1.
probabilidades_clase_1 = probabilidades[:, indice_clase_1]

# ============================================================
# 3. DEFINIR LOS UMBRALES QUE VAMOS A EVALUAR
# ============================================================

# Evaluaremos umbrales desde 0.10 hasta 0.90,
# avanzando en intervalos de 0.05.
umbrales = np.round(np.arange(0.10, 0.91, 0.05), 2)

# ============================================================
# 4. EVALUAR CADA UMBRAL
# ============================================================

resultados = []

for umbral in umbrales:

    # Convertimos las probabilidades en predicciones.
    # Si la probabilidad es >= al umbral, predice clase 1.
    y_pred = (probabilidades_clase_1 >= umbral).astype(int)

    # Calculamos la matriz de confusión.
    tn, fp, fn, tp = confusion_matrix(
        y_val,
        y_pred,
        labels=[0, 1]
    ).ravel()

    # Calculamos las métricas.
    accuracy = accuracy_score(y_val, y_pred)

    precision = precision_score(
        y_val, y_pred, zero_division=0
    )

    recall = recall_score(
        y_val, y_pred, zero_division=0
    )

    f1 = f1_score(
        y_val, y_pred, zero_division=0
    )

    # Cantidad total de publicaciones enviadas a revisión.
    revisiones = tp + fp

    # Guardamos los resultados de este umbral.
    resultados.append({
        "umbral": umbral,
        "accuracy": accuracy,
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "TN": tn,
        "FP": fp,
        "FN": fn,
        "TP": tp,
        "revisiones": revisiones,
        "porcentaje_revision": revisiones / len(y_val) * 100
    })

# ============================================================
# 5. MOSTRAR Y GUARDAR LOS RESULTADOS
# ============================================================

df_resultados = pd.DataFrame(resultados)

# Mostramos las métricas principales.
print("\nRESULTADOS DE EVALUACIÓN DE UMBRALES")
print(
    df_resultados.to_string(
        index=False,
        formatters={
            "accuracy": "{:.4f}".format,
            "precision": "{:.4f}".format,
            "recall": "{:.4f}".format,
            "f1": "{:.4f}".format,
            "porcentaje_revision": "{:.2f}%".format
        }
    )
)

# Guardamos los resultados para analizarlos posteriormente.
df_resultados.to_csv(
    "resultados_umbrales.csv",
    index=False
)

print("\nResultados guardados en resultados_umbrales.csv")