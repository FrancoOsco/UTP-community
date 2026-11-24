import json, numpy as np, tensorflow as tf, keras
print("keras", keras.__version__, "| tf", tf.__version__)

ART = "artifacts"
print(open(f"{ART}/configuracion.json", encoding="utf-8").read())
print(open(f"{ART}/metadata.json", encoding="utf-8").read())

vec = keras.models.load_model(f"{ART}/vectorizador_textos.keras", compile=False)
print("Vectorizador:", type(vec))

mod = keras.models.load_model(f"{ART}/modelo_embedding_conv1d.keras", compile=False)
print("Modelo:", type(mod), "| construido:", mod.built)
mod.summary()

textos = ["Qué buena clase, gracias profesor", "Vete a la mierda, hijo de puta"]
try:
    ids = vec.predict(np.array(textos).reshape(-1, 1), verbose=0)
except Exception:
    ids = vec(tf.constant(textos)).numpy()
print("ids:", ids.shape, ids.dtype)          # esperado: (2, MAX_LEN) enteros
print("probs:", mod.predict(ids, verbose=0).ravel())