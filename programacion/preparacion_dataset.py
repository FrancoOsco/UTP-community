import pandas as pd


# FASE 1. Preparación del dataset

# PASO 1. Cargar el archivo CSV con Pandas.

# Cargar el dataset
df = pd.read_csv("datasets/dataset_manuel_ima.csv", sep=";",
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

#########################

# PASO 6. Dividir los datos en entrenamiento, validación y prueba.