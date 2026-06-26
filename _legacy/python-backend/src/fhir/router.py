"""FHIR R4 REST API"""

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from src.db import models as m
from src.db.database import get_db
from src.fhir.service import (
    appointment_to_fhir,
    diagnostic_report_to_fhir,
    imaging_study_to_fhir,
    patient_to_fhir,
    search_patients,
)

router = APIRouter(prefix="/fhir/R4", tags=["FHIR"])


@router.get("/metadata")
def capability_statement():
    return {
        "resourceType": "CapabilityStatement",
        "status": "active",
        "fhirVersion": "4.0.1",
        "format": ["json"],
        "rest": [
            {
                "mode": "server",
                "resource": [
                    {"type": "Patient", "interaction": [{"code": "read"}, {"code": "search-type"}]},
                    {"type": "Appointment", "interaction": [{"code": "read"}, {"code": "search-type"}]},
                    {"type": "ImagingStudy", "interaction": [{"code": "read"}, {"code": "search-type"}]},
                    {"type": "DiagnosticReport", "interaction": [{"code": "read"}, {"code": "search-type"}]},
                ],
            }
        ],
    }


@router.get("/Patient")
def search_patient(name: str | None = Query(None), db: Session = Depends(get_db)):
    return search_patients(db, name).model_dump(exclude_none=True)


@router.get("/Patient/{patient_id}")
def read_patient(patient_id: int, db: Session = Depends(get_db)):
    p = db.get(m.Patient, patient_id)
    if not p:
        raise HTTPException(404, "Patient not found")
    return patient_to_fhir(p).model_dump(exclude_none=True)


@router.get("/Appointment")
def search_appointment(db: Session = Depends(get_db)):
    rows = db.query(m.Appointment).order_by(m.Appointment.scheduled_at.desc()).limit(50).all()
    entries = [
        {
            "fullUrl": f"http://localhost:8010/fhir/R4/Appointment/{a.id}",
            "resource": appointment_to_fhir(a).model_dump(exclude_none=True),
        }
        for a in rows
    ]
    return {"resourceType": "Bundle", "type": "searchset", "total": len(entries), "entry": entries}


@router.get("/ImagingStudy")
def search_imaging(patient: int | None = Query(None), db: Session = Depends(get_db)):
    q = db.query(m.ImagingStudy)
    if patient:
        q = q.filter(m.ImagingStudy.patient_id == patient)
    rows = q.limit(50).all()
    entries = [
        {
            "fullUrl": f"http://localhost:8010/fhir/R4/ImagingStudy/{s.id}",
            "resource": imaging_study_to_fhir(s).model_dump(exclude_none=True),
        }
        for s in rows
    ]
    return {"resourceType": "Bundle", "type": "searchset", "total": len(entries), "entry": entries}


@router.get("/DiagnosticReport")
def search_diagnostic_report(db: Session = Depends(get_db)):
    rows = db.query(m.RadiologyReport).limit(50).all()
    entries = [
        {
            "fullUrl": f"http://localhost:8010/fhir/R4/DiagnosticReport/{r.id}",
            "resource": diagnostic_report_to_fhir(r).model_dump(exclude_none=True),
        }
        for r in rows
    ]
    return {"resourceType": "Bundle", "type": "searchset", "total": len(entries), "entry": entries}
