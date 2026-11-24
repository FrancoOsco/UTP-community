from pydantic import BaseModel, Field, field_validator


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    model_version: str


class PredictRequest(BaseModel):
    texto: str = Field(..., min_length=1)

    @field_validator("texto")
    @classmethod
    def texto_no_vacio(cls, value: str) -> str:
        texto_normalizado = value.strip()

        if not texto_normalizado:
            raise ValueError("El texto no puede estar vacío.")

        return texto_normalizado

    model_config = {"extra": "forbid"}


class PredictResponse(BaseModel):
    label: int = Field(..., ge=0, le=1)
    probability: float = Field(..., ge=0.0, le=1.0)
    score: float = Field(..., ge=0.0, le=1.0)
    model_version: str
    model_name: str
