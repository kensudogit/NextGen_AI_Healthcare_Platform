package com.nghealth.platform.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.nghealth.platform.config.AppProperties;
import com.nghealth.platform.domain.RadiologyReport;
import com.nghealth.platform.repository.AppointmentRepository;
import com.nghealth.platform.repository.ImagingStudyRepository;
import com.nghealth.platform.repository.PatientRepository;
import com.nghealth.platform.repository.RadiologyReportRepository;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class FhirService {

    private final FhirContext fhirContext = FhirContext.forR4();
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ImagingStudyRepository imagingStudyRepository;
    private final RadiologyReportRepository radiologyReportRepository;
    private final AppProperties appProperties;

    public FhirService(
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            ImagingStudyRepository imagingStudyRepository,
            RadiologyReportRepository radiologyReportRepository,
            AppProperties appProperties) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.imagingStudyRepository = imagingStudyRepository;
        this.radiologyReportRepository = radiologyReportRepository;
        this.appProperties = appProperties;
    }

    public String metadataJson() {
        CapabilityStatement cs = new CapabilityStatement();
        cs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        cs.setFhirVersion(Enumerations.FHIRVersion._4_0_1);
        cs.addFormat("json");
        CapabilityStatement.CapabilityStatementRestComponent rest = cs.addRest();
        rest.setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);
        for (String type : List.of("Patient", "Appointment", "ImagingStudy", "DiagnosticReport")) {
            CapabilityStatement.CapabilityStatementRestResourceComponent res = rest.addResource();
            res.setType(type);
            res.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.READ);
            res.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE);
        }
        return parser().encodeResourceToString(cs);
    }

    public String searchPatients(String name) {
        List<com.nghealth.platform.domain.Patient> rows = name == null || name.isBlank()
                ? patientRepository.findAll()
                : patientRepository.findByFamilyNameContainingOrGivenNameContaining(name, name);
        List<Resource> resources = new ArrayList<>();
        for (com.nghealth.platform.domain.Patient p : rows) {
            resources.add(toFhirPatient(p));
        }
        return bundle("Patient", resources);
    }

    public String getPatient(Long id) {
        com.nghealth.platform.domain.Patient p = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        return parser().encodeResourceToString(toFhirPatient(p));
    }

    public String searchAppointments() {
        List<Resource> resources = new ArrayList<>();
        for (com.nghealth.platform.domain.Appointment a : appointmentRepository.findAll()) {
            resources.add(toFhirAppointment(a));
        }
        return bundle("Appointment", resources);
    }

    public String getAppointment(Long id) {
        com.nghealth.platform.domain.Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        return parser().encodeResourceToString(toFhirAppointment(a));
    }

    public String searchImagingStudies() {
        List<Resource> resources = new ArrayList<>();
        for (com.nghealth.platform.domain.ImagingStudy s : imagingStudyRepository.findAll()) {
            resources.add(toFhirImagingStudy(s));
        }
        return bundle("ImagingStudy", resources);
    }

    public String getImagingStudy(Long id) {
        com.nghealth.platform.domain.ImagingStudy s = imagingStudyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ImagingStudy not found"));
        return parser().encodeResourceToString(toFhirImagingStudy(s));
    }

    public String searchDiagnosticReports() {
        List<Resource> resources = new ArrayList<>();
        for (RadiologyReport r : radiologyReportRepository.findAll()) {
            resources.add(toFhirDiagnosticReport(r));
        }
        return bundle("DiagnosticReport", resources);
    }

    public String getDiagnosticReport(Long id) {
        RadiologyReport r = radiologyReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DiagnosticReport not found"));
        return parser().encodeResourceToString(toFhirDiagnosticReport(r));
    }

    private String bundle(String type, List<Resource> resources) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(resources.size());
        for (Resource res : resources) {
            Bundle.BundleEntryComponent entry = bundle.addEntry();
            entry.setFullUrl(appProperties.fhir().baseUrl() + "/" + type + "/" + res.getIdElement().getIdPart());
            entry.setResource(res);
        }
        return parser().encodeResourceToString(bundle);
    }

    private Patient toFhirPatient(com.nghealth.platform.domain.Patient p) {
        Patient fp = new Patient();
        fp.setId(String.valueOf(p.getId()));
        fp.setMeta(new Meta().setLastUpdated(Date.from(Instant.now())));
        fp.addIdentifier().setSystem("http://nextgen.health/mrn").setValue(p.getMrn());
        fp.addName().setFamily(p.getFamilyName()).addGiven(p.getGivenName());
        if (p.getGender() != null) {
            fp.setGender(Enumerations.AdministrativeGender.fromCode(p.getGender()));
        }
        fp.setBirthDate(java.sql.Date.valueOf(p.getBirthDate()));
        if (p.getPhone() != null) {
            fp.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(p.getPhone());
        }
        if (p.getEmail() != null) {
            fp.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(p.getEmail());
        }
        return fp;
    }

    private Appointment toFhirAppointment(com.nghealth.platform.domain.Appointment a) {
        Appointment fa = new Appointment();
        fa.setId(String.valueOf(a.getId()));
        fa.setMeta(new Meta().setLastUpdated(Date.from(Instant.now())));
        fa.setStatus(Appointment.AppointmentStatus.BOOKED);
        fa.setDescription(a.getPurpose());
        fa.setStart(Date.from(a.getScheduledAt()));
        fa.addParticipant().setStatus(Appointment.ParticipationStatus.ACCEPTED)
                .getActor().setDisplay(a.getPatientName());
        fa.addServiceType().setText(a.getDepartment());
        return fa;
    }

    private ImagingStudy toFhirImagingStudy(com.nghealth.platform.domain.ImagingStudy s) {
        ImagingStudy fi = new ImagingStudy();
        fi.setId(String.valueOf(s.getId()));
        fi.setMeta(new Meta().setLastUpdated(Date.from(Instant.now())));
        fi.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        fi.addModality().setSystem("http://dicom.nema.org/resources/ontology/DCM").setCode(s.getModality());
        fi.getSubject().setReference("Patient/" + s.getPatientId());
        fi.setStarted(Date.from(s.getPerformedAt()));
        fi.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:" + s.getStudyUid());
        fi.setNumberOfSeries(1);
        fi.setNumberOfInstances(s.getInstanceCount());
        fi.setDescription(s.getDescription());
        return fi;
    }

    private DiagnosticReport toFhirDiagnosticReport(RadiologyReport r) {
        DiagnosticReport dr = new DiagnosticReport();
        dr.setId(String.valueOf(r.getId()));
        dr.setMeta(new Meta().setLastUpdated(Date.from(Instant.now())));
        dr.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        dr.addCategory().addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v2-0074")
                .setCode("RAD")
                .setDisplay("Radiology");
        dr.getCode().setText(r.getModality() + " Report");
        dr.getSubject().setReference("Patient/" + r.getPatientId());
        dr.setEffective(new DateTimeType(Date.from(r.getReportedAt())));
        dr.setConclusion(r.getAiSummary() != null ? r.getAiSummary() : r.getReportText().substring(0, Math.min(500, r.getReportText().length())));
        return dr;
    }

    private IParser parser() {
        return fhirContext.newJsonParser().setPrettyPrint(true);
    }
}
