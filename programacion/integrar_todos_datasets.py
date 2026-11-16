
import pandas as pd
from pathlib import Path

# ==========================================
# 1. CONFIGURACIÓN
# ==========================================

CARPETA = Path("datasets preparados")

ARCHIVOS = {
    "offendes": "daset_offendes_preparado.csv",
    "clandestino": "dataset_clandestino_preparado.csv",
    "manuel_ima": "dataset_manuel_ima_preparado.csv",
    "mlt-txc-dst": "dataset_mult-toxi-dst_preparado.csv",
    "sintetico": "dataset_sintetico_preparado.csv"
}


# ==========================================
# 2. CARGAR LOS CINCO DATASETS
# ==========================================

datasets = []

for origen, nombre_archivo in ARCHIVOS.items():

    ruta = CARPETA / nombre_archivo

    df = pd.read_csv(ruta, encoding="utf-8")

    # Verificar columnas necesarias
    if not {"texto", "etiqueta"}.issubset(df.columns):
        raise ValueError(
            f"El archivo {nombre_archivo} no contiene "
            "las columnas texto y etiqueta."
        )

    # Conservar únicamente las columnas necesarias
    df = df[["texto", "etiqueta"]].copy()

    # Registrar procedencia
    df["origen"] = origen

    datasets.append(df)

    print(f"{origen}: {len(df)} registros cargados")


# ==========================================
# 3. CONCATENAR DATASETS
# ==========================================

df_total = pd.concat(
    datasets,
    ignore_index=True
)

print("\n========== INTEGRACIÓN ==========")

print("Total de registros:", len(df_total))

print("\nRegistros por origen:")
print(df_total["origen"].value_counts())


# ==========================================
# 4. VALIDACIÓN GENERAL
# ==========================================

df_total["texto"] = (
    df_total["texto"]
    .astype("string")
    .str.strip()
)

df_total = df_total.dropna(subset=["texto"])

df_total = df_total[df_total["texto"].ne("")]

# Verificar etiquetas válidas
df_total["etiqueta"] = pd.to_numeric(
    df_total["etiqueta"],
    errors="coerce"
)

df_total = df_total[
    df_total["etiqueta"].isin([0, 1])
].copy()

df_total["etiqueta"] = df_total["etiqueta"].astype("int64")


# 5. DETECTAR ETIQUETAS CONTRADICTORIAS

etiquetas_por_texto = (
    df_total.groupby("texto")["etiqueta"].nunique()
)

textos_conflictivos = etiquetas_por_texto[
    etiquetas_por_texto > 1
].index

conflictos = df_total[
    df_total["texto"].isin(textos_conflictivos)
].sort_values("texto")

conflictos.to_csv(
    CARPETA / "conflictos_entre_datasets.csv",
    index=False,
    encoding="utf-8"
)

print("\n========== CONFLICTOS ==========")

print("Textos conflictivos:", len(textos_conflictivos))
print("Registros involucrados:", len(conflictos))

# 6. EXCLUIR TEMPORALMENTE LOS CONFLICTOS

df_sin_conflictos = df_total[
    ~df_total["texto"].isin(textos_conflictivos)
].copy()

# 7. DETECTAR DUPLICADOS ENTRE FUENTES

duplicados = df_sin_conflictos[
    df_sin_conflictos.duplicated(
        subset=["texto", "etiqueta"],
        keep=False
    )
].sort_values("texto")

duplicados.to_csv(
    CARPETA / "duplicados_entre_datasets.csv",
    index=False,
    encoding="utf-8"
)

print("\n========== DUPLICADOS ==========")

print("Registros involucrados:", len(duplicados))

# 8. CONSOLIDAR REGISTROS ÚNICOS

# Agrupar textos idénticos con la misma etiqueta.
# Conservar todos sus orígenes para auditoría.

df_consolidado = (
    df_sin_conflictos
    .groupby(["texto", "etiqueta"], as_index=False)
    .agg(
        origen=("origen", lambda x: "; ".join(sorted(set(x))))
    )
)

df_consolidado = df_consolidado[
    ["texto", "etiqueta", "origen"]
]

# 9. RESULTADOS FINALES

print("\n========== DATASET CONSOLIDADO ==========")

print("Registros originales:", len(df_total))
print("Textos conflictivos:", len(textos_conflictivos))
print("Registros finales:", len(df_consolidado))

print("\nDistribución de etiquetas:")
print(df_consolidado["etiqueta"].value_counts())

print("\nPorcentaje por etiqueta:")
print(
    df_consolidado["etiqueta"]
    .value_counts(normalize=True) * 100
)

print("\nRegistros por origen:")
print(
    df_consolidado["origen"]
    .str.split("; ")
    .explode()
    .value_counts()
)

print("\nDuplicados restantes:",
      df_consolidado.duplicated(
          subset=["texto"]
      ).sum())

# 10. EXPORTAR RESULTADOS

df_consolidado.to_csv(
    CARPETA / "dataset_consolidado.csv",
    index=False,
    encoding="utf-8"
)

print("\nProceso de integración finalizado.")