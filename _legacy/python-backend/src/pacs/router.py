"""PACS / DICOM API"""

from pathlib import Path

from fastapi import APIRouter, Depends, File, HTTPException, Query, UploadFile
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from src.db.database import get_db
from src.pacs.dicom_service import get_study, ingest_dicom, list_studies

router = APIRouter(prefix="/api/pacs", tags=["PACS"])


@router.get("/studies")
def studies(patient_id: int | None = Query(None), db: Session = Depends(get_db)):
    return {"studies": list_studies(db, patient_id)}


@router.get("/studies/{study_id}")
def study_detail(study_id: int, db: Session = Depends(get_db)):
    s = get_study(study_id, db)
    if not s:
        raise HTTPException(404, "Study not found")
    from src.pacs.dicom_service import _study_dict

    return _study_dict(s)


@router.post("/studies/upload")
async def upload_dicom(
    patient_id: int = Query(...),
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
):
    data = await file.read()
    if not data:
        raise HTTPException(400, "Empty file")
    try:
        return ingest_dicom(data, patient_id, db)
    except Exception as e:
        raise HTTPException(400, f"DICOM parse error: {e}") from e


@router.get("/studies/{study_id}/preview")
def study_preview(study_id: int, db: Session = Depends(get_db)):
    s = get_study(study_id, db)
    if not s or not s.preview_path or not Path(s.preview_path).exists():
        raise HTTPException(404, "Preview not available")
    return FileResponse(s.preview_path, media_type="image/png")


@router.get("/studies/{study_id}/dicom")
def download_dicom(study_id: int, db: Session = Depends(get_db)):
    s = get_study(study_id, db)
    if not s or not s.file_path or not Path(s.file_path).exists():
        raise HTTPException(404, "DICOM file not found")
    return FileResponse(s.file_path, media_type="application/dicom", filename=Path(s.file_path).name)
