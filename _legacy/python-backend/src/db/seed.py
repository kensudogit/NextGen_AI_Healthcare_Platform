"""Initial demo data"""

from datetime import date, datetime, timedelta, timezone

from sqlalchemy.orm import Session

from src.db.models import (
    Appointment,
    ClinicalNote,
    Encounter,
    ImagingStudy,
    Patient,
    RadiologyReport,
)


def seed_demo_data(db: Session) -> None:
    if db.query(Patient).first():
        return

    patients = [
        Patient(
            mrn="MRN-10001",
            family_name="田中",
            given_name="太郎",
            birth_date=date(1985, 4, 12),
            gender="male",
            phone="090-1234-5678",
            email="tanaka@example.com",
            allergies="ペニシリン",
        ),
        Patient(
            mrn="MRN-10002",
            family_name="佐藤",
            given_name="花子",
            birth_date=date(1972, 8, 3),
            gender="female",
            phone="080-9876-5432",
            allergies=None,
        ),
        Patient(
            mrn="MRN-10003",
            family_name="鈴木",
            given_name="一郎",
            birth_date=date(1990, 11, 25),
            gender="male",
            phone="070-5555-1111",
        ),
    ]
    db.add_all(patients)
    db.flush()

    enc = Encounter(
        patient_id=patients[0].id,
        encounter_type="outpatient",
        chief_complaint="咳・発熱3日",
        diagnosis="急性上気道炎",
        provider_name="Dr. 山田",
        started_at=datetime.now(timezone.utc) - timedelta(days=2),
    )
    db.add(enc)
    db.flush()
    db.add(
        ClinicalNote(
            encounter_id=enc.id,
            note_type="progress",
            author="Dr. 山田",
            content="38.2°C。咽頭発赤あり。抗菌薬は不要。対症療法を指示。",
        )
    )

    now = datetime.now(timezone.utc)
    db.add_all(
        [
            Appointment(
                patient_id=patients[0].id,
                patient_name="田中 太郎",
                patient_phone="090-1234-5678",
                department="内科",
                purpose="定期受診",
                scheduled_at=now + timedelta(days=3),
                source="staff",
            ),
            Appointment(
                patient_id=patients[1].id,
                patient_name="佐藤 花子",
                patient_phone="080-9876-5432",
                department="整形外科",
                purpose="膝の痛み",
                scheduled_at=now + timedelta(days=1),
                source="phone_ai",
            ),
        ]
    )

    study = ImagingStudy(
        patient_id=patients[1].id,
        study_uid="1.2.840.113619.2.55.3.604688776.969.1740000000.1",
        modality="CT",
        body_part="CHEST",
        description="胸部CT 造影なし",
        instance_count=1,
        performed_at=now - timedelta(hours=6),
    )
    db.add(study)
    db.flush()

    db.add(
        RadiologyReport(
            patient_id=patients[1].id,
            study_id=study.id,
            modality="CT",
            report_text=(
                "【所見】\n"
                "両肺野にすりガラス影を認める。右下葉に5mmの結節影。\n"
                "心拡大なし。胸水なし。\n"
                "【印象】\n"
                "両肺すりガラス影 — 感染症 vs 間質性肺炎の鑑別。\n"
                "右下葉結節 — 3-6ヶ月後フォローCT推奨。"
            ),
            radiologist="Dr. 放射 一郎",
        )
    )
    db.commit()
