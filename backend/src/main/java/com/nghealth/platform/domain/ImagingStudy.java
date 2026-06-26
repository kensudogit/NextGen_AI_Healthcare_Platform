package com.nghealth.platform.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "imaging_studies")
public class ImagingStudy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false, unique = true, length = 128)
    private String studyUid;

    @Column(nullable = false, length = 10)
    private String modality;

    @Column(length = 80)
    private String bodyPart;

    @Column(length = 200)
    private String description;

    private int instanceCount = 1;

    @Column(length = 500)
    private String filePath;

    @Column(length = 500)
    private String previewPath;

    @Column(length = 500)
    private String s3Key;

    @Column(nullable = false)
    private Instant performedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getStudyUid() { return studyUid; }
    public void setStudyUid(String studyUid) { this.studyUid = studyUid; }
    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }
    public String getBodyPart() { return bodyPart; }
    public void setBodyPart(String bodyPart) { this.bodyPart = bodyPart; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getInstanceCount() { return instanceCount; }
    public void setInstanceCount(int instanceCount) { this.instanceCount = instanceCount; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getPreviewPath() { return previewPath; }
    public void setPreviewPath(String previewPath) { this.previewPath = previewPath; }
    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public Instant getPerformedAt() { return performedAt; }
    public void setPerformedAt(Instant performedAt) { this.performedAt = performedAt; }
}
