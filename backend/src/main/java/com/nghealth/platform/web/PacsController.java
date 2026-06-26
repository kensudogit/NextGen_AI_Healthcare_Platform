package com.nghealth.platform.web;

import com.nghealth.platform.config.AppProperties;
import com.nghealth.platform.domain.ImagingStudy;
import com.nghealth.platform.service.pacs.DicomService;
import com.nghealth.platform.service.pacs.PacsExportImporter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/pacs")
public class PacsController {

    private final DicomService dicomService;
    private final PacsExportImporter exportImporter;
    private final AppProperties appProperties;

    public PacsController(
            DicomService dicomService,
            PacsExportImporter exportImporter,
            AppProperties appProperties) {
        this.dicomService = dicomService;
        this.exportImporter = exportImporter;
        this.appProperties = appProperties;
    }

    @GetMapping("/studies")
    public Map<String, Object> listStudies(@RequestParam(required = false) Long patientId) {
        return Map.of("studies", dicomService.listStudies(patientId));
    }

    @PostMapping("/studies/upload")
    public Map<String, Object> upload(@RequestParam Long patientId, @RequestParam("file") MultipartFile file) throws IOException {
        return dicomService.ingest(file.getBytes(), patientId);
    }

    /** PACS エクスポート形式フォルダ (PATIENT / STUDY / SERIES / IMG*.dcm) を取り込み */
    @PostMapping("/import/export-folder")
    public Map<String, Object> importExportFolder(@RequestParam(required = false) String path) throws IOException {
        String target = path;
        if (target == null || target.isBlank()) {
            target = appProperties.storage().pacsExportPath();
        }
        if (target == null || target.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "path is required");
        }
        return exportImporter.importFolder(Paths.get(target));
    }

    @GetMapping("/studies/{id}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable Long id) throws IOException {
        ImagingStudy study = dicomService.getStudy(id);
        byte[] bytes = dicomService.getPreviewBytes(study);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(bytes);
    }

    @GetMapping("/studies/{id}/dicom")
    public ResponseEntity<byte[]> dicom(@PathVariable Long id) throws IOException {
        ImagingStudy study = dicomService.getStudy(id);
        byte[] bytes = dicomService.getDicomBytes(study);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=study.dcm")
                .contentType(MediaType.parseMediaType("application/dicom"))
                .body(bytes);
    }
}
