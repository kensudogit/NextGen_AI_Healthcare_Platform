package com.nghealth.platform.service;

import com.nghealth.platform.domain.*;
import com.nghealth.platform.repository.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class DataSeedService {

    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final ClinicalNoteRepository noteRepository;
    private final AppointmentRepository appointmentRepository;
    private final ImagingStudyRepository imagingStudyRepository;
    private final RadiologyReportRepository radiologyReportRepository;

    public DataSeedService(
            PatientRepository patientRepository,
            EncounterRepository encounterRepository,
            ClinicalNoteRepository noteRepository,
            AppointmentRepository appointmentRepository,
            ImagingStudyRepository imagingStudyRepository,
            RadiologyReportRepository radiologyReportRepository) {
        this.patientRepository = patientRepository;
        this.encounterRepository = encounterRepository;
        this.noteRepository = noteRepository;
        this.appointmentRepository = appointmentRepository;
        this.imagingStudyRepository = imagingStudyRepository;
        this.radiologyReportRepository = radiologyReportRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (patientRepository.count() > 0) {
            return;
        }

        Patient p1 = savePatient("MRN-10001", "田中", "太郎", LocalDate.of(1985, 4, 12), "male", "090-1234-5678", "tanaka@example.com", "ペニシリン");
        Patient p2 = savePatient("MRN-10002", "佐藤", "花子", LocalDate.of(1972, 8, 3), "female", "080-9876-5432", null, null);
        savePatient("MRN-10003", "鈴木", "一郎", LocalDate.of(1990, 11, 25), "male", "070-5555-1111", null, null);

        Encounter enc = new Encounter();
        enc.setPatientId(p1.getId());
        enc.setEncounterType("outpatient");
        enc.setChiefComplaint("咳・発熱3日");
        enc.setDiagnosis("急性上気道炎");
        enc.setProviderName("Dr. 山田");
        enc.setStartedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        encounterRepository.save(enc);

        ClinicalNote note = new ClinicalNote();
        note.setEncounterId(enc.getId());
        note.setNoteType("progress");
        note.setAuthor("Dr. 山田");
        note.setContent("38.2°C。咽頭発赤あり。抗菌薬は不要。対症療法を指示。");
        noteRepository.save(note);

        Instant now = Instant.now();
        Appointment a1 = new Appointment();
        a1.setPatientId(p1.getId());
        a1.setPatientName("田中 太郎");
        a1.setPatientPhone("090-1234-5678");
        a1.setDepartment("内科");
        a1.setPurpose("定期受診");
        a1.setScheduledAt(now.plus(3, ChronoUnit.DAYS));
        a1.setSource("staff");
        appointmentRepository.save(a1);

        Appointment a2 = new Appointment();
        a2.setPatientId(p2.getId());
        a2.setPatientName("佐藤 花子");
        a2.setPatientPhone("080-9876-5432");
        a2.setDepartment("整形外科");
        a2.setPurpose("膝の痛み");
        a2.setScheduledAt(now.plus(1, ChronoUnit.DAYS));
        a2.setSource("phone_ai");
        appointmentRepository.save(a2);

        ImagingStudy study = new ImagingStudy();
        study.setPatientId(p2.getId());
        study.setStudyUid("1.2.840.113619.2.55.3.604688776.969.1740000000.1");
        study.setModality("CT");
        study.setBodyPart("CHEST");
        study.setDescription("胸部CT 造影なし");
        study.setPerformedAt(now.minus(6, ChronoUnit.HOURS));
        imagingStudyRepository.save(study);

        RadiologyReport report = new RadiologyReport();
        report.setPatientId(p2.getId());
        report.setStudyId(study.getId());
        report.setModality("CT");
        report.setReportText("""
                【所見】
                両肺野にすりガラス影を認める。右下葉に5mmの結節影。
                心拡大なし。胸水なし。
                【印象】
                両肺すりガラス影 — 感染症 vs 間質性肺炎の鑑別。
                右下葉結節 — 3-6ヶ月後フォローCT推奨。
                """);
        report.setRadiologist("Dr. 放射 一郎");
        radiologyReportRepository.save(report);
    }

    private Patient savePatient(String mrn, String family, String given, LocalDate birth, String gender, String phone, String email, String allergies) {
        Patient p = new Patient();
        p.setMrn(mrn);
        p.setFamilyName(family);
        p.setGivenName(given);
        p.setBirthDate(birth);
        p.setGender(gender);
        p.setPhone(phone);
        p.setEmail(email);
        p.setAllergies(allergies);
        return patientRepository.save(p);
    }
}
