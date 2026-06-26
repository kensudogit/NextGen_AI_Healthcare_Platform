from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from src.config import settings
from src.dashboard.router import router as dashboard_router
from src.db.database import SessionLocal, init_db
from src.db.seed import seed_demo_data
from src.emr.router import router as emr_router
from src.fhir.router import router as fhir_router
from src.pacs.router import router as pacs_router
from src.phone.router import router as phone_router
from src.radiology.router import router as radiology_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    db = SessionLocal()
    try:
        seed_demo_data(db)
    finally:
        db.close()
    yield


app = FastAPI(
    title="NextGen AI Healthcare Platform",
    description="AI電話受付 · 電子カルテ · PACS/DICOM · 読影AI · FHIR",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_list(),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(dashboard_router)
app.include_router(emr_router)
app.include_router(fhir_router)
app.include_router(pacs_router)
app.include_router(radiology_router)
app.include_router(phone_router)


@app.get("/health")
def health():
    return {"status": "ok", "hospital": settings.hospital_name}


@app.get("/")
def root():
    return {
        "name": "NextGen AI Healthcare Platform",
        "docs": "/docs",
        "fhir": "/fhir/R4/metadata",
        "dashboard": "/api/dashboard/stats",
    }
