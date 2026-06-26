"""AI 読影 API"""

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session

from src.ai.radiology import summarize_and_save, summarize_radiology_report
from src.db import models as m
from src.db.database import get_db

router = APIRouter(prefix="/api/radiology", tags=["Radiology AI"])


class ReportCreate(BaseModel):
    patient_id: int
    study_id: int | None = None
    modality: str = "CT"
    report_text: str
    radiologist: str | None = None


class SummarizeBody(BaseModel):
    report_text: str


@router.get("/reports")
def list_reports(db: Session = Depends(get_db)):
    rows = (
        db.query(m.RadiologyReport)
        .join(m.Patient)
        .order_by(m.RadiologyReport.reported_at.desc())
        .all()
    )
    return {
        "reports": [
            {
                "id": r.id,
                "patient_id": r.patient_id,
                "patient_name": f"{r.patient.family_name} {r.patient.given_name}",
                "modality": r.modality,
                "report_text": r.report_text,
                "ai_summary": r.ai_summary,
                "ai_findings": r.ai_findings,
                "urgency": r.urgency,
                "radiologist": r.radiologist,
                "reported_at": r.reported_at.isoformat(),
                "fhir_url": f"/fhir/R4/DiagnosticReport/{r.id}",
            }
            for r in rows
        ]
    }


@router.get("/reports/{report_id}")
def get_report(report_id: int, db: Session = Depends(get_db)):
    r = db.get(m.RadiologyReport, report_id)
    if not r:
        raise HTTPException(404, "Report not found")
    import json

    findings = []
    if r.ai_findings:
        try:
            findings = json.loads(r.ai_findings)
        except json.JSONDecodeError:
            findings = [r.ai_findings]
    return {
        "id": r.id,
        "patient_id": r.patient_id,
        "modality": r.modality,
        "report_text": r.report_text,
        "ai_summary": r.ai_summary,
        "key_findings": findings,
        "urgency": r.urgency,
        "radiologist": r.radiologist,
        "reported_at": r.reported_at.isoformat(),
    }


@router.post("/reports")
def create_report(body: ReportCreate, db: Session = Depends(get_db)):
    r = m.RadiologyReport(
        patient_id=body.patient_id,
        study_id=body.study_id,
        modality=body.modality,
        report_text=body.report_text,
        radiologist=body.radiologist,
    )
    db.add(r)
    db.commit()
    db.refresh(r)
    return {"id": r.id}


@router.post("/reports/{report_id}/summarize")
def ai_summarize_report(report_id: int, db: Session = Depends(get_db)):
    try:
        return summarize_and_save(report_id, db)
    except ValueError as e:
        raise HTTPException(404, str(e)) from e


@router.post("/summarize")
def summarize_text(body: SummarizeBody):
    return summarize_radiology_report(body.report_text)
