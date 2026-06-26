"""DICOM / PACS 処理"""

import io
import uuid
from datetime import datetime, timezone
from pathlib import Path

import pydicom
from PIL import Image
from sqlalchemy.orm import Session

from src.config import settings
from src.db import models as m


def storage_root() -> Path:
    p = Path(settings.dicom_storage_path)
    p.mkdir(parents=True, exist_ok=True)
    return p


def ingest_dicom(file_bytes: bytes, patient_id: int, db: Session) -> dict:
    ds = pydicom.dcmread(io.BytesIO(file_bytes), force=True)
    study_uid = str(getattr(ds, "StudyInstanceUID", uuid.uuid4()))
    modality = str(getattr(ds, "Modality", "OT"))
    body_part = str(getattr(ds, "BodyPartExamined", "")) or None
    desc = str(getattr(ds, "StudyDescription", "")) or None

    fname = f"{study_uid.replace('.', '_')}.dcm"
    fpath = storage_root() / fname
    fpath.write_bytes(file_bytes)

    preview_path = None
    try:
        arr = ds.pixel_array
        img = Image.fromarray(arr.astype("float32"))
        img = (img / img.max() * 255).astype("uint8") if img.max() else img.astype("uint8")
        preview = storage_root() / f"{fname}.png"
        Image.fromarray(img if img.mode else img.convert("L")).save(preview)
        preview_path = str(preview)
    except Exception:
        pass

    performed = datetime.now(timezone.utc)
    if hasattr(ds, "StudyDate") and ds.StudyDate:
        try:
            performed = datetime.strptime(str(ds.StudyDate), "%Y%m%d").replace(tzinfo=timezone.utc)
        except ValueError:
            pass

    study = m.ImagingStudy(
        patient_id=patient_id,
        study_uid=study_uid,
        modality=modality,
        body_part=body_part,
        description=desc,
        instance_count=1,
        file_path=str(fpath),
        preview_path=preview_path,
        performed_at=performed,
    )
    db.add(study)
    db.commit()
    db.refresh(study)
    return _study_dict(study)


def _study_dict(s: m.ImagingStudy, patient_name: str | None = None) -> dict:
    if patient_name is None and s.patient:
        patient_name = f"{s.patient.family_name} {s.patient.given_name}"
    return {
        "id": s.id,
        "patient_id": s.patient_id,
        "patient_name": patient_name or f"Patient #{s.patient_id}",
        "study_uid": s.study_uid,
        "modality": s.modality,
        "body_part": s.body_part,
        "description": s.description,
        "instance_count": s.instance_count,
        "has_preview": bool(s.preview_path),
        "performed_at": s.performed_at.isoformat(),
        "fhir_url": f"/fhir/R4/ImagingStudy/{s.id}",
    }


def list_studies(db: Session, patient_id: int | None = None) -> list[dict]:
    q = (
        db.query(m.ImagingStudy)
        .join(m.Patient)
        .order_by(m.ImagingStudy.performed_at.desc())
    )
    if patient_id:
        q = q.filter(m.ImagingStudy.patient_id == patient_id)
    return [_study_dict(s) for s in q.all()]


def get_study(study_id: int, db: Session) -> m.ImagingStudy | None:
    return db.get(m.ImagingStudy, study_id)
