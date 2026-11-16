# =====================================================
# IMPORTACIONES
# =====================================================

import pandas as pd
import numpy as np
import joblib

from sklearn.model_selection import train_test_split

from sklearn.pipeline import Pipeline
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression

from sklearn.metrics import (
    confusion_matrix,
    classification_report,
    roc_auc_score,
    accuracy_score,
    precision_score,
    recall_score,
    f1_score
)


# =====================================================
# CONFIGURACIÓN
# =====================================================

RANDOM_STATE = 42
TEST_SIZE = 0.20
UMBRAL = 0.50

# Seleccionar los datasets que participarán
rutas_datasets = [
    {
        "nombre": "OffendES",
        "ruta": "datasets preparados/daset_offendes_preparado.csv",
        "columnas_texto": ["texto"],
        "columna_etiqueta": "etiqueta"
    },
    {
        "nombre": "Manuel IMA",
        "ruta": "datasets preparados/dataset_manuel_ima_preparado.csv",
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
        "nombre": "nuevo",
        "ruta": "datasets preparados/dataset_mult-toxi-dst_preparado.csv",
        "columnas_texto": ["texto"],
        "columna_etiqueta": "etiqueta"
    },
    {
        "nombre": "Sintetico",
        "ruta": "datasets preparados/dataset_sintetico_preparado.csv",
        "columnas_texto": ["texto"],
        "columna_etiqueta": "etiqueta"
    }
]

# prueba transversal
ruta_test_sintetico = {
    "nombre": "Dataset Sintetico",
    "ruta": "datasets preparados/dataset_sintetico_preparado.csv",
    "columnas_texto": ["texto"],
    "columna_etiqueta": "etiqueta"
}

# =====================================================
# 1. CARGAR Y CONSOLIDAR DATASETS
# =====================================================

def consolidar_datasets(rutas):

    datasets = []

    for config in rutas:

        nombre = config["nombre"]
        ruta = config["ruta"]

        print(f"\nCargando: {nombre}")

        try:

            df = pd.read_csv(ruta)

            # Verificar columnas necesarias
            columnas_requeridas = (
                config["columnas_texto"] +
                [config["columna_etiqueta"]]
            )

            faltantes = [
                col for col in columnas_requeridas
                if col not in df.columns
            ]

            if faltantes:
                raise ValueError(
                    f"Columnas inexistentes: {faltantes}"
                )

            # Unificar columnas de texto
            df["texto"] = (
                df[config["columnas_texto"]]
                .fillna("")
                .astype(str)
                .agg(" ".join, axis=1)
                .str.strip()
            )

            # Obtener etiqueta
            df["etiqueta"] = pd.to_numeric(
                df[config["columna_etiqueta"]],
                errors="coerce"
            )

            # Eliminar registros inválidos
            df = df.dropna(
                subset=["texto", "etiqueta"]
            )

            df = df[
                (df["texto"] != "") &
                (df["etiqueta"].isin([0, 1]))
            ].copy()

            df["etiqueta"] = df["etiqueta"].astype(int)

            # Guardar procedencia
            df["origen"] = nombre

            # Conservar únicamente los campos necesarios
            df = df[["texto", "etiqueta", "origen"]]

            datasets.append(df)

            print(f"Registros válidos: {len(df)}")

        except Exception as e:

            print(f"Error cargando {nombre}: {e}")

    if not datasets:
        raise ValueError("No se pudo cargar ningún dataset.")

    # Unir todos los datasets
    df_consolidado = pd.concat(
        datasets,
        ignore_index=True
    )

    print("\n========== CONSOLIDACIÓN ==========")

    print(f"Total de registros: {len(df_consolidado)}")

    print("\nDistribución de etiquetas:")
    print(df_consolidado["etiqueta"].value_counts().sort_index())

    print("\nRegistros por dataset:")
    print(df_consolidado["origen"].value_counts())

    return df_consolidado

# Cargar dataset sintético
def cargar_test_sintetico(config):
    df = pd.read_csv(config["ruta"])

    # Validar columnas
    columnas_requeridas = (
        config["columnas_texto"] + [config["columna_etiqueta"]]
    )

    faltantes = [
        columna for columna in columnas_requeridas
        if columna not in df.columns
    ]

    if faltantes:
        raise ValueError(
            f"Faltan columnas en el dataset sintético: {faltantes}"
        )

    # Unir columnas de texto
    df["texto"] = (
        df[config["columnas_texto"]]
        .fillna("")
        .astype(str)
        .agg(" ".join, axis=1)
    )

    # Normalizar etiquetas
    df["etiqueta"] = pd.to_numeric(
        df[config["columna_etiqueta"]],
        errors="coerce"
    )

    # Conservar registros válidos
    df = df[
        (df["texto"].str.strip() != "") &
        (df["etiqueta"].isin([0, 1]))
    ].copy()

    df["etiqueta"] = df["etiqueta"].astype(int)

    return df[["texto", "etiqueta"]].reset_index(drop=True)

def evaluar_modelo(modelo, X_test, y_test, nombre_prueba):
    # Predicciones
    y_prob = modelo.predict_proba(X_test)[:, 1]
    y_pred = (y_prob >= UMBRAL).astype(int)

    # Matriz de confusión
    tn, fp, fn, tp = confusion_matrix(
        y_test,
        y_pred,
        labels=[0, 1]
    ).ravel()

    # Métricas
    resultados = {
        "Prueba": nombre_prueba,
        "Registros": len(y_test),
        "Accuracy": accuracy_score(y_test, y_pred),
        "Precision": precision_score(
            y_test, y_pred, zero_division=0
        ),
        "Recall": recall_score(
            y_test, y_pred, zero_division=0
        ),
        "F1": f1_score(
            y_test, y_pred, zero_division=0
        ),
        "AUC": roc_auc_score(y_test, y_prob)
            if len(np.unique(y_test)) == 2 else np.nan,
        "TN": tn,
        "FP": fp,
        "FN": fn,
        "TP": tp,
        "Tasa_FN": fn / (fn + tp) if (fn + tp) > 0 else 0
    }

    print(f"\n{'=' * 50}")
    print(f"RESULTADOS: {nombre_prueba}")
    print(f"{'=' * 50}")

    for metrica, valor in resultados.items():
        if isinstance(valor, float):
            print(f"{metrica}: {valor:.4f}")
        else:
            print(f"{metrica}: {valor}")

    return resultados

# =====================================================
# 2. CONTROL DE DUPLICADOS
# =====================================================

def limpiar_duplicados(df):

    print("\n========== CONTROL DE DUPLICADOS ==========")

    # Normalización básica para detectar textos repetidos
    df = df.copy()

    df["texto_normalizado"] = (
        df["texto"]
        .str.lower()
        .str.strip()
        .str.replace(r"\s+", " ", regex=True)
    )

    # Detectar textos con etiquetas contradictorias
    etiquetas_por_texto = (
        df.groupby("texto_normalizado")["etiqueta"]
        .nunique()
    )

    textos_conflictivos = etiquetas_por_texto[
        etiquetas_por_texto > 1
    ].index

    cantidad_conflictos = len(textos_conflictivos)

    if cantidad_conflictos > 0:

        print(
            f"Textos con etiquetas contradictorias: "
            f"{cantidad_conflictos}"
        )

        # Excluir los textos contradictorios
        df = df[
            ~df["texto_normalizado"].isin(textos_conflictivos)
        ].copy()

    # Eliminar duplicados exactos normalizados
    antes = len(df)

    df = df.drop_duplicates(
        subset=["texto_normalizado"],
        keep="first"
    )

    eliminados = antes - len(df)

    print(f"Duplicados eliminados: {eliminados}")
    print(f"Registros finales: {len(df)}")

    return df.drop(columns=["texto_normalizado"])


# =====================================================
# 3. ENTRENAMIENTO Y EVALUACIÓN
# =====================================================

def entrenar_modelo(df, df_test_sintetico):

    # ==========================================
    # 1. PREPARAR DATOS DE ENTRENAMIENTO
    # ==========================================

    X = df["texto"]
    y = df["etiqueta"]

    # División interna 80/20
    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=TEST_SIZE,
        stratify=y,
        random_state=RANDOM_STATE
    )

    # ==========================================
    # 2. CREAR Y ENTRENAR MODELO
    # ==========================================

    modelo = Pipeline([
        (
            "tfidf",
            TfidfVectorizer(
                max_features=30000,
                ngram_range=(1, 2),
                min_df=2
            )
        ),
        (
            "clasificador",
            LogisticRegression(
                max_iter=1000,
                class_weight="balanced",
                random_state=RANDOM_STATE
            )
        )
    ])

    modelo.fit(X_train, y_train)

    # ==========================================
    # 3. PRUEBA 1: TEST INTERNO
    # ==========================================

    resultados_internos = evaluar_modelo(
        modelo,
        X_test,
        y_test,
        "Test interno"
    )

    # ==========================================
    # 4. PRUEBA 2: TEST SINTÉTICO
    # ==========================================

    X_sintetico = df_test_sintetico["texto"]
    y_sintetico = df_test_sintetico["etiqueta"]

    resultados_sinteticos = evaluar_modelo(
        modelo,
        X_sintetico,
        y_sintetico,
        "Test sintético"
    )

    return modelo, resultados_internos, resultados_sinteticos


def rutas_datasets_nombre():

    return [
        config["nombre"]
        for config in rutas_datasets
    ]


# =====================================================
# 4. EJECUCIÓN PRINCIPAL
# =====================================================

if __name__ == "__main__":

    # ==========================================
    # 1. CONSOLIDAR DATASETS DE ENTRENAMIENTO
    # ==========================================

    df = consolidar_datasets(rutas_datasets)

    df = limpiar_duplicados(df)

    print("\nDatasets utilizados para entrenar:")
    print(df["origen"].value_counts())

    print("\nTotal de registros para entrenamiento:")
    print(len(df))

    # ==========================================
    # 2. CARGAR TEST SINTÉTICO
    # ==========================================

    df_test_sintetico = cargar_test_sintetico(
        ruta_test_sintetico
    )

    print("\nRegistros del test sintético:")
    print(len(df_test_sintetico))

    print("\nDistribución de etiquetas del test sintético:")
    print(df_test_sintetico["etiqueta"].value_counts())

    # ==========================================
    # 3. ENTRENAR Y EVALUAR
    # ==========================================

    modelo, resultados_internos, resultados_sinteticos = (
        entrenar_modelo(df, df_test_sintetico)
    )

    # ==========================================
    # 4. GUARDAR MODELO
    # ==========================================

    # joblib.dump(modelo, "modelo_consolidado.pkl")

    # ==========================================
    # 5. GUARDAR RESULTADOS
    # ==========================================
    """
    resultados = pd.DataFrame([
        resultados_internos,
        resultados_sinteticos
    ])

    resultados.to_csv(
        "resultados_consolidacion.csv",
        index=False
    )

    print("\nResultados guardados correctamente.")
    """