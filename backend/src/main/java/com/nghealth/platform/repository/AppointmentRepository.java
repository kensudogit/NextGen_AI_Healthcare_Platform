package com.nghealth.platform.repository;

import com.nghealth.platform.domain.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByScheduledAtAfterOrderByScheduledAtAsc(Instant after);
    long countByScheduledAtBetween(Instant start, Instant end);
    long countByScheduledAtAfterAndStatus(Instant after, String status);
}
