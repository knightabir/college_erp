package com.college.erp.module.admissions.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.module.admissions.domain.Application;
import com.college.erp.module.admissions.domain.ApplicationRepository;
import com.college.erp.module.admissions.domain.ApplicationStatus;
import com.college.erp.module.admissions.domain.SeatAllotment;
import com.college.erp.module.admissions.dto.ConvertToStudentRequest;
import com.college.erp.module.student.domain.Gender;
import com.college.erp.module.student.dto.CreateStudentRequest;
import com.college.erp.module.student.dto.StudentDto;
import com.college.erp.module.student.service.StudentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * INVENTED — see V015__create_admissions_tables.sql's header comment. 06-design-document.md's
 * cross-module dependency table names an "admissions.EnrollmentConversionService |
 * StudentEnrolledEvent | academic | Set up subject offerings" event flow, but no
 * ApplicationEventPublisher/@EventListener infrastructure exists anywhere else in this codebase
 * either (rule #5 in this file's own module-boundary-rules section describes the pattern, but
 * nothing has actually built it yet — e.g. library's own documented BookIssuedEvent isn't wired
 * up). Rather than invent that infrastructure here first, this calls
 * module.student.service.StudentService directly (an ordinary, sanctioned cross-module *service*
 * call) and does not publish an event. Revisit once the event bus pattern is established for real.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentConversionService {

    private static final String MODULE = "ADMISSIONS";

    private final ApplicationWorkflowService applicationWorkflowService;
    private final MeritProcessingService meritProcessingService;
    private final ApplicationRepository applicationRepository;
    private final StudentService studentService;
    private final AuditLogWriter auditLogWriter;

    /** Requires the application's seat allotment to be ACCEPTED first (see
     *  MeritProcessingService.getAcceptedSeatAllotmentEntity javadoc). Creates a real Student +
     *  initial Enrollment in the allotted batch, then marks the application ADMITTED. */
    @Transactional
    public StudentDto convert(UUID applicationId, ConvertToStudentRequest request, UUID convertedByUserId) {
        Application application = applicationWorkflowService.getApplicationEntity(applicationId);
        SeatAllotment allotment = meritProcessingService.getAcceptedSeatAllotmentEntity(applicationId);

        CreateStudentRequest studentRequest = new CreateStudentRequest();
        studentRequest.setUserId(request.getUserId());
        studentRequest.setEnrollmentNumber(request.getEnrollmentNumber());
        String[] nameParts = splitName(application.getApplicantName());
        studentRequest.setFirstName(nameParts[0]);
        studentRequest.setLastName(nameParts[1]);
        studentRequest.setPersonalEmail(application.getApplicantEmail());
        studentRequest.setPhone(application.getApplicantPhone());
        studentRequest.setDateOfBirth(application.getDateOfBirth());
        studentRequest.setGender(parseGender(application.getGender()));
        studentRequest.setBatchId(allotment.getBatch().getId());
        studentRequest.setSectionId(request.getSectionId());

        StudentDto student = studentService.create(studentRequest);

        application.transitionTo(ApplicationStatus.ADMITTED);
        applicationRepository.save(application);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "Application", applicationId.toString(),
                "Converted to student " + student.getEnrollmentNumber());
        log.info("Application [{}] converted to student [{}]",
                application.getApplicationNumber(), student.getEnrollmentNumber());

        return student;
    }

    private String[] splitName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length == 2 ? parts : new String[] {parts[0], parts[0]};
    }

    private Gender parseGender(String gender) {
        if (gender == null) {
            return null;
        }
        try {
            return Gender.valueOf(gender.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
