package com.nghealth.platform.web;

import com.nghealth.platform.domain.PhoneCall;
import com.nghealth.platform.repository.PhoneCallRepository;
import com.nghealth.platform.service.ai.PhoneAiService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/phone")
public class PhoneController {

    private final PhoneAiService phoneAiService;
    private final PhoneCallRepository callRepository;

    public PhoneController(PhoneAiService phoneAiService, PhoneCallRepository callRepository) {
        this.phoneAiService = phoneAiService;
        this.callRepository = callRepository;
    }

    public record StartRequest(String callerNumber) {}
    public record UtteranceRequest(String callSid, String text) {}
    public record EndRequest(String callSid) {}

    @PostMapping("/start")
    public Map<String, Object> start(@RequestBody StartRequest body) {
        return phoneAiService.startCall(body.callerNumber());
    }

    @PostMapping("/utterance")
    public Map<String, Object> utterance(@RequestBody UtteranceRequest body) {
        return phoneAiService.processUtterance(body.callSid(), body.text());
    }

    @PostMapping("/end")
    public Map<String, Object> end(@RequestBody EndRequest body) {
        return phoneAiService.endCall(body.callSid());
    }

    @GetMapping("/calls")
    public Map<String, Object> listCalls() {
        List<Map<String, Object>> calls = callRepository.findTop30ByOrderByStartedAtDesc().stream().map(this::callMap).toList();
        return Map.of("calls", calls);
    }

    @GetMapping("/calls/{id}/turns")
    public Map<String, Object> turns(@PathVariable Long id) {
        PhoneCall call = callRepository.findById(id).orElseThrow();
        List<Map<String, Object>> turns = call.getTurns().stream()
                .map(t -> Map.<String, Object>of("seq", t.getSeq(), "role", t.getRole(), "text", t.getText(), "at", t.getCreatedAt().toString()))
                .toList();
        return Map.of("turns", turns);
    }

    private Map<String, Object> callMap(PhoneCall c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("call_sid", c.getCallSid());
        m.put("caller_number", c.getCallerNumber());
        m.put("intent", c.getIntent());
        m.put("status", c.getStatus());
        m.put("summary", c.getSummary());
        m.put("transferred", c.isTransferred());
        m.put("appointment_id", c.getAppointmentId());
        m.put("started_at", c.getStartedAt().toString());
        m.put("ended_at", c.getEndedAt() != null ? c.getEndedAt().toString() : null);
        return m;
    }
}
