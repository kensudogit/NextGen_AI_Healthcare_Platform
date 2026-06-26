package com.nghealth.platform.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "phone_calls")
public class PhoneCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String callSid;

    @Column(length = 20)
    private String callerNumber;

    @Column(length = 40)
    private String intent;

    @Column(length = 20)
    private String status = "active";

    @Column(columnDefinition = "text")
    private String summary;

    private Long appointmentId;

    private boolean transferred = false;

    @Column(nullable = false)
    private Instant startedAt = Instant.now();

    private Instant endedAt;

    @OneToMany(mappedBy = "call", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<PhoneTurn> turns = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCallSid() { return callSid; }
    public void setCallSid(String callSid) { this.callSid = callSid; }
    public String getCallerNumber() { return callerNumber; }
    public void setCallerNumber(String callerNumber) { this.callerNumber = callerNumber; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public boolean isTransferred() { return transferred; }
    public void setTransferred(boolean transferred) { this.transferred = transferred; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public List<PhoneTurn> getTurns() { return turns; }
    public void setTurns(List<PhoneTurn> turns) { this.turns = turns; }
}
