"""Configuración de rutas y valores de inferencia del servicio."""

import os
from pathlib import Path


# app/config.py -> moderation-api -> raíz del repositorio.
PROJECT_ROOT = Path(__file__).resolve().parents[2]


def _artifact_directory() -> Path:
    configured_directory = os.getenv("MODERATION_ARTIFACTS_DIR")
    if configured_directory:
        candidate = Path(configured_directory).expanduser()
        if not candidate.is_absolute():
            candidate = PROJECT_ROOT / candidate
        return candidate.resolve()

    candidates = (
        PROJECT_ROOT / "moderation-api" / "artifacts",
        PROJECT_ROOT / "programacion" / "modelos" / "moderacion" / "conv1D_keras",
    )
    required_files = (
        "modelo_embedding_conv1d.keras",
        "vectorizador_textos.keras",
        "configuracion.json",
        "metadata.json",
    )
    for candidate in candidates:
        if all((candidate / filename).is_file() for filename in required_files):
            return candidate.resolve()

    # Retain the API's conventional location so startup reports the missing
    # artifact clearly if neither complete artifact set is available.
    return candidates[0].resolve()


ARTIFACTS_DIR = _artifact_directory()
MODEL_PATH = ARTIFACTS_DIR / "modelo_embedding_conv1d.keras"
VECTORIZER_PATH = ARTIFACTS_DIR / "vectorizador_textos.keras"
INFERENCE_CONFIG_PATH = ARTIFACTS_DIR / "configuracion.json"
METADATA_PATH = ARTIFACTS_DIR / "metadata.json"

# Fallbacks keep configuration explicit; persisted values in the JSON files
# are authoritative when the service loads the model.
DEFAULT_CLASSIFICATION_THRESHOLD = 0.4
DEFAULT_MODEL_VERSION = "1.0.0"
