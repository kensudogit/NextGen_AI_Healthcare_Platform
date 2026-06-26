package com.nghealth.platform.service;

import com.nghealth.platform.domain.*;
import com.nghealth.platform.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
public class EmrService {

    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final ClinicalNoteRepository noteRepository;

    public EmrService(
            PatientRepository patientRepository,
            EncounterRepository encounterRepository,
            ClinicalNoteRepository noteRepository) {
        this.patientRepository = patientRepository;
        this.encounterRepository = encounterRepository;
        this.noteRepository = noteRepository;
    }

    public List<Map<String, Object>> listPatients() {
        return patientRepository.findAll().stream().map(this::patientMap).toList();
    }

    public Map<String, Object> getPatient(Long id) {
        Patient p = patientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        List<Encounter> encounters = encounterRepository.findByPatientId(id);
        List<Long> encIds = encounters.stream().map(Encounter::getId).toList();
        List<ClinicalNote> notes = encIds.isEmpty() ? List.of() : noteRepository.findByEncounterIdIn(encIds);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patient", patientMap(p));
        result.put("encounters", encounters.stream().map(e -> Map.of(
                "id", e.getId(),
                "type", e.getEncounterType(),
                "chief_complaint", e.getChiefComplaint(),
                "diagnosis", e.getDiagnosis(),
                "provider", e.getProviderName(),
                "started_at", e.getStartedAt().toString())).toList());
        result.put("notes", notes.stream().map(n -> Map.of(
                "id", n.getId(),
                "encounter_id", n.getEncounterId(),
                "note_type", n.getNoteType(),
                "author", n.getAuthor(),
                "content", n.getContent(),
                "created_at", n.getCreatedAt().toString())).toList());
        return result;
    }

    public Map<String, Object> createPatient(String mrn, String familyName, String givenName, LocalDate birthDate, String gender, String phone, String email, String allergies) {
        Patient p = new Patient();
        p.setMrn(mrn);
        p.setFamilyName(familyName);
        p.setGivenName(givenName);
        p.setBirthDate(birthDate);
        p.setGender(gender);
        p.setPhone(phone);
        p.setEmail(email);
        p.setAllergies(allergies);
        return patientMap(patientRepository.save(p));
    }

    private Map<String, Object> patientMap(Patient p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("mrn", p.getMrn());
        m.put("name", p.fullName());
        m.put("family_name", p.getFamilyName());
        m.put("given_name", p.getGivenName());
        m.put("birth_date", p.getBirthDate().toString());
        m.put("gender", p.getGender());
        m.put("phone", p.getPhone());
        m.put("email", p.getEmail());
        m.put("allergies", p.getAllergies());
        m.put("fhir_url", "/fhir/R4/Patient/" + p.getId());
        return m;
    }
}
