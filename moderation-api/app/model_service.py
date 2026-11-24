import json
import math
import threading
from pathlib import Path
from typing import Any

import numpy as np
import tensorflow as tf

from . import config
from .text_cleaning import clean_text

_SMOKE_TEXTS = ["Qué buena clase, gracias profesor", "Vete a la mierda, hijo de puta"]
MODEL_NAME = "embedding-conv1d"


class ModerationModelService:
    """Carga el modelo (con TextVectorization embebido) y realiza inferencias."""

    def __init__(self) -> None:
        self._model: tf.keras.Model | None = None
        self._threshold = config.DEFAULT_CLASSIFICATION_THRESHOLD
        self._model_version = config.DEFAULT_MODEL_VERSION
        self._text_rank: int | None = None   # 2 -> (n, 1) | 1 -> (n,)
        self._lock = threading.Lock()

    @property
    def model_loaded(self) -> bool:
        return self._model is not None

    @property
    def model_version(self) -> str:
        return self._model_version

    @staticmethod
    def _read_json(path: Path) -> dict[str, Any]:
        with path.open("r", encoding="utf-8") as file:
            data = json.load(file)
        if not isinstance(data, dict):
            raise ValueError(f"{path.name} debe contener un objeto JSON.")
        return data

    def _probabilities(self, texts: list[str]) -> np.ndarray:
        cleaned = [clean_text(t) for t in texts]
        if any(not t for t in cleaned):
            raise ValueError("El texto no puede estar vacío.")

        # El modelo puede esperar (n, 1) o (n,): se detecta una vez y se recuerda
        ranks = (2, 1) if self._text_rank is None else (self._text_rank,)
        last_error: Exception | None = None
        output = None
        for rank in ranks:
            batch = tf.constant(cleaned, dtype=tf.string)
            if rank == 2:
                batch = tf.reshape(batch, (-1, 1))
            try:
                output = self._model(batch, training=False)
            except Exception as exc:  # noqa: BLE001
                last_error = exc
                continue
            self._text_rank = rank
            break

        if output is None:
            raise RuntimeError(f"El modelo no acepta la entrada de texto: {last_error}")

        values = np.asarray(output, dtype=np.float64).reshape(-1)
        if values.size != len(cleaned):
            raise ValueError("El modelo debe devolver una probabilidad por texto.")
        if not np.all(np.isfinite(values)) or values.min() < 0.0 or values.max() > 1.0:
            raise ValueError("El modelo devolvió una probabilidad inválida.")
        return values

    def load(self) -> None:
        for path in (config.MODEL_PATH, config.INFERENCE_CONFIG_PATH, config.METADATA_PATH):
            if not path.is_file():
                raise FileNotFoundError(f"No se encontró el artefacto requerido: {path}")

        inference_config = self._read_json(config.INFERENCE_CONFIG_PATH)
        metadata = self._read_json(config.METADATA_PATH)

        if "umbral_clasificacion" not in inference_config:
            print("[moderation] AVISO: falta 'umbral_clasificacion'; se usa el valor por defecto.")
        threshold = float(
            inference_config.get("umbral_clasificacion", config.DEFAULT_CLASSIFICATION_THRESHOLD)
        )
        if not 0.0 <= threshold <= 1.0:
            raise ValueError("umbral_clasificacion debe estar entre 0 y 1.")

        model_version = str(metadata.get("version", config.DEFAULT_MODEL_VERSION)).strip()
        if not model_version:
            raise ValueError("metadata.json debe declarar una versión no vacía.")

        model = tf.keras.models.load_model(config.MODEL_PATH, compile=False)

        # Prueba de humo: sustituye a model.input / output_shape, que fallan aquí.
        # Verifica que el modelo acepta strings y devuelve una probabilidad por texto.
        self._model = model
        self._text_rank = None
        try:
            probs = self._probabilities(_SMOKE_TEXTS)
        except Exception:
            self._model = None
            raise

        self._threshold = threshold
        self._model_version = model_version
        print(
            f"[moderation] modelo v{model_version} listo | umbral={threshold} "
            f"| humo: {np.round(probs, 3).tolist()}"
        )

    def predict(self, texto: str) -> dict[str, int | float | str]:
        if not self.model_loaded:
            raise RuntimeError("El modelo de moderación todavía no está cargado.")
        if not isinstance(texto, str) or not texto.strip():
            raise ValueError("El texto no puede estar vacío.")

        with self._lock:
            score = float(self._probabilities([texto])[0])

        label = int(score >= self._threshold)
        confidence = score if label == 1 else 1.0 - score

        return {
            "label": label,
            "probability": confidence,
            "score": score,   # probabilidad de "Requiere revisión" (útil para ordenar la cola)
            "model_version": self._model_version,
            "model_name": MODEL_NAME,
        }
