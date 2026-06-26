"""電子カルテ API"""

from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from src.db import models as m
from src.db.database import get_db

router = APIRouter(prefix="/api/emr", tags=["EMR"])


class PatientCreate(BaseModel):
    mrn: str
    family_name: str
    given_name: str
    birth_date: str
    gender: str = "unknown"
    phone: str | None = None
    email: str | None = None
    allergies: str | None = None


class EncounterCreate(BaseModel):
    patient_id: int
    chief_complaint: str | None = None
    diagnosis: str | None = None
    provider_name: str = "Dr. Staff"


class NoteCreate(BaseModel):
    encounter_id: int
    note_type: str = "progress"
    author: str
    content: str


def _patient_dict(p: m.Patient) -> dict:
    return {
        "id": p.id,
        "mrn": p.mrn,
        "name": f"{p.family_name} {p.given_name}",
        "family_name": p.family_name,
        "given_name": p.given_name,
        "birth_date": p.birth_date.isoformat(),
        "gender": p.gender,
        "phone": p.phone,
        "email": p.email,
        "allergies": p.allergies,
        "fhir_url": f"/fhir/R4/Patient/{p.id}",
    }


@router.get("/patients")
def list_patients(db: Session = Depends(get_db)):
    rows = db.query(m.Patient).order_by(m.Patient.id).all()
    return {"patients": [_patient_dict(p) for p in rows]}


@router.get("/patients/{patient_id}")
def get_patient(patient_id: int, db: Session = Depends(get_db)):
    p = db.get(m.Patient, patient_id)
    if not p:
        raise HTTPException(404, "Patient not found")
    encounters = db.query(m.Encounter).filter(m.Encounter.patient_id == patient_id).all()
    notes = []
    for e in encounters:
        for n in e.notes:
            notes.append(
                {
                    "id": n.id,
                    "encounter_id": e.id,
                    "note_type": n.note_type,
                    "author": n.author,
                    "content": n.content,
                    "created_at": n.created_at.isoformat(),
                }
            )
    return {
        "patient": _patient_dict(p),
        "encounters": [
            {
                "id": e.id,
                "type": e.encounter_type,
                "chief_complaint": e.chief_complaint,
                "diagnosis": e.diagnosis,
                "provider": e.provider_name,
                "started_at": e.started_at.isoformat(),
            }
            for e in encounters
        ],
        "notes": notes,
    }


@router.post("/patients")
def create_patient(body: PatientCreate, db: Session = Depends(get_db)):
    p = m.Patient(
        mrn=body.mrn,
        family_name=body.family_name,
        given_name=body.given_name,
        birth_date=datetime.fromisoformat(body.birth_date).date(),
        gender=body.gender,
        phone=body.phone,
        email=body.email,
        allergies=body.allergies,
    )
    db.add(p)
    db.commit()
    db.refresh(p)
    return _patient_dict(p)


@router.post("/encounters")
def create_encounter(body: EncounterCreate, db: Session = Depends(get_db)):
    if not db.get(m.Patient, body.patient_id):
        raise HTTPException(404, "Patient not found")
    e = m.Encounter(
        patient_id=body.patient_id,
        chief_complaint=body.chief_complaint,
        diagnosis=body.diagnosis,
        provider_name=body.provider_name,
    )
    db.add(e)
    db.commit()
    db.refresh(e)
    return {"id": e.id, "patient_id": e.patient_id}


@router.post("/notes")
def create_note(body: NoteCreate, db: Session = Depends(get_db)):
    if not db.get(m.Encounter, body.encounter_id):
        raise HTTPException(404, "Encounter not found")
    n = m.ClinicalNote(
        encounter_id=body.encounter_id,
        note_type=body.note_type,
        author=body.author,
        content=body.content,
    )
    db.add(n)
    db.commit()
    db.refresh(n)
    return {"id": n.id}
