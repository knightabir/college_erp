package com.college.erp.module.examination.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.dto.SubjectOfferingDto;
import com.college.erp.module.academic.service.SubjectService;
import com.college.erp.module.attendance.dto.AttendanceSummaryDto;
import com.college.erp.module.attendance.service.AttendanceService;
import com.college.erp.module.examination.domain.Exam;
import com.college.erp.module.examination.domain.HallTicket;
import com.college.erp.module.examination.domain.HallTicketRepository;
import com.college.erp.module.examination.dto.HallTicketDto;
import com.college.erp.module.examination.dto.HallTicketGenerationResultDto;
import com.college.erp.module.examination.dto.IneligibleStudentDto;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [EXAM-002], 05-api-documentation.md §6. Eligibility is checked per subject offering
 * in the exam's batch+term (attendance is tracked per offering, not as one batch-wide number) —
 * a student failing the threshold in ANY offering is marked ineligible, citing the first one found.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HallTicketService {

    private static final String MODULE = "EXAMINATION";

    private final HallTicketRepository hallTicketRepository;
    private final ExamService examService;
    private final StudentService studentService;
    private final SubjectService subjectService;
    private final AttendanceService attendanceService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public HallTicketGenerationResultDto generate(UUID examId) {
        Exam exam = examService.getEntity(examId);
        List<Student> students = studentService.listAllStudentEntitiesByBatch(exam.getBatch().getId());
        List<SubjectOfferingDto> offerings =
                subjectService.listOfferings(exam.getBatch().getId(), exam.getTerm().getId());

        List<IneligibleStudentDto> ineligibleStudents = new ArrayList<>();
        int generated = 0;
        int sequence = 0;

        for (Student student : students) {
            sequence++;
            String ineligibilityReason = findIneligibilityReason(student.getId(), offerings);
            boolean eligible = ineligibilityReason == null;

            HallTicket hallTicket = HallTicket.builder()
                    .exam(exam)
                    .student(student)
                    .hallTicketNumber(generateHallTicketNumber(exam, sequence))
                    .eligible(eligible)
                    .ineligibilityReason(ineligibilityReason)
                    .issuedAt(eligible ? Instant.now() : null)
                    .build();
            hallTicketRepository.save(hallTicket);

            if (eligible) {
                generated++;
            } else {
                ineligibleStudents.add(IneligibleStudentDto.builder()
                        .studentId(student.getId())
                        .enrollmentNumber(student.getEnrollmentNumber())
                        .reason(ineligibilityReason)
                        .build());
            }
        }

        auditLogWriter.write(AuditAction.CREATE, MODULE, "HallTicket", examId.toString(),
                "Generated hall tickets for exam " + exam.getName() + ": "
                        + generated + " eligible, " + ineligibleStudents.size() + " ineligible");
        log.info("Hall tickets generated for exam [{}]: {} eligible, {} ineligible",
                exam.getName(), generated, ineligibleStudents.size());

        return HallTicketGenerationResultDto.builder()
                .generated(generated)
                .ineligible(ineligibleStudents.size())
                .ineligibleStudents(ineligibleStudents)
                .build();
    }

    @Transactional(readOnly = true)
    public HallTicketDto getForStudent(UUID examId, UUID studentId) {
        return HallTicketDto.from(hallTicketRepository.findByExamIdAndStudentIdAndDeletedFalse(examId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("HallTicket for student", studentId)));
    }

    private String findIneligibilityReason(UUID studentId, List<SubjectOfferingDto> offerings) {
        for (SubjectOfferingDto offering : offerings) {
            AttendanceSummaryDto summary = attendanceService.getSummary(studentId, offering.getId());
            if (!summary.isEligibleForExam()) {
                return "Attendance below required threshold in " + offering.getSubjectName()
                        + " (" + summary.getAttendancePercentage() + "%)";
            }
        }
        return null;
    }

    private String generateHallTicketNumber(Exam exam, int sequence) {
        String examCode = exam.getId().toString().substring(0, 8).toUpperCase();
        return "HT" + examCode + String.format("%04d", sequence);
    }
}
