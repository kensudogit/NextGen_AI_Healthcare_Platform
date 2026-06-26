package com.nghealth.platform.repository;

import com.nghealth.platform.domain.PhoneCall;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PhoneCallRepository extends JpaRepository<PhoneCall, Long> {
    Optional<PhoneCall> findByCallSid(String callSid);
    List<PhoneCall> findTop30ByOrderByStartedAtDesc();
    long countByStartedAtAfter(Instant after);
    long countByStatus(String status);
}
