from contextlib import asynccontextmanager
import hmac
import os
from typing import AsyncIterator

from fastapi import Depends, FastAPI, HTTPException, Request, Security
from fastapi.security import APIKeyHeader

from .model_service import ModerationModelService
from .schemas import HealthResponse, PredictRequest, PredictResponse

API_KEY_ENV_NAME = "MODERATION_API_KEY"
api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    service_api_key = os.getenv(API_KEY_ENV_NAME, "").strip()
    if len(service_api_key) < 32:
        raise RuntimeError(
            f"Configura {API_KEY_ENV_NAME} con una clave aleatoria de al menos 32 caracteres."
        )

    model_service = ModerationModelService()
    model_service.load()
    app.state.model_service = model_service
    app.state.service_api_key = service_api_key
    yield


app = FastAPI(
    title="UTP Community Moderation API",
    version="1.0.0",
    lifespan=lifespan,
)


def verify_service_api_key(
    request: Request,
    provided_key: str | None = Security(api_key_header),
) -> None:
    expected_key: str = request.app.state.service_api_key
    if provided_key is None or not hmac.compare_digest(provided_key, expected_key):
        raise HTTPException(
            status_code=401,
            detail="Missing or invalid API key.",
            headers={"WWW-Authenticate": "APIKey"},
        )


@app.get("/health", response_model=HealthResponse)
def health(request: Request) -> HealthResponse:
    model_service: ModerationModelService = request.app.state.model_service
    return HealthResponse(
        status="ok" if model_service.model_loaded else "unavailable",
        model_loaded=model_service.model_loaded,
        model_version=model_service.model_version,
    )


@app.post("/predict", response_model=PredictResponse)
def predict(
    payload: PredictRequest,
    request: Request,
    _: None = Depends(verify_service_api_key),
) -> PredictResponse:
    model_service: ModerationModelService = request.app.state.model_service
    return PredictResponse(**model_service.predict(payload.texto))
