package com.nghealth.platform.repository;

import com.nghealth.platform.domain.RadiologyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RadiologyReportRepository extends JpaRepository<RadiologyReport, Long> {
    long countByAiSummaryIsNull();
}
