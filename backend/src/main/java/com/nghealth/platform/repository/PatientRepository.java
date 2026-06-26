package com.nghealth.platform.repository;

import com.nghealth.platform.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByFamilyNameContainingOrGivenNameContaining(String family, String given);
    boolean existsByMrn(String mrn);
}
