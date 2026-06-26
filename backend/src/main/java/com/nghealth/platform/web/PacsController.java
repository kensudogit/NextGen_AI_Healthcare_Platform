package com.nghealth.platform.web;

import com.nghealth.platform.domain.ImagingStudy;
import com.nghealth.platform.service.pacs.DicomService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/pacs")
public class PacsController {

    private final DicomService dicomService;

    public PacsController(DicomService dicomService) {
        this.dicomService = dicomService;
    }

    @GetMapping("/studies")
    public Map<String, Object> listStudies(@RequestParam(required = false) Long patientId) {
        return Map.of("studies", dicomService.listStudies(patientId));
    }

    @PostMapping("/studies/upload")
    public Map<String, Object> upload(@RequestParam Long patientId, @RequestParam("file") MultipartFile file) throws IOException {
        return dicomService.ingest(file.getBytes(), patientId);
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
