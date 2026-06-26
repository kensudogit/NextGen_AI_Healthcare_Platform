package com.nghealth.platform.service.pacs;

import com.nghealth.platform.config.AppProperties;
import com.nghealth.platform.domain.ImagingStudy;
import com.nghealth.platform.domain.Patient;
import com.nghealth.platform.repository.ImagingStudyRepository;
import com.nghealth.platform.repository.PatientRepository;
import com.nghealth.platform.service.storage.StorageService;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DicomService {

    private final ImagingStudyRepository studyRepository;
    private final PatientRepository patientRepository;
    private final StorageService storageService;
    private final AppProperties appProperties;

    public DicomService(
            ImagingStudyRepository studyRepository,
            PatientRepository patientRepository,
            StorageService storageService,
            AppProperties appProperties) {
        this.studyRepository = studyRepository;
        this.patientRepository = patientRepository;
        this.storageService = storageService;
        this.appProperties = appProperties;
    }

    public List<Map<String, Object>> listStudies(Long patientId) {
        List<ImagingStudy> studies = patientId == null
                ? studyRepository.findAll()
                : studyRepository.findByPatientIdOrderByPerformedAtDesc(patientId);
        studies.sort(Comparator.comparing(ImagingStudy::getPerformedAt).reversed());
        return studies.stream().map(this::toMap).toList();
    }

    public Map<String, Object> ingest(byte[] data, Long patientId) throws IOException {
        patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        Attributes attrs = DicomAttributesReader.read(data);
        return persistStudy(data, attrs, patientId, 1, null);
    }

    /** PACS エクスポートフォルダ取り込み用 */
    public Map<String, Object> ingestFromExport(
            byte[] data, Attributes attrs, Long patientId, int instanceCount, String bodyPartOverride) throws IOException {
        return persistStudy(data, attrs, patientId, instanceCount, bodyPartOverride);
    }

    private Map<String, Object> persistStudy(
            byte[] data, Attributes attrs, Long patientId, int instanceCount, String bodyPartOverride) throws IOException {
        String studyUid = attrs.getString(Tag.StudyInstanceUID, UUID.randomUUID().toString());
        String modality = attrs.getString(Tag.Modality, "OT");
        String bodyPart = bodyPartOverride != null ? bodyPartOverride : attrs.getString(Tag.BodyPartExamined);
        if (bodyPart == null || bodyPart.isBlank()) {
            bodyPart = attrs.getString(Tag.SeriesDescription);
        }
        String description = attrs.getString(Tag.StudyDescription);
        Instant performed = parseStudyDate(attrs.getString(Tag.StudyDate));

        String key = "studies/" + studyUid.replace('.', '_') + ".dcm";
        String stored = storageService.storeDicom(data, key);

        String previewKey = storageService.previewKey(key);
        String previewPath = null;
        byte[] preview = DicomPreviewGenerator.toPng(data);
        if (preview != null) {
            storageService.storePreview(preview, previewKey);
            previewPath = appProperties.storage().useS3() ? previewKey : storageService.localPath(previewKey).toString();
        }

        ImagingStudy study = new ImagingStudy();
        study.setPatientId(patientId);
        study.setStudyUid(studyUid);
        study.setModality(modality);
        study.setBodyPart(bodyPart);
        study.setDescription(description);
        study.setInstanceCount(Math.max(1, instanceCount));
        study.setFilePath(stored);
        study.setPreviewPath(previewPath);
        study.setS3Key(appProperties.storage().useS3() ? key : null);
        study.setPerformedAt(performed);
        studyRepository.save(study);
        return toMap(study);
    }

    public ImagingStudy getStudy(Long id) {
        return studyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Study not found"));
    }

    public byte[] getDicomBytes(ImagingStudy study) throws IOException {
        if (study.getS3Key() != null && !study.getS3Key().isBlank()) {
            return storageService.readDicom(study.getS3Key());
        }
        String filePath = study.getFilePath();
        if (filePath == null || filePath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DICOM file not available");
        }
        if (!filePath.startsWith("s3://") && !Files.exists(Paths.get(filePath))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DICOM file not found");
        }
        return storageService.readDicom(filePath);
    }

    /** シード等でメタデータのみ存在する検査に DICOM / プレビューを補完 */
    public void backfillMissingStorage() throws IOException {
        for (ImagingStudy study : studyRepository.findAll()) {
            if (hasStoredFile(study)) {
                continue;
            }
            byte[] dicom = SampleDicomGenerator.create(
                    study.getStudyUid(),
                    study.getModality(),
                    study.getBodyPart(),
                    study.getDescription());
            String key = "studies/" + study.getStudyUid().replace('.', '_') + ".dcm";
            String stored = storageService.storeDicom(dicom, key);
            String previewKey = storageService.previewKey(key);
            String previewPath = null;
            byte[] preview = DicomPreviewGenerator.toPng(dicom);
            if (preview != null) {
                storageService.storePreview(preview, previewKey);
                previewPath = appProperties.storage().useS3()
                        ? previewKey
                        : storageService.localPath(previewKey).toString();
            }
            study.setFilePath(stored);
            study.setPreviewPath(previewPath);
            study.setS3Key(appProperties.storage().useS3() ? key : null);
            studyRepository.save(study);
        }
    }

    private boolean hasStoredFile(ImagingStudy study) {
        if (study.getS3Key() != null && !study.getS3Key().isBlank()) {
            return true;
        }
        String filePath = study.getFilePath();
        if (filePath == null || filePath.isBlank()) {
            return false;
        }
        if (filePath.startsWith("s3://")) {
            return true;
        }
        return Files.exists(Paths.get(filePath));
    }

    public byte[] getPreviewBytes(ImagingStudy study) throws IOException {
        if (study.getPreviewPath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Preview not available");
        }
        if (appProperties.storage().useS3()) {
            return storageService.readPreview(study.getPreviewPath());
        }
        return storageService.readPreview(study.getPreviewPath());
    }

    private Map<String, Object> toMap(ImagingStudy s) {
        String patientName = patientRepository.findById(s.getPatientId())
                .map(Patient::fullName)
                .orElse("Patient #" + s.getPatientId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("patient_id", s.getPatientId());
        m.put("patient_name", patientName);
        m.put("study_uid", s.getStudyUid());
        m.put("modality", s.getModality());
        m.put("body_part", s.getBodyPart());
        m.put("description", s.getDescription());
        m.put("instance_count", s.getInstanceCount());
        m.put("has_preview", s.getPreviewPath() != null);
        m.put("performed_at", s.getPerformedAt().toString());
        m.put("fhir_url", "/fhir/R4/ImagingStudy/" + s.getId());
        return m;
    }

    private Instant parseStudyDate(String studyDate) {
        if (studyDate == null || studyDate.isBlank()) {
            return Instant.now();
        }
        try {
            LocalDate d = LocalDate.parse(studyDate, DateTimeFormatter.BASIC_ISO_DATE);
            return d.atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
