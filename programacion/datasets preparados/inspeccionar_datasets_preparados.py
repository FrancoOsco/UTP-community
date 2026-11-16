import pandas as pd
import numpy as np

from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.metrics import roc_auc_score, accuracy_score, f1_score

from sklearn.metrics import (
    confusion_matrix,
    classification_report,
    precision_score,
    recall_score
)

# =====================================================
# 1. CONFIGURACIÓN DE LOS DATASETS
# =====================================================

# Solo necesitas agregar o modificar las rutas.
# Ajusta los nombres de columnas según cada archivo.

rutas_datasets = [
    {
        "nombre": "OffendES",
        "ruta": "datasets preparados/daset_offendes_preparado.csv",
        "columnas_texto": ["texto"],
        "columna_etiqueta": "etiqueta"
    },
    {
        "nombre": "SocialTOX",
        "ruta": "datasets preparados/dataset_necostox_preparado.csv",
        "columnas_texto": ["texto"],
        "columna_etiqueta": "etiqueta"
    },
    {
        "nombre": "ALIA",
        "ruta": "datasets preparados/dataset_alia_preparado.csv",
        "columnas_texto": ["texto"],
        "columna_etiqueta": "etiqueta"
    },
    {
        "nombre": "CLANDESTINO",
        "ruta": "datasets preparados/dataset_clandestino_preparado.csv",
        "columnas_texto": ["texto"],
        "columna_etiqueta": "etiqueta"
    },
    {
        "nombre": "Manuel IMA",
        "ruta": "datasets preparados/dataset_manuel_ima_preparado.csv",
        "columnas_texto": ["texto"],
        "columna_etiqueta": "etiqueta"
    }
]


# =====================================================
# 2. CONFIGURACIÓN GENERAL
# =====================================================

RANDOM_STATE = 42
TEST_SIZE = 0.20

# =====================================================
# 3. FUNCIÓN PARA CARGAR DATASETS
# =====================================================

def cargar_dataset(ruta):

    if ruta.endswith(".csv"):
        return pd.read_csv(ruta, sep=None, engine="python")

    elif ruta.endswith((".xlsx", ".xls")):
        return pd.read_excel(ruta)

    elif ruta.endswith(".json"):
        return pd.read_json(ruta)

    elif ruta.endswith(".jsonl"):
        return pd.read_json(ruta, lines=True)

    else:
        raise ValueError(f"Formato no soportado: {ruta}")

# =====================================================
# 4. EVALUAR UN DATASET
# =====================================================

def evaluar_dataset(config):

    nombre = config["nombre"]
    ruta = config["ruta"]

    print(f"\nEvaluando: {nombre}")

    try:

        # Cargar datos
        df = cargar_dataset(ruta)

        columnas_texto = config["columnas_texto"]
        columna_etiqueta = config["columna_etiqueta"]

        # Verificar columnas
        columnas_necesarias = columnas_texto + [columna_etiqueta]

        for columna in columnas_necesarias:
            if columna not in df.columns:
                raise ValueError(f"No existe la columna: {columna}")

        # Eliminar registros incompletos
        df = df.dropna(subset=columnas_necesarias).copy()

        # Construir texto de entrada
        X = df[columnas_texto].astype(str).agg(" ".join, axis=1)

        # Obtener etiquetas binarias
        y = pd.to_numeric(
            df[columna_etiqueta],
            errors="coerce"
        )

        # Eliminar etiquetas inválidas o ambiguas
        validos = y.isin([0, 1])

        X = X[validos]
        y = y[validos].astype(int)

        # Verificar cantidad de clases
        if y.nunique() != 2:
            raise ValueError(
                "El dataset necesita contener ambas clases: 0 y 1."
            )

        if y.value_counts().min() < 2:
            raise ValueError(
                "No hay suficientes registros de alguna clase."
            )

        # Separar entrenamiento y prueba
        X_train, X_test, y_train, y_test = train_test_split(
            X,
            y,
            test_size=TEST_SIZE,
            random_state=RANDOM_STATE,
            stratify=y
        )

        # Modelo común para todos los datasets
        modelo = Pipeline([

            ("tfidf", TfidfVectorizer(
                max_features=30000,
                ngram_range=(1, 2),
                min_df=2
            )),

            ("clasificador", LogisticRegression(
                max_iter=1000,
                class_weight="balanced",
                random_state=RANDOM_STATE
            ))

        ])

        # Entrenamiento
        modelo.fit(X_train, y_train)

        # =====================================================
        # PREDICCIONES
        # =====================================================

        # Probabilidades de clase 1
        probabilidades = modelo.predict_proba(X_test)[:, 1]

        # Predicciones
        umbral = 0.5
        predicciones = (probabilidades >= umbral).astype(int)

        # =====================================================
        # MATRIZ DE CONFUSIÓN
        # =====================================================

        matriz = confusion_matrix(
            y_test,
            predicciones,
            labels=[0, 1]
        )

        tn, fp, fn, tp = matriz.ravel()

        correctos = tn + tp
        incorrectos = fp + fn

        # =====================================================
        # MÉTRICAS
        # =====================================================

        auc = roc_auc_score(y_test, probabilidades)
        accuracy = accuracy_score(y_test, predicciones)

        precision = precision_score(
            y_test, predicciones, zero_division=0
        )

        recall = recall_score(
            y_test, predicciones, zero_division=0
        )

        f1 = f1_score(
            y_test, predicciones, zero_division=0
        )

        porcentaje_fn = (
            fn / (fn + tp) * 100
            if (fn + tp) > 0 else 0
        )

        # =====================================================
        # MOSTRAR RESULTADOS
        # =====================================================

        print(f"\n========== {nombre} ==========")

        print("\nMatriz de confusión:")
        print(matriz)

        print("\nResultados:")
        print(f"TN: {tn}")
        print(f"FP: {fp}")
        print(f"FN: {fn}")
        print(f"TP: {tp}")

        print(f"\nCorrectos: {correctos}")
        print(f"Incorrectos: {incorrectos}")

        print(f"\nAUC: {auc:.4f}")
        print(f"Accuracy: {accuracy:.4f}")
        print(f"Precision: {precision:.4f}")
        print(f"Recall: {recall:.4f}")
        print(f"F1: {f1:.4f}")

        print(f"\nPorcentaje de falsos negativos: {porcentaje_fn:.2f}%")

        print("\nReporte de clasificación:")
        print(classification_report(
            y_test,
            predicciones,
            labels=[0, 1],
            target_names=["Permitido", "Requiere intervención"],
            zero_division=0
        ))

        # =====================================================
        # RETORNAR RESULTADOS
        # =====================================================

        return {
            "dataset": nombre,
            "registros": len(X),
            "entrenamiento": len(X_train),
            "prueba": len(X_test),
            "clase_0": int((y == 0).sum()),
            "clase_1": int((y == 1).sum()),

            "TN": int(tn),
            "FP": int(fp),
            "FN": int(fn),
            "TP": int(tp),

            "correctos": int(correctos),
            "incorrectos": int(incorrectos),

            "AUC": round(auc, 4),
            "Accuracy": round(accuracy, 4),
            "Precision": round(precision, 4),
            "Recall": round(recall, 4),
            "F1": round(f1, 4),

            "porcentaje_FN": round(porcentaje_fn, 2),

            "estado": "Correcto"
        }

    except Exception as e:

        print(f"Error en {nombre}: {e}")

        return {
            "dataset": nombre,
            "AUC": None,
            "Accuracy": None,
            "F1": None,
            "estado": str(e)
        }

# =====================================================
# 5. PROCESAR TODOS LOS DATASETS
# =====================================================

resultados = []

for dataset in rutas_datasets:

    resultado = evaluar_dataset(dataset)

    resultados.append(resultado)


# =====================================================
# 6. MOSTRAR Y GUARDAR RESULTADOS
# =====================================================

df_resultados = pd.DataFrame(resultados)

print("\n========== COMPARACIÓN DE DATASETS ==========")

print(
    df_resultados.to_string(index=False)
)

df_resultados.to_csv(
    "resultados_auc_datasets.csv",
    index=False,
    encoding="utf-8-sig"
)

print("\nResultados guardados en resultados_auc_datasets.csv")