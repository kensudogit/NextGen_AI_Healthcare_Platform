package com.nghealth.platform.web;

import com.nghealth.platform.domain.Patient;
import com.nghealth.platform.domain.RadiologyReport;
import com.nghealth.platform.repository.PatientRepository;
import com.nghealth.platform.repository.RadiologyReportRepository;
import com.nghealth.platform.service.ai.RadiologyAiService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/radiology")
public class RadiologyController {

    private final RadiologyReportRepository reportRepository;
    private final PatientRepository patientRepository;
    private final RadiologyAiService radiologyAiService;

    public RadiologyController(
            RadiologyReportRepository reportRepository,
            PatientRepository patientRepository,
            RadiologyAiService radiologyAiService) {
        this.reportRepository = reportRepository;
        this.patientRepository = patientRepository;
        this.radiologyAiService = radiologyAiService;
    }

    @GetMapping("/reports")
    public Map<String, Object> listReports() {
        List<Map<String, Object>> reports = reportRepository.findAll().stream()
                .sorted((a, b) -> b.getReportedAt().compareTo(a.getReportedAt()))
                .map(this::reportMap)
                .toList();
        return Map.of("reports", reports);
    }

    @PostMapping("/reports/{id}/summarize")
    public Map<String, Object> summarize(@PathVariable Long id) {
        return radiologyAiService.summarizeAndSave(id);
    }

    public record SummarizeTextRequest(String reportText) {}

    @PostMapping("/summarize")
    public Map<String, Object> summarizeText(@RequestBody SummarizeTextRequest body) {
        return radiologyAiService.summarize(body.reportText());
    }

    private Map<String, Object> reportMap(RadiologyReport r) {
        String patientName = patientRepository.findById(r.getPatientId()).map(Patient::fullName).orElse("—");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("patient_id", r.getPatientId());
        m.put("patient_name", patientName);
        m.put("modality", r.getModality());
        m.put("report_text", r.getReportText());
        m.put("ai_summary", r.getAiSummary());
        m.put("ai_findings", r.getAiFindings());
        m.put("urgency", r.getUrgency());
        m.put("radiologist", r.getRadiologist());
        m.put("reported_at", r.getReportedAt().toString());
        m.put("fhir_url", "/fhir/R4/DiagnosticReport/" + r.getId());
        return m;
    }
}
