package com.nghealth.platform.service.pacs;

import com.nghealth.platform.domain.Patient;
import com.nghealth.platform.repository.ImagingStudyRepository;
import com.nghealth.platform.repository.PatientRepository;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

/**
 * PACS エクスポート形式の取り込み:
 * PATIENT_xxx/STUDY_xxx/SERIES_xxx/IMG000001.dcm
 */
@Service
public class PacsExportImporter {

    private final DicomService dicomService;
    private final PatientRepository patientRepository;
    private final ImagingStudyRepository studyRepository;

    public PacsExportImporter(
            DicomService dicomService,
            PatientRepository patientRepository,
            ImagingStudyRepository studyRepository) {
        this.dicomService = dicomService;
        this.patientRepository = patientRepository;
        this.studyRepository = studyRepository;
    }

    public Map<String, Object> importFolder(Path root) throws IOException {
        if (root == null || !Files.isDirectory(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export folder not found: " + root);
        }

        Map<String, List<Path>> filesByStudy = new LinkedHashMap<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".dcm"))
                    .forEach(path -> {
                        try {
                            Attributes attrs = DicomAttributesReader.read(path);
                            String studyUid = attrs.getString(Tag.StudyInstanceUID);
                            if (studyUid == null || studyUid.isBlank()) {
                                return;
                            }
                            filesByStudy.computeIfAbsent(studyUid, k -> new ArrayList<>()).add(path);
                        } catch (IOException ignored) {
                        }
                    });
        }

        int imported = 0;
        int skipped = 0;
        List<Map<String, Object>> studies = new ArrayList<>();

        for (Map.Entry<String, List<Path>> entry : filesByStudy.entrySet()) {
            if (studyRepository.findByStudyUid(entry.getKey()).isPresent()) {
                skipped++;
                continue;
            }
            List<Path> files = entry.getValue();
            files.sort(Comparator.comparing(p -> p.getFileName().toString()));
            Path primary = files.getFirst();
            byte[] data = Files.readAllBytes(primary);
            Attributes attrs = DicomAttributesReader.read(data);
            Patient patient = resolvePatient(attrs);
            Map<String, Object> study = dicomService.ingestFromExport(
                    data, attrs, patient.getId(), files.size(), inferBodyPart(attrs, primary));
            studies.add(study);
            imported++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("root", root.toString());
        result.put("studies_found", filesByStudy.size());
        result.put("imported", imported);
        result.put("skipped_existing", skipped);
        result.put("studies", studies);
        return result;
    }

    private Patient resolvePatient(Attributes attrs) {
        String mrn = Optional.ofNullable(attrs.getString(Tag.PatientID))
                .filter(s -> !s.isBlank())
                .orElse("PACS-" + UUID.randomUUID().toString().substring(0, 8));

        return patientRepository.findByMrn(mrn).orElseGet(() -> {
            String[] name = parsePatientName(attrs.getString(Tag.PatientName));
            Patient p = new Patient();
            p.setMrn(mrn);
            p.setFamilyName(name[0]);
            p.setGivenName(name[1]);
            p.setBirthDate(LocalDate.of(1970, 1, 1));
            p.setGender("unknown");
            return patientRepository.save(p);
        });
    }

    private static String[] parsePatientName(String patientName) {
        if (patientName == null || patientName.isBlank()) {
            return new String[]{"Unknown", "Patient"};
        }
        String[] parts = patientName.split("\\^");
        String family = parts[0].trim();
        String given = parts.length > 1 ? parts[1].trim() : "";
        if (family.isBlank()) {
            family = "Unknown";
        }
        if (given.isBlank()) {
            given = "Patient";
        }
        return new String[]{family, given};
    }

    private static String inferBodyPart(Attributes attrs, Path dicomPath) {
        String bodyPart = attrs.getString(Tag.BodyPartExamined);
        if (bodyPart != null && !bodyPart.isBlank()) {
            return bodyPart;
        }
        String seriesDesc = attrs.getString(Tag.SeriesDescription);
        if (seriesDesc != null && !seriesDesc.isBlank()) {
            return seriesDesc;
        }
        Path seriesDir = dicomPath.getParent();
        if (seriesDir != null) {
            String dir = seriesDir.getFileName().toString();
            if (dir.startsWith("SERIES") && dir.contains("_")) {
                return dir.substring(dir.indexOf('_') + 1).replace('_', ' ');
            }
        }
        return null;
    }
}
