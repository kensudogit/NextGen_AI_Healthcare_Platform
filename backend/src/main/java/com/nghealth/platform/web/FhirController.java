package com.nghealth.platform.web;

import com.nghealth.platform.service.fhir.FhirService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fhir/R4")
public class FhirController {

    private final FhirService fhirService;

    public FhirController(FhirService fhirService) {
        this.fhirService = fhirService;
    }

    @GetMapping(value = "/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> metadata() {
        return ResponseEntity.ok(fhirService.metadataJson());
    }

    @GetMapping(value = "/Patient", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchPatients(@RequestParam(required = false) String name) {
        return ResponseEntity.ok(fhirService.searchPatients(name));
    }

    @GetMapping(value = "/Patient/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getPatient(@PathVariable Long id) {
        return ResponseEntity.ok(fhirService.getPatient(id));
    }

    @GetMapping(value = "/Appointment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchAppointments() {
        return ResponseEntity.ok(fhirService.searchAppointments());
    }

    @GetMapping(value = "/Appointment/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(fhirService.getAppointment(id));
    }

    @GetMapping(value = "/ImagingStudy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchImagingStudies() {
        return ResponseEntity.ok(fhirService.searchImagingStudies());
    }

    @GetMapping(value = "/ImagingStudy/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getImagingStudy(@PathVariable Long id) {
        return ResponseEntity.ok(fhirService.getImagingStudy(id));
    }

    @GetMapping(value = "/DiagnosticReport", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchDiagnosticReports() {
        return ResponseEntity.ok(fhirService.searchDiagnosticReports());
    }

    @GetMapping(value = "/DiagnosticReport/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDiagnosticReport(@PathVariable Long id) {
        return ResponseEntity.ok(fhirService.getDiagnosticReport(id));
    }
}
