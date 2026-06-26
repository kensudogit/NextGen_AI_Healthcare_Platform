"""職員ダッシュボード API"""

from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends
from sqlalchemy import func
from sqlalchemy.orm import Session

from src.config import settings
from src.db import models as m
from src.db.database import get_db

router = APIRouter(prefix="/api/dashboard", tags=["Dashboard"])


@router.get("/stats")
def dashboard_stats(db: Session = Depends(get_db)):
    now = datetime.now(timezone.utc)
    today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)

    return {
        "hospital_name": settings.hospital_name,
        "generated_at": now.isoformat(),
        "counts": {
            "patients": db.query(func.count(m.Patient.id)).scalar() or 0,
            "appointments_today": db.query(func.count(m.Appointment.id))
            .filter(m.Appointment.scheduled_at >= today_start)
            .filter(m.Appointment.scheduled_at < today_start + timedelta(days=1))
            .scalar()
            or 0,
            "appointments_upcoming": db.query(func.count(m.Appointment.id))
            .filter(m.Appointment.scheduled_at >= now)
            .filter(m.Appointment.status == "booked")
            .scalar()
            or 0,
            "imaging_studies": db.query(func.count(m.ImagingStudy.id)).scalar() or 0,
            "radiology_reports": db.query(func.count(m.RadiologyReport.id)).scalar() or 0,
            "reports_pending_summary": db.query(func.count(m.RadiologyReport.id))
            .filter(m.RadiologyReport.ai_summary.is_(None))
            .scalar()
            or 0,
            "phone_calls_today": db.query(func.count(m.PhoneCall.id))
            .filter(m.PhoneCall.started_at >= today_start)
            .scalar()
            or 0,
            "active_phone_calls": db.query(func.count(m.PhoneCall.id))
            .filter(m.PhoneCall.status == "active")
            .scalar()
            or 0,
        },
        "recent_appointments": [
            {
                "id": a.id,
                "patient_name": a.patient_name,
                "department": a.department,
                "purpose": a.purpose,
                "scheduled_at": a.scheduled_at.isoformat(),
                "source": a.source,
            }
            for a in db.query(m.Appointment)
            .filter(m.Appointment.scheduled_at >= now)
            .order_by(m.Appointment.scheduled_at)
            .limit(5)
            .all()
        ],
        "recent_calls": [
            {
                "id": c.id,
                "intent": c.intent,
                "status": c.status,
                "summary": (c.summary or "")[:120],
                "started_at": c.started_at.isoformat(),
            }
            for c in db.query(m.PhoneCall).order_by(m.PhoneCall.started_at.desc()).limit(5).all()
        ],
        "integrations": {
            "fhir_base": "/fhir/R4",
            "emr_api": "/api/emr",
            "pacs_api": "/api/pacs",
            "phone_webhook": "/api/phone",
        },
    }


@router.get("/appointments")
def list_appointments(db: Session = Depends(get_db)):
    rows = db.query(m.Appointment).order_by(m.Appointment.scheduled_at.desc()).limit(50).all()
    return {
        "appointments": [
            {
                "id": a.id,
                "patient_id": a.patient_id,
                "patient_name": a.patient_name,
                "department": a.department,
                "purpose": a.purpose,
                "status": a.status,
                "scheduled_at": a.scheduled_at.isoformat(),
                "source": a.source,
            }
            for a in rows
        ]
    }
