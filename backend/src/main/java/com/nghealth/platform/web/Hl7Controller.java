package com.nghealth.platform.web;

import ca.uhn.hl7v2.HL7Exception;
import com.nghealth.platform.service.hl7.Hl7Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/hl7")
public class Hl7Controller {

    private final Hl7Service hl7Service;

    public Hl7Controller(Hl7Service hl7Service) {
        this.hl7Service = hl7Service;
    }

    public record Hl7Message(String message) {}
    public record Hl7BuildRequest(String mrn, String familyName, String givenName, String reportText, String type) {}

    @PostMapping("/parse")
    public Map<String, Object> parse(@RequestBody Hl7Message body) {
        try {
            return hl7Service.parse(body.message());
        } catch (HL7Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HL7 parse error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HL7 parse error: " + e.getMessage(), e);
        }
    }

    @PostMapping("/build")
    public Map<String, String> build(@RequestBody Hl7BuildRequest body) {
        try {
            String msg = "ORU".equalsIgnoreCase(body.type())
                    ? hl7Service.buildOruR01(body.mrn(), body.reportText())
                    : hl7Service.buildAdtA01(body.mrn(), body.familyName(), body.givenName());
            return Map.of("message", msg, "version", "2.5");
        } catch (HL7Exception | java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HL7 build error: " + e.getMessage());
        }
    }
}
