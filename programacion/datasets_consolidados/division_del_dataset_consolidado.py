import pandas as pd

# Fase 1. Auditoría de calidad de etiquetas

# Fase 2. División del dataset

from sklearn.model_selection import train_test_split

# ============================================================
# 1. CARGAR EL DATASET
# ============================================================

# Utilizamos el dataset consolidado después de la auditoría.
df = pd.read_csv("datasets_consolidados/dataset_consolidado.csv")

# Nos aseguramos de que las etiquetas sean números enteros.
df["etiqueta"] = df["etiqueta"].astype(int)

# Eliminamos registros que no tengan texto o etiqueta válida.
df = df.dropna(subset=["texto", "etiqueta"])

# Eliminamos espacios al inicio y al final del texto.
df["texto"] = df["texto"].astype(str).str.strip()

# Conservamos únicamente las dos etiquetas del proyecto.
df = df[df["etiqueta"].isin([0, 1])]

# Eliminamos textos vacíos.
df = df[df["texto"] != ""]

# 2. SEPARAR CARACTERÍSTICAS Y ETIQUETAS

# X contiene los textos que recibirá el modelo.
X = df["texto"]

# y contiene la respuesta correcta para cada texto.
y = df["etiqueta"]

# 3. PRIMERA DIVISIÓN: ENTRENAMIENTO (70%) Y TEMPORAL (30%)

# Separamos el 70% para entrenamiento.
# El 30% restante se utilizará para validación y prueba.

X_train, X_temp, y_train, y_temp = train_test_split(
    X,
    y,
    test_size=0.30,
    random_state=42,
    stratify=y
)

# 4. SEGUNDA DIVISIÓN: VALIDACIÓN (15%) Y PRUEBA (15%)

# Dividimos el 30% restante en dos partes iguales.
# Cada conjunto representará el 15% del dataset original.
X_val, X_test, y_val, y_test = train_test_split(
    X_temp,
    y_temp,
    test_size=0.50,
    random_state=42,
    stratify=y_temp
)

# 5. MOSTRAR RESULTADOS

print("========== DIVISIÓN DEL DATASET ==========")

print(f"Dataset completo: {len(df)}")

print(f"\nEntrenamiento: {len(X_train)}")
print(f"Validación:    {len(X_val)}")
print(f"Prueba:        {len(X_test)}")

print("\nDistribución de etiquetas:")

print("\nEntrenamiento:")
print(y_train.value_counts(normalize=True) * 100)

print("\nValidación:")
print(y_val.value_counts(normalize=True) * 100)

print("\nPrueba:")
print(y_test.value_counts(normalize=True) * 100)

# 6. GUARDAR LOS CONJUNTOS

# Guardamos cada conjunto para poder reutilizarlo posteriormente.
# No incluimos la columna "origen" como característica del modelo.
pd.DataFrame({
    "texto": X_train,
    "etiqueta": y_train
}).to_csv("train.csv", index=False, encoding="utf-8-sig")

pd.DataFrame({
    "texto": X_val,
    "etiqueta": y_val
}).to_csv("validation.csv", index=False, encoding="utf-8-sig")

pd.DataFrame({
    "texto": X_test,
    "etiqueta": y_test
}).to_csv("test.csv", index=False, encoding="utf-8-sig")

print("\nConjuntos guardados correctamente.")
