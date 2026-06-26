"""SQLAlchemy models — EMR, PACS, Phone, Appointments"""

from datetime import date, datetime, timezone

from sqlalchemy import Boolean, Date, DateTime, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.db.database import Base


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


class Patient(Base):
    __tablename__ = "patients"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    mrn: Mapped[str] = mapped_column(String(32), unique=True, index=True)
    family_name: Mapped[str] = mapped_column(String(80))
    given_name: Mapped[str] = mapped_column(String(80))
    birth_date: Mapped[date] = mapped_column(Date)
    gender: Mapped[str] = mapped_column(String(10), default="unknown")
    phone: Mapped[str | None] = mapped_column(String(20))
    email: Mapped[str | None] = mapped_column(String(120))
    address: Mapped[str | None] = mapped_column(Text)
    allergies: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

    encounters: Mapped[list["Encounter"]] = relationship(back_populates="patient")
    appointments: Mapped[list["Appointment"]] = relationship(back_populates="patient")
    imaging_studies: Mapped[list["ImagingStudy"]] = relationship(back_populates="patient")
    radiology_reports: Mapped[list["RadiologyReport"]] = relationship(back_populates="patient")


class Encounter(Base):
    __tablename__ = "encounters"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    patient_id: Mapped[int] = mapped_column(ForeignKey("patients.id"), index=True)
    encounter_type: Mapped[str] = mapped_column(String(40), default="outpatient")
    status: Mapped[str] = mapped_column(String(20), default="finished")
    chief_complaint: Mapped[str | None] = mapped_column(Text)
    diagnosis: Mapped[str | None] = mapped_column(Text)
    provider_name: Mapped[str | None] = mapped_column(String(80))
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    ended_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))

    patient: Mapped["Patient"] = relationship(back_populates="encounters")
    notes: Mapped[list["ClinicalNote"]] = relationship(back_populates="encounter")


class ClinicalNote(Base):
    __tablename__ = "clinical_notes"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    encounter_id: Mapped[int] = mapped_column(ForeignKey("encounters.id"), index=True)
    note_type: Mapped[str] = mapped_column(String(40), default="progress")
    author: Mapped[str] = mapped_column(String(80))
    content: Mapped[str] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

    encounter: Mapped["Encounter"] = relationship(back_populates="notes")


class Appointment(Base):
    __tablename__ = "appointments"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    patient_id: Mapped[int | None] = mapped_column(ForeignKey("patients.id"), index=True)
    patient_name: Mapped[str] = mapped_column(String(120))
    patient_phone: Mapped[str | None] = mapped_column(String(20))
    department: Mapped[str] = mapped_column(String(80), default="内科")
    purpose: Mapped[str] = mapped_column(String(200))
    status: Mapped[str] = mapped_column(String(20), default="booked")
    scheduled_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    source: Mapped[str] = mapped_column(String(30), default="staff")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

    patient: Mapped["Patient | None"] = relationship(back_populates="appointments")


class ImagingStudy(Base):
    __tablename__ = "imaging_studies"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    patient_id: Mapped[int] = mapped_column(ForeignKey("patients.id"), index=True)
    study_uid: Mapped[str] = mapped_column(String(128), unique=True, index=True)
    modality: Mapped[str] = mapped_column(String(10))
    body_part: Mapped[str | None] = mapped_column(String(80))
    description: Mapped[str | None] = mapped_column(String(200))
    instance_count: Mapped[int] = mapped_column(Integer, default=1)
    file_path: Mapped[str | None] = mapped_column(String(500))
    preview_path: Mapped[str | None] = mapped_column(String(500))
    performed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

    patient: Mapped["Patient"] = relationship(back_populates="imaging_studies")


class RadiologyReport(Base):
    __tablename__ = "radiology_reports"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    patient_id: Mapped[int] = mapped_column(ForeignKey("patients.id"), index=True)
    study_id: Mapped[int | None] = mapped_column(ForeignKey("imaging_studies.id"))
    modality: Mapped[str] = mapped_column(String(10))
    report_text: Mapped[str] = mapped_column(Text)
    ai_summary: Mapped[str | None] = mapped_column(Text)
    ai_findings: Mapped[str | None] = mapped_column(Text)
    urgency: Mapped[str | None] = mapped_column(String(20))
    radiologist: Mapped[str | None] = mapped_column(String(80))
    reported_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

    patient: Mapped["Patient"] = relationship(back_populates="radiology_reports")


class PhoneCall(Base):
    __tablename__ = "phone_calls"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    call_sid: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    caller_number: Mapped[str | None] = mapped_column(String(20))
    intent: Mapped[str | None] = mapped_column(String(40))
    status: Mapped[str] = mapped_column(String(20), default="active")
    summary: Mapped[str | None] = mapped_column(Text)
    appointment_id: Mapped[int | None] = mapped_column(ForeignKey("appointments.id"))
    transferred: Mapped[bool] = mapped_column(Boolean, default=False)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    ended_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))

    turns: Mapped[list["PhoneTurn"]] = relationship(back_populates="call", order_by="PhoneTurn.seq")


class PhoneTurn(Base):
    __tablename__ = "phone_turns"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    call_id: Mapped[int] = mapped_column(ForeignKey("phone_calls.id"), index=True)
    seq: Mapped[int] = mapped_column(Integer)
    role: Mapped[str] = mapped_column(String(10))
    text: Mapped[str] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

    call: Mapped["PhoneCall"] = relationship(back_populates="turns")
