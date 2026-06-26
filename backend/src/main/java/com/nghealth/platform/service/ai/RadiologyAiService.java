package com.nghealth.platform.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nghealth.platform.domain.RadiologyReport;
import com.nghealth.platform.repository.RadiologyReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RadiologyAiService {

    private final RadiologyReportRepository reportRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper mapper = new ObjectMapper();

    public RadiologyAiService(RadiologyReportRepository reportRepository, OpenAiService openAiService) {
        this.reportRepository = reportRepository;
        this.openAiService = openAiService;
    }

    public Map<String, Object> summarizeAndSave(Long reportId) {
        RadiologyReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        Map<String, Object> summary = summarize(report.getReportText());
        report.setAiSummary((String) summary.get("summary"));
        report.setAiFindings((String) summary.get("findings"));
        report.setUrgency((String) summary.get("urgency"));
        reportRepository.save(report);
        Map<String, Object> result = new LinkedHashMap<>(summary);
        result.put("id", report.getId());
        return result;
    }

    public Map<String, Object> summarize(String reportText) {
        String system = """
                放射線レポートを要約。JSONのみ:
                {"summary":"2-3文の日本語要約","findings":"主要所見を箇条書き風テキスト","urgency":"low|medium|high"}
                """;
        String json = openAiService.chatJson(system, List.of(Map.of("role", "user", "content", reportText)));
        if (json != null) {
            try {
                JsonNode node = mapper.readTree(json);
                return Map.of(
                        "summary", node.path("summary").asText(""),
                        "findings", node.path("findings").asText(""),
                        "urgency", node.path("urgency").asText("low"));
            } catch (Exception ignored) {
            }
        }
        return ruleSummary(reportText);
    }

    private Map<String, Object> ruleSummary(String text) {
        String urgency = text.contains("緊急") || text.contains("要フォロー") ? "high" : "low";
        String summary = text.lines().filter(l -> l.contains("印象") || l.contains("【印象】"))
                .findFirst().orElse(text.substring(0, Math.min(200, text.length())));
        return Map.of("summary", summary, "findings", "ルールベース抽出（OpenAI APIキー未設定）", "urgency", urgency);
    }
}
