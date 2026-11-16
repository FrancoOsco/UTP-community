
import pandas as pd

from sklearn.feature_extraction.text import TfidfVectorizer
from scipy.sparse import save_npz
import joblib

# ============================================================
# 1. CARGAR LOS CONJUNTOS DE DATOS
# ============================================================

# Recuperamos los tres conjuntos que generamos anteriormente.

train = pd.read_csv("train.csv")
validation = pd.read_csv("validation.csv")
test = pd.read_csv("test.csv")

# Separamos los textos de las etiquetas.

X_train = train["texto"]
X_val = validation["texto"]
X_test = test["texto"]

y_train = train["etiqueta"]
y_val = validation["etiqueta"]
y_test = test["etiqueta"]

# ============================================================
# 2. CREAR EL VECTORIZADOR TF-IDF
# ============================================================

vectorizador = TfidfVectorizer(

    # Convierte todas las palabras a minúsculas.
    # "Amenaza" y "amenaza" serán consideradas el mismo término.
    lowercase=True,

    # Considera unigramas y bigramas.
    # (1, 2) significa palabras individuales y parejas de palabras.
    ngram_range=(1, 2)

)

# ============================================================
# 3. AJUSTAR Y TRANSFORMAR EL CONJUNTO DE ENTRENAMIENTO
# ============================================================

# fit_transform realiza dos operaciones:
#
# 1. fit: aprende el vocabulario y los pesos IDF a partir
#    de los textos de entrenamiento.
#
# 2. transform: convierte esos textos en matrices numéricas.
#
# IMPORTANTE:
# Solo utilizamos entrenamiento para aprender el vocabulario.

X_train_tfidf = vectorizador.fit_transform(X_train)

# ============================================================
# 4. TRANSFORMAR VALIDACIÓN Y PRUEBA
# ============================================================

# Aquí NO utilizamos fit ni fit_transform.
#
# Solo transformamos utilizando el vocabulario y los pesos
# aprendidos previamente con el conjunto de entrenamiento.

X_val_tfidf = vectorizador.transform(X_val)

X_test_tfidf = vectorizador.transform(X_test)

# ============================================================
# 5. MOSTRAR DIMENSIONES DE LAS MATRICES
# ============================================================

print("========== RESULTADOS DE TF-IDF ==========")

print("Matriz de entrenamiento:", X_train_tfidf.shape)
print("Matriz de validación:   ", X_val_tfidf.shape)
print("Matriz de prueba:       ", X_test_tfidf.shape)

print("\nTamaño del vocabulario:")
print(len(vectorizador.vocabulary_))

# ============================================================
# 6. GUARDAR EL VECTORIZADOR Y LAS MATRICES
# ============================================================

# Guardamos el vectorizador para reutilizar exactamente
# el mismo vocabulario y los mismos pesos IDF.

joblib.dump(vectorizador, "vectorizador_tfidf.pkl")

# Guardamos las matrices dispersas.
# TF-IDF produce matrices con muchos ceros, por eso utilizamos
# save_npz para almacenarlas eficientemente.

save_npz("X_train_tfidf.npz", X_train_tfidf)
save_npz("X_val_tfidf.npz", X_val_tfidf)
save_npz("X_test_tfidf.npz", X_test_tfidf)

print("\nVectorizador y matrices guardados correctamente.")