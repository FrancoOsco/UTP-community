# FASE 2.8. inspeccionar los errores del modelo

# Paso 1 — Construir un DataFrame de diagnóstico

diagnostico = df_val[
    ["titulo", "descripcion", "texto"]
].copy()

diagnostico["etiqueta_real"] = y_val
diagnostico["probabilidad_revision"] = y_val_proba[:, 1]

diagnostico["prediccion"] = (
    diagnostico["probabilidad_revision"] >= 0.15
).astype(int)

diagnostico["resultado"] = np.select(
    [
        (diagnostico["etiqueta_real"] == 1) & (diagnostico["prediccion"] == 1),
        (diagnostico["etiqueta_real"] == 0) & (diagnostico["prediccion"] == 0),
        (diagnostico["etiqueta_real"] == 0) & (diagnostico["prediccion"] == 1),
        (diagnostico["etiqueta_real"] == 1) & (diagnostico["prediccion"] == 0),
    ],
    ["TP", "TN", "FP", "FN"],
    default="NA"
)

print(diagnostico["resultado"].value_counts())

# Paso 2 — Examinar falsos negativos

falsos_negativos = diagnostico[
    diagnostico["resultado"] == "FN"
].sort_values(
    "probabilidad_revision",
    ascending=False
)

print("\nTop 10 falsos negativos (ofensivos que el modelo dejó pasar):")
print(falsos_negativos[["descripcion", "probabilidad_revision"]].head(10).to_string())

# Paso 3 — Examinar falsos positivos

falsos_positivos = diagnostico[
    diagnostico["resultado"] == "FP"
].sort_values(
    "probabilidad_revision",
    ascending=False
)

print("\nTop 10 falsos positivos (inocentes marcados como ofensivos):")
print(falsos_positivos[["descripcion", "probabilidad_revision"]].head(10).to_string())

auditoria = diagnostico[
    [
        "titulo",
        "descripcion",
        "etiqueta_real",
        "prediccion",
        "probabilidad_revision",
        "resultado"
    ]
].copy()

auditoria.index.name = "indice_original"

print(auditoria.head(10).to_string())