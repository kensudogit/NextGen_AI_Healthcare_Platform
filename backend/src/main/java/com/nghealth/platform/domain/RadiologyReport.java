package com.nghealth.platform.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "radiology_reports")
public class RadiologyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    private Long studyId;

    @Column(nullable = false, length = 10)
    private String modality;

    @Column(nullable = false, columnDefinition = "text")
    private String reportText;

    @Column(columnDefinition = "text")
    private String aiSummary;

    @Column(columnDefinition = "text")
    private String aiFindings;

    @Column(length = 20)
    private String urgency;

    @Column(length = 80)
    private String radiologist;

    @Column(nullable = false)
    private Instant reportedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getStudyId() { return studyId; }
    public void setStudyId(Long studyId) { this.studyId = studyId; }
    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }
    public String getReportText() { return reportText; }
    public void setReportText(String reportText) { this.reportText = reportText; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public String getAiFindings() { return aiFindings; }
    public void setAiFindings(String aiFindings) { this.aiFindings = aiFindings; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getRadiologist() { return radiologist; }
    public void setRadiologist(String radiologist) { this.radiologist = radiologist; }
    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
}
