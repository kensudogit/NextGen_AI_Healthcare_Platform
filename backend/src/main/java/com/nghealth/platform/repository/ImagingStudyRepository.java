package com.nghealth.platform.repository;

import com.nghealth.platform.domain.ImagingStudy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ImagingStudyRepository extends JpaRepository<ImagingStudy, Long> {
    List<ImagingStudy> findByPatientIdOrderByPerformedAtDesc(Long patientId);
}
