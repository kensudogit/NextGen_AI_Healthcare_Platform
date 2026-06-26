package com.nghealth.platform.web;

import com.nghealth.platform.service.EmrService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/emr")
public class EmrController {

    private final EmrService emrService;

    public EmrController(EmrService emrService) {
        this.emrService = emrService;
    }

    @GetMapping("/patients")
    public Map<String, Object> listPatients() {
        return Map.of("patients", emrService.listPatients());
    }

    @GetMapping("/patients/{id}")
    public Map<String, Object> getPatient(@PathVariable Long id) {
        return emrService.getPatient(id);
    }

    public record PatientCreate(String mrn, String familyName, String givenName, String birthDate, String gender, String phone, String email, String allergies) {}

    @PostMapping("/patients")
    public Map<String, Object> createPatient(@RequestBody PatientCreate body) {
        return emrService.createPatient(
                body.mrn(), body.familyName(), body.givenName(),
                LocalDate.parse(body.birthDate()), body.gender(),
                body.phone(), body.email(), body.allergies());
    }
}
