"""FHIR R4 resource mapping"""

from datetime import datetime

from fhir.resources.appointment import Appointment as FhirAppointment
from fhir.resources.bundle import Bundle, BundleEntry
from fhir.resources.diagnosticreport import DiagnosticReport
from fhir.resources.humanname import HumanName
from fhir.resources.imagingstudy import ImagingStudy as FhirImagingStudy
from fhir.resources.meta import Meta
from fhir.resources.patient import Patient as FhirPatient
from sqlalchemy.orm import Session

from src.db import models as m

BASE_URL = "http://localhost:8010/fhir/R4"


def _meta() -> Meta:
    return Meta(versionId="1", lastUpdated=datetime.utcnow())


def patient_to_fhir(p: m.Patient) -> FhirPatient:
    return FhirPatient(
        id=str(p.id),
        meta=_meta(),
        identifier=[{"system": "http://nextgen.health/mrn", "value": p.mrn}],
        name=[HumanName(family=p.family_name, given=[p.given_name])],
        gender=p.gender if p.gender in ("male", "female", "other", "unknown") else "unknown",
        birthDate=p.birth_date.isoformat(),
        telecom=[
            t
            for t in [
                {"system": "phone", "value": p.phone, "use": "mobile"} if p.phone else None,
                {"system": "email", "value": p.email} if p.email else None,
            ]
            if t
        ],
    )


def appointment_to_fhir(a: m.Appointment) -> FhirAppointment:
    return FhirAppointment(
        id=str(a.id),
        meta=_meta(),
        status=a.status if a.status in ("booked", "cancelled", "fulfilled") else "booked",
        description=a.purpose,
        start=a.scheduled_at.isoformat(),
        participant=[
            {
                "actor": {"display": a.patient_name},
                "status": "accepted",
            }
        ],
        serviceType=[{"text": a.department}],
    )


def imaging_study_to_fhir(s: m.ImagingStudy) -> FhirImagingStudy:
    return FhirImagingStudy(
        id=str(s.id),
        meta=_meta(),
        status="available",
        modality=[{"system": "http://dicom.nema.org/resources/ontology/DCM", "code": s.modality}],
        subject={"reference": f"Patient/{s.patient_id}"},
        started=s.performed_at.isoformat(),
        uid=s.study_uid,
        numberOfSeries=1,
        numberOfInstances=s.instance_count,
        description=s.description,
    )


def diagnostic_report_to_fhir(r: m.RadiologyReport) -> DiagnosticReport:
    return DiagnosticReport(
        id=str(r.id),
        meta=_meta(),
        status="final",
        category=[
            {
                "coding": [
                    {
                        "system": "http://terminology.hl7.org/CodeSystem/v2-0074",
                        "code": "RAD",
                        "display": "Radiology",
                    }
                ]
            }
        ],
        code={"text": f"{r.modality} Report"},
        subject={"reference": f"Patient/{r.patient_id}"},
        effectiveDateTime=r.reported_at.isoformat(),
        conclusion=r.ai_summary or r.report_text[:500],
        presentedForm=[{"contentType": "text/plain", "data": r.report_text.encode().hex()}],
    )


def bundle_from_resources(resources: list, resource_type: str) -> Bundle:
    entries = [
        BundleEntry(fullUrl=f"{BASE_URL}/{resource_type}/{r.id}", resource=res)
        for r, res in resources
    ]
    return Bundle(type="searchset", total=len(entries), entry=entries)


def search_patients(db: Session, name: str | None = None) -> Bundle:
    q = db.query(m.Patient)
    if name:
        q = q.filter(
            (m.Patient.family_name.contains(name)) | (m.Patient.given_name.contains(name))
        )
    rows = q.limit(50).all()
    return bundle_from_resources([(p, patient_to_fhir(p)) for p in rows], "Patient")
