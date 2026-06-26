package com.nghealth.platform.repository;

import com.nghealth.platform.domain.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {
    List<ClinicalNote> findByEncounterIdIn(List<Long> encounterIds);
}
