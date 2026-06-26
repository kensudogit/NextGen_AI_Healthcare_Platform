package com.nghealth.platform.service.hl7;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class Hl7Service {

    private static final DateTimeFormatter HL7_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PipeParser parser = new PipeParser();

    public Map<String, Object> parse(String message) throws HL7Exception {
        if (message == null || message.isBlank()) {
            throw new HL7Exception("HL7 message is empty");
        }

        String normalized = prepareForParsing(message);
        Message parsed = parser.parse(normalized);
        Terser terser = new Terser(parsed);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("messageType", parsed.getName());
        result.put("version", parsed.getVersion() != null ? parsed.getVersion() : "2.5");

        String msgType = safeGet(terser, "/MSH-9-1");
        String trigger = safeGet(terser, "/MSH-9-2");
        if (msgType != null && trigger != null) {
            result.put("event", msgType + "^" + trigger);
        } else {
            String event = safeGet(terser, "/MSH-9");
            if (event != null && !event.isBlank()) {
                result.put("event", event);
            }
        }

        String patientId = firstNonBlank(
                safeGet(terser, "/.PID-3-1"),
                safeGet(terser, "/PATIENT_RESULT/PATIENT/PID-3-1"));
        if (patientId != null) {
            result.put("patientId", patientId);
        }

        String family = firstNonBlank(
                safeGet(terser, "/.PID-5-1"),
                safeGet(terser, "/PATIENT_RESULT/PATIENT/PID-5-1"));
        String given = firstNonBlank(
                safeGet(terser, "/.PID-5-2"),
                safeGet(terser, "/PATIENT_RESULT/PATIENT/PID-5-2"));
        if (family != null) {
            result.put("patientName", given != null ? family + "^" + given : family);
        }

        String observation = safeGet(terser, "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-5");
        if (observation != null && !observation.isBlank()) {
            result.put("observation", observation);
        }

        if (parsed instanceof ADT_A01) {
            result.put("structure", "ADT_A01");
        } else if (parsed instanceof ORU_R01) {
            result.put("structure", "ORU_R01");
        }

        return result;
    }

    public String buildAdtA01(String mrn, String familyName, String givenName) throws HL7Exception, java.io.IOException {
        ADT_A01 adt = new ADT_A01();
        adt.initQuickstart("ADT", "A01", "P");
        adt.getPID().getPatientIdentifierList(0).getIDNumber().setValue(mrn);
        adt.getPID().getPatientName(0).getFamilyName().getSurname().setValue(familyName);
        adt.getPID().getPatientName(0).getGivenName().setValue(givenName);
        adt.getEVN().getRecordedDateTime().getTime().setValue(LocalDateTime.now().format(HL7_TS));
        return parser.encode(adt);
    }

    public String buildOruR01(String mrn, String reportText) throws HL7Exception, java.io.IOException {
        ORU_R01 oru = new ORU_R01();
        oru.initQuickstart("ORU", "R01", "P");
        oru.getPATIENT_RESULT().getPATIENT().getPID().getPatientIdentifierList(0).getIDNumber().setValue(mrn);
        oru.getPATIENT_RESULT().getORDER_OBSERVATION().getOBSERVATION(0).getOBX().getObservationValue(0).parse(reportText);
        oru.getPATIENT_RESULT().getORDER_OBSERVATION().getOBSERVATION(0).getOBX().getValueType().setValue("TX");
        return parser.encode(oru);
    }

    private String prepareForParsing(String message) {
        String normalized = message.replace("\r\n", "\r").replace("\n", "\r").trim();
        String[] segments = normalized.split("\r");

        List<String> output = new ArrayList<>();
        if (segments.length == 0 || !segments[0].startsWith("MSH|")) {
            return normalized.endsWith("\r") ? normalized : normalized + "\r";
        }

        String msh = segments[0];
        if (!msh.endsWith("|2.5")) {
            msh = msh + "|2.5";
        }
        output.add(msh);

        boolean isAdt = msh.contains("ADT^");
        boolean hasEvn = false;
        for (int i = 1; i < segments.length; i++) {
            if (segments[i].startsWith("EVN|")) {
                hasEvn = true;
                break;
            }
        }

        if (isAdt && !hasEvn) {
            output.add("EVN||" + extractMshTimestamp(msh));
        }

        for (int i = 1; i < segments.length; i++) {
            if (!segments[i].isBlank()) {
                output.add(segments[i]);
            }
        }

        return String.join("\r", output) + "\r";
    }

    private String extractMshTimestamp(String msh) {
        String[] fields = msh.split("\\|", -1);
        if (fields.length > 7 && !fields[7].isBlank()) {
            return fields[7];
        }
        return LocalDateTime.now().format(HL7_TS);
    }

    private static String safeGet(Terser terser, String path) {
        try {
            return terser.get(path);
        } catch (HL7Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
