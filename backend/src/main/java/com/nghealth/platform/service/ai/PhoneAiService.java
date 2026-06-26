package com.nghealth.platform.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nghealth.platform.config.AppProperties;
import com.nghealth.platform.domain.Appointment;
import com.nghealth.platform.domain.PhoneCall;
import com.nghealth.platform.domain.PhoneTurn;
import com.nghealth.platform.repository.AppointmentRepository;
import com.nghealth.platform.repository.PhoneCallRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PhoneAiService {

    private static final List<String> EMERGENCY = List.of("胸が痛い", "意識", "呼吸", "救急", "119", "血が", "倒れた");
    private static final List<String> TRANSFER = List.of("苦情", "弁護士", "診断", "処方", "薬を", "人と話");

    private final PhoneCallRepository callRepository;
    private final AppointmentRepository appointmentRepository;
    private final OpenAiService openAiService;
    private final AppProperties appProperties;
    private final ObjectMapper mapper = new ObjectMapper();

    public PhoneAiService(
            PhoneCallRepository callRepository,
            AppointmentRepository appointmentRepository,
            OpenAiService openAiService,
            AppProperties appProperties) {
        this.callRepository = callRepository;
        this.appointmentRepository = appointmentRepository;
        this.openAiService = openAiService;
        this.appProperties = appProperties;
    }

    public Map<String, Object> startCall(String callerNumber) {
        PhoneCall call = new PhoneCall();
        call.setCallSid("call-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        call.setCallerNumber(callerNumber);
        call.setStatus("active");
        String greeting = appProperties.hospitalName() + "です。AI受付がご案内します。予約・診療時間・お問い合わせをお伺いします。";
        PhoneTurn turn = new PhoneTurn();
        turn.setCall(call);
        turn.setSeq(0);
        turn.setRole("assistant");
        turn.setText(greeting);
        call.getTurns().add(turn);
        callRepository.save(call);
        return Map.of("call_sid", call.getCallSid(), "call_id", call.getId(), "reply", greeting, "action", "continue");
    }

    public Map<String, Object> processUtterance(String callSid, String text) {
        PhoneCall call = callRepository.findByCallSid(callSid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call not found"));
        if (!"active".equals(call.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Call ended");
        }

        int seq = call.getTurns().size();
        PhoneTurn userTurn = new PhoneTurn();
        userTurn.setCall(call);
        userTurn.setSeq(seq);
        userTurn.setRole("user");
        userTurn.setText(text);
        call.getTurns().add(userTurn);

        Map<String, Object> data = EMERGENCY.stream().anyMatch(text::contains)
                ? ruleReply(text)
                : aiReply(text, call.getTurns());

        String reply = String.valueOf(data.getOrDefault("reply", "かしこまりました。"));
        String action = String.valueOf(data.getOrDefault("action", "continue"));
        call.setIntent(String.valueOf(data.getOrDefault("intent", call.getIntent())));

        Long appointmentId = null;
        if ("book_appointment".equals(action) || "appointment".equals(data.get("intent"))) {
            Appointment apt = tryBook(data, text);
            if (apt != null) {
                appointmentId = apt.getId();
                call.setAppointmentId(apt.getId());
                reply = apt.getPatientName() + "様、" + apt.getDepartment() + "の予約を承りました。";
            }
        }
        if ("transfer".equals(action)) {
            call.setTransferred(true);
            call.setStatus("transferred");
        }

        PhoneTurn assistantTurn = new PhoneTurn();
        assistantTurn.setCall(call);
        assistantTurn.setSeq(seq + 1);
        assistantTurn.setRole("assistant");
        assistantTurn.setText(reply);
        call.getTurns().add(assistantTurn);
        callRepository.save(call);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("call_sid", callSid);
        result.put("reply", reply);
        result.put("intent", call.getIntent());
        result.put("action", action);
        result.put("appointment_id", appointmentId);
        result.put("transferred", call.isTransferred());
        return result;
    }

    public Map<String, Object> endCall(String callSid) {
        PhoneCall call = callRepository.findByCallSid(callSid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call not found"));
        String transcript = call.getTurns().stream()
                .map(t -> t.getRole() + ": " + t.getText())
                .reduce((a, b) -> a + "\n" + b).orElse("");
        String summary = "意図: " + (call.getIntent() != null ? call.getIntent() : "不明") + " / ターン数: " + call.getTurns().size();
        String aiSummary = openAiService.chatText("通話要約を2-3文の日本語で。", transcript);
        if (aiSummary != null) {
            summary = aiSummary;
        }
        call.setSummary(summary);
        call.setStatus("completed");
        call.setEndedAt(Instant.now());
        callRepository.save(call);
        return Map.of("call_sid", callSid, "summary", summary, "status", "completed");
    }

    private Map<String, Object> ruleReply(String text) {
        if (EMERGENCY.stream().anyMatch(text::contains)) {
            return Map.of("reply", "緊急の症状の可能性があります。ただちに119番または救急外来へ。オペレーターへおつなぎします。", "intent", "emergency", "action", "transfer");
        }
        if (TRANSFER.stream().anyMatch(text::contains)) {
            return Map.of("reply", "担当者におつなぎします。少々お待ちください。", "intent", "transfer", "action", "transfer");
        }
        if (text.contains("予約")) {
            return Map.of("reply", "予約ですね。お名前とご希望の診療科、日時をお知らせください。", "intent", "appointment", "action", "continue");
        }
        if (text.contains("時間") || text.contains("診療") || text.contains("休診")) {
            return Map.of("reply", appProperties.hospitalName() + "の診療時間は平日9:00-17:00、土曜9:00-12:00です。", "intent", "hours", "action", "continue");
        }
        return Map.of("reply", appProperties.hospitalName() + "です。予約・診療時間などお伺いします。", "intent", "inquiry", "action", "continue");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> aiReply(String text, List<PhoneTurn> turns) {
        String system = """
                あなたは病院AI電話受付です。JSONのみ返答:
                {"reply":"短い日本語","intent":"appointment|inquiry|hours|emergency|transfer|other","action":"continue|book_appointment|transfer|end","appointment":{"name":"","department":"","datetime_hint":""}}
                """;
        List<Map<String, String>> history = new ArrayList<>();
        for (PhoneTurn t : turns) {
            if ("user".equals(t.getRole()) || "assistant".equals(t.getRole())) {
                history.add(Map.of("role", t.getRole(), "content", t.getText()));
            }
        }
        history.add(Map.of("role", "user", "content", text));
        String json = openAiService.chatJson(system, history);
        if (json == null) {
            return ruleReply(text);
        }
        try {
            JsonNode node = mapper.readTree(json);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("reply", node.path("reply").asText("かしこまりました。"));
            result.put("intent", node.path("intent").asText("inquiry"));
            result.put("action", node.path("action").asText("continue"));
            if (node.has("appointment")) {
                result.put("appointment", mapper.convertValue(node.get("appointment"), Map.class));
            }
            return result;
        } catch (Exception e) {
            return ruleReply(text);
        }
    }

    @SuppressWarnings("unchecked")
    private Appointment tryBook(Map<String, Object> data, String text) {
        String name = null;
        String dept = "内科";
        if (data.get("appointment") instanceof Map<?, ?> ap) {
            name = (String) ap.get("name");
            if (ap.get("department") != null) {
                dept = (String) ap.get("department");
            }
        }
        if (name == null || name.isBlank()) {
            if (text.matches(".*[\\u4e00-\\u9fff]{2,}.*")) {
                name = text.replaceAll(".*([\\u4e00-\\u9fff]{2,8}).*", "$1");
            } else {
                return null;
            }
        }
        Appointment apt = new Appointment();
        apt.setPatientName(name.trim());
        apt.setDepartment(dept);
        apt.setPurpose("電話予約");
        apt.setScheduledAt(Instant.now().plus(3, ChronoUnit.DAYS));
        apt.setSource("phone_ai");
        apt.setStatus("booked");
        return appointmentRepository.save(apt);
    }
}
