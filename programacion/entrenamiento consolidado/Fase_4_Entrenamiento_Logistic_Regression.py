
import pandas as pd
import joblib
import time

from scipy.sparse import load_npz

from sklearn.linear_model import LogisticRegression

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

# ============================================================
# 1. CARGAR LAS MATRICES TF-IDF
# ============================================================

# Recuperamos las matrices numéricas que generamos en la fase 3.
# Cada fila representa una publicación y cada columna un término
# o combinación de términos del vocabulario TF-IDF.

X_train_tfidf = load_npz("X_train_tfidf.npz")
X_val_tfidf = load_npz("X_val_tfidf.npz")

# No cargamos X_test_tfidf porque todavía no debemos utilizar
# el conjunto de prueba durante la selección del modelo.


# ============================================================
# 2. CARGAR LAS ETIQUETAS
# ============================================================

# Las matrices TF-IDF contienen los textos transformados,
# pero necesitamos las etiquetas reales para entrenar y evaluar.

train = pd.read_csv("train.csv")
validation = pd.read_csv("validation.csv")

y_train = train["etiqueta"]
y_val = validation["etiqueta"]


# ============================================================
# 3. VERIFICAR LAS DIMENSIONES
# ============================================================

# Comprobamos que cada matriz tenga la misma cantidad de filas
# que su correspondiente conjunto de etiquetas.

assert X_train_tfidf.shape[0] == len(y_train)
assert X_val_tfidf.shape[0] == len(y_val)

print("========== DIMENSIONES ==========")

print("Entrenamiento:", X_train_tfidf.shape)
print("Validación:", X_val_tfidf.shape)


# ============================================================
# 4. DEFINIR LAS CONFIGURACIONES A COMPARAR
# ============================================================

# Cada configuración representa un experimento diferente.
#
# class_weight=None:
# No modifica el peso de las clases.
#
# class_weight="balanced":
# Calcula automáticamente pesos inversamente proporcionales
# a la frecuencia de cada clase.
#
# C:
# Controla la intensidad de la regularización.
# Un C menor implica una regularización más fuerte.

configuraciones = {

    "LR_baseline": {
        "class_weight": None,
        "C": 1.0
    },

    "LR_balanced": {
        "class_weight": "balanced",
        "C": 1.0
    },

    "LR_balanced_C05": {
        "class_weight": "balanced",
        "C": 0.5
    }

}


# ============================================================
# 5. ENTRENAR Y EVALUAR CADA CONFIGURACIÓN
# ============================================================

# Aquí almacenaremos los resultados de cada experimento.
resultados = []

# También guardaremos los modelos entrenados para reutilizarlos.
modelos = {}

for nombre, parametros in configuraciones.items():

    print(f"\n========== {nombre} ==========")

    # --------------------------------------------------------
    # CREAR EL MODELO
    # --------------------------------------------------------

    modelo = LogisticRegression(
        max_iter=1000,
        random_state=42,
        class_weight=parametros["class_weight"],
        C=parametros["C"]
    )

    # --------------------------------------------------------
    # ENTRENAR EL MODELO
    # --------------------------------------------------------

    # fit aprende los coeficientes que relacionan las palabras
    # representadas por TF-IDF con las etiquetas 0 y 1.

    inicio = time.time()

    modelo.fit(X_train_tfidf, y_train)

    tiempo_entrenamiento = time.time() - inicio

    # --------------------------------------------------------
    # REALIZAR PREDICCIONES SOBRE VALIDACIÓN
    # --------------------------------------------------------

    # predict_proba devuelve dos probabilidades por publicación:
    # columna 0: probabilidad de clase 0.
    # columna 1: probabilidad de clase 1.

    probabilidades = modelo.predict_proba(X_val_tfidf)[:, 1]

    # Utilizamos inicialmente el umbral predeterminado de 0.5.
    # Si la probabilidad de clase 1 es >= 0.5, predice revisión.

    UMBRAL = 0.50

    predicciones = (probabilidades >= UMBRAL).astype(int)

    # --------------------------------------------------------
    # CALCULAR MÉTRICAS
    # --------------------------------------------------------

    accuracy = accuracy_score(y_val, predicciones)

    # precision: de todas las publicaciones marcadas para revisión,
    # ¿cuántas realmente pertenecen a la clase 1?

    precision = precision_score(
        y_val,
        predicciones,
        zero_division=0
    )

    # recall: de todas las publicaciones que realmente requieren
    # revisión, ¿cuántas logró detectar el modelo?

    recall = recall_score(
        y_val,
        predicciones,
        zero_division=0
    )

    # f1: combina precision y recall en una sola métrica.

    f1 = f1_score(
        y_val,
        predicciones,
        zero_division=0
    )

    # ROC-AUC mide la capacidad de ordenar positivos por encima
    # de negativos considerando distintos umbrales.

    roc_auc = roc_auc_score(y_val, probabilidades)

    # Average Precision resume el comportamiento de precision
    # y recall a través de distintos umbrales.

    average_precision = average_precision_score(
        y_val,
        probabilidades
    )

    # --------------------------------------------------------
    # MATRIZ DE CONFUSIÓN
    # --------------------------------------------------------

    # La matriz tiene este orden:
    #
    # [[TN, FP],
    #  [FN, TP]]
    #
    # TN: permitidos correctamente identificados.
    # FP: permitidos marcados para revisión.
    # FN: publicaciones que requerían revisión y no se detectaron.
    # TP: publicaciones que requerían revisión y sí se detectaron.

    matriz = confusion_matrix(
        y_val,
        predicciones,
        labels=[0, 1]
    )

    TN, FP, FN, TP = matriz.ravel()

    # --------------------------------------------------------
    # GUARDAR RESULTADOS
    # --------------------------------------------------------

    resultados.append({
        "modelo": nombre,
        "C": parametros["C"],
        "class_weight": parametros["class_weight"],
        "accuracy": accuracy,
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "roc_auc": roc_auc,
        "average_precision": average_precision,
        "TN": TN,
        "FP": FP,
        "FN": FN,
        "TP": TP,
        "revisiones": TP + FP,
        "tiempo_entrenamiento": tiempo_entrenamiento
    })

    # Guardamos el modelo entrenado en memoria.
    modelos[nombre] = modelo

    # --------------------------------------------------------
    # MOSTRAR RESULTADOS DEL EXPERIMENTO
    # --------------------------------------------------------

    print(f"Tiempo de entrenamiento: {tiempo_entrenamiento:.2f} segundos")

    print(f"Accuracy:          {accuracy:.4f}")
    print(f"Precision:         {precision:.4f}")
    print(f"Recall:            {recall:.4f}")
    print(f"F1:                {f1:.4f}")
    print(f"ROC-AUC:           {roc_auc:.4f}")
    print(f"Average Precision: {average_precision:.4f}")

    print("\nMatriz de confusión:")
    print(matriz)

    print("\nReporte de clasificación:")
    print(
        classification_report(
            y_val,
            predicciones,
            target_names=["Permitido", "Requiere revisión"],
            zero_division=0
        )
    )


# ============================================================
# 6. COMPARAR LOS MODELOS
# ============================================================

df_resultados = pd.DataFrame(resultados)

# Ordenamos los modelos por F1 de la clase positiva.
# Esto permite observar inicialmente cuáles equilibran mejor
# precision y recall.

df_resultados = df_resultados.sort_values(
    by="f1",
    ascending=False
)

print("\n========== COMPARACIÓN FINAL ==========")

columnas = [
    "modelo",
    "accuracy",
    "precision",
    "recall",
    "f1",
    "roc_auc",
    "average_precision",
    "FP",
    "FN",
    "revisiones"
]

print(
    df_resultados[columnas].to_string(index=False)
)

# Exportamos los resultados para poder analizarlos en Excel.
df_resultados.to_csv(
    "resultados_entrenamiento.csv",
    index=False,
    encoding="utf-8-sig"
)


# ============================================================
# 7. GUARDAR LOS MODELOS ENTRENADOS
# ============================================================

# Guardamos cada modelo en un archivo independiente.
# Así no tendremos que volver a entrenarlos para utilizarlos.

for nombre, modelo in modelos.items():

    joblib.dump(
        modelo,
        f"{nombre}.pkl"
    )

print("\nEntrenamiento finalizado.")
print("Resultados guardados en resultados_entrenamiento.csv")
