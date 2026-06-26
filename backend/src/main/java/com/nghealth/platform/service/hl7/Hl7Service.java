package com.nghealth.platform.service.hl7;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.parser.PipeParser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class Hl7Service {

    private final PipeParser parser = new PipeParser();

    public Map<String, Object> parse(String message) throws HL7Exception {
        var parsed = parser.parse(message);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("messageType", parsed.getName());
        result.put("version", "2.5");
        if (parsed instanceof ADT_A01 adt) {
            result.put("event", "ADT^A01");
            result.put("patientId", adt.getPID().getPatientIdentifierList(0).getIDNumber().getValue());
            result.put("patientName", adt.getPID().getPatientName(0).getFamilyName().getSurname().getValue());
        } else if (parsed instanceof ORU_R01 oru) {
            result.put("event", "ORU^R01");
            result.put("patientId", oru.getPATIENT_RESULT().getPATIENT().getPID().getPatientIdentifierList(0).getIDNumber().getValue());
            result.put("observation", oru.getPATIENT_RESULT().getORDER_OBSERVATION().getOBSERVATION(0).getOBX().getObservationValue(0).getData().encode());
        } else {
            result.put("event", parsed.getName());
        }
        return result;
    }

    public String buildAdtA01(String mrn, String familyName, String givenName) throws HL7Exception, java.io.IOException {
        ADT_A01 adt = new ADT_A01();
        adt.initQuickstart("ADT", "A01", "P");
        adt.getPID().getPatientIdentifierList(0).getIDNumber().setValue(mrn);
        adt.getPID().getPatientName(0).getFamilyName().getSurname().setValue(familyName);
        adt.getPID().getPatientName(0).getGivenName().setValue(givenName);
        adt.getEVN().getRecordedDateTime().getTime().setValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
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
}
