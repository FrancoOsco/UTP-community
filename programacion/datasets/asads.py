import pandas as pd

# 1. Ruta de tu archivo parquet de origen
ruta_parquet = r"es-00000-of-00001.parquet"

# 2. Leer el archivo Parquet con Pandas
df = pd.read_parquet(ruta_parquet)

# 3. Ruta donde quieres guardar tu nuevo CSV
ruta_csv = r"datasets/dataset_convertido.csv"

# 4. Guardarlo como CSV (usando ';' y utf-8-sig para cuidar tildes y eñes)
df.to_csv(ruta_csv, sep=';', index=False, encoding='utf-8-sig')

print("¡Archivo convertido de Parquet a CSV exitosamente!")