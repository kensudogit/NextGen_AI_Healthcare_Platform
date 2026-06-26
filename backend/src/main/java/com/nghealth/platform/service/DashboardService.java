package com.nghealth.platform.service;

import com.nghealth.platform.config.AppProperties;
import com.nghealth.platform.domain.*;
import com.nghealth.platform.repository.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DashboardService {

    private final AppProperties appProperties;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ImagingStudyRepository imagingStudyRepository;
    private final RadiologyReportRepository radiologyReportRepository;
    private final PhoneCallRepository phoneCallRepository;

    public DashboardService(
            AppProperties appProperties,
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            ImagingStudyRepository imagingStudyRepository,
            RadiologyReportRepository radiologyReportRepository,
            PhoneCallRepository phoneCallRepository) {
        this.appProperties = appProperties;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.imagingStudyRepository = imagingStudyRepository;
        this.radiologyReportRepository = radiologyReportRepository;
        this.phoneCallRepository = phoneCallRepository;
    }

    public Map<String, Object> stats() {
        Instant now = Instant.now();
        Instant todayStart = now.truncatedTo(ChronoUnit.DAYS);

        Map<String, Object> counts = Map.of(
                "patients", patientRepository.count(),
                "appointments_today", appointmentRepository.countByScheduledAtBetween(todayStart, todayStart.plus(1, ChronoUnit.DAYS)),
                "appointments_upcoming", appointmentRepository.countByScheduledAtAfterAndStatus(now, "booked"),
                "imaging_studies", imagingStudyRepository.count(),
                "radiology_reports", radiologyReportRepository.count(),
                "reports_pending_summary", radiologyReportRepository.countByAiSummaryIsNull(),
                "phone_calls_today", phoneCallRepository.countByStartedAtAfter(todayStart),
                "active_phone_calls", phoneCallRepository.countByStatus("active"));

        List<Map<String, Object>> recentAppointments = appointmentRepository
                .findByScheduledAtAfterOrderByScheduledAtAsc(now).stream().limit(5)
                .map(a -> Map.<String, Object>of(
                        "id", a.getId(),
                        "patient_name", a.getPatientName(),
                        "department", a.getDepartment(),
                        "purpose", a.getPurpose(),
                        "scheduled_at", a.getScheduledAt().toString(),
                        "source", a.getSource()))
                .toList();

        List<Map<String, Object>> recentCalls = phoneCallRepository.findTop30ByOrderByStartedAtDesc().stream().limit(5)
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("intent", c.getIntent());
                    m.put("status", c.getStatus());
                    m.put("summary", c.getSummary() != null ? c.getSummary().substring(0, Math.min(120, c.getSummary().length())) : "");
                    m.put("started_at", c.getStartedAt().toString());
                    return m;
                }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hospital_name", appProperties.hospitalName());
        result.put("generated_at", now.toString());
        result.put("counts", counts);
        result.put("recent_appointments", recentAppointments);
        result.put("recent_calls", recentCalls);
        result.put("integrations", Map.of(
                "fhir_base", "/fhir/R4",
                "hl7_api", "/api/hl7",
                "emr_api", "/api/emr",
                "pacs_api", "/api/pacs",
                "phone_webhook", "/api/phone",
                "aws_s3", appProperties.storage().useS3() ? "enabled" : "local"));
        return result;
    }

    public List<Map<String, Object>> listAppointments() {
        return appointmentRepository.findAll().stream()
                .sorted(Comparator.comparing(Appointment::getScheduledAt).reversed())
                .limit(50)
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("patient_id", a.getPatientId());
                    m.put("patient_name", a.getPatientName());
                    m.put("department", a.getDepartment());
                    m.put("purpose", a.getPurpose());
                    m.put("status", a.getStatus());
                    m.put("scheduled_at", a.getScheduledAt().toString());
                    m.put("source", a.getSource());
                    return m;
                }).toList();
    }
}
