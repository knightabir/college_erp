package com.college.erp.module.student.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.domain.StudentRepository;
import com.college.erp.module.student.domain.StudentStatus;
import com.college.erp.module.student.dto.CreateStudentRequest;
import com.college.erp.module.student.dto.StudentDto;
import com.college.erp.module.student.dto.StudentSummaryDto;
import com.college.erp.module.student.dto.UpdateStudentRequest;
import com.college.erp.tenant.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private static final String MODULE = "STUDENT";

    private final StudentRepository studentRepository;
    private final EnrollmentService enrollmentService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public StudentDto create(CreateStudentRequest request) {
        String enrollmentNumber = request.getEnrollmentNumber().toUpperCase();
        if (studentRepository.existsByEnrollmentNumberAndDeletedFalse(enrollmentNumber)) {
            throw new DuplicateResourceException(
                    "Student with enrollment number: " + enrollmentNumber);
        }

        Student student = Student.builder()
                .userId(request.getUserId())
                .enrollmentNumber(enrollmentNumber)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .personalEmail(request.getPersonalEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .aadharNumber(request.getAadharNumber())
                .category(request.getCategory())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .admissionDate(LocalDate.now())
                .build();
        student.setTenantId(TenantContext.getCurrentTenantId());

        Student saved = studentRepository.save(student);

        // Same transaction as the Student insert — a student never exists without an enrollment.
        enrollmentService.createInitialEnrollment(saved, request.getBatchId(), request.getSectionId());

        auditLogWriter.write(AuditAction.CREATE, MODULE, "Student", saved.getId().toString(),
                "Enrolled: " + saved.getEnrollmentNumber() + " " + saved.getFullName());
        log.info("Student enrolled: [{}] {}", saved.getEnrollmentNumber(), saved.getFullName());

        return StudentDto.from(saved);
    }

    @Transactional
    public StudentDto update(UUID id, UpdateStudentRequest request) {
        Student student = getEntity(id);

        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setPhone(request.getPhone());
        student.setBloodGroup(request.getBloodGroup());
        student.setAddress(request.getAddress());
        student.setCity(request.getCity());
        student.setState(request.getState());
        student.setPincode(request.getPincode());

        Student saved = studentRepository.save(student);
        auditLogWriter.write(AuditAction.UPDATE, MODULE, "Student", saved.getId().toString(),
                "Updated: " + saved.getEnrollmentNumber());

        return StudentDto.from(saved);
    }

    @Transactional
    public void graduate(UUID id) {
        Student student = getEntity(id);
        student.setStatus(StudentStatus.GRADUATED);
        student.setGraduationDate(LocalDate.now());
        studentRepository.save(student);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "Student", id.toString(),
                "Graduated: " + student.getEnrollmentNumber());
        log.info("Student graduated: [{}] {}", student.getEnrollmentNumber(), student.getFullName());
    }

    @Transactional(readOnly = true)
    public StudentDto getById(UUID id) {
        return StudentDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public StudentDto getByUserId(UUID userId) {
        return StudentDto.from(studentRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile for user", userId)));
    }

    @Transactional(readOnly = true)
    public StudentDto getByEnrollmentNumber(String enrollmentNumber) {
        return StudentDto.from(studentRepository.findByEnrollmentNumberAndDeletedFalse(enrollmentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student", enrollmentNumber)));
    }

    @Transactional(readOnly = true)
    public Page<StudentSummaryDto> listBySection(UUID sectionId, Pageable pageable) {
        return studentRepository.findBySectionId(sectionId, pageable).map(StudentSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public Page<StudentSummaryDto> listByBatch(UUID batchId, Pageable pageable) {
        return studentRepository.findByBatchId(batchId, pageable).map(StudentSummaryDto::from);
    }

    /** Unpaged entity list — e.g. module.examination generating hall tickets for a whole batch
     *  roster in one pass. See BatchService.getBatchEntity javadoc for the entity-accessor pattern. */
    @Transactional(readOnly = true)
    public List<Student> listAllStudentEntitiesByBatch(UUID batchId) {
        return studentRepository.findAllByBatchId(batchId);
    }

    /** For other modules that need the managed entity — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public Student getStudentEntity(UUID id) {
        return getEntity(id);
    }

    /** Resolves "the student profile belonging to this logged-in user" — used by self-service
     *  endpoints (condonation requests, etc.) where the student acts for themselves. */
    @Transactional(readOnly = true)
    public Student getStudentEntityByUserId(UUID userId) {
        return studentRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile for user", userId));
    }

    /** Batch variant — e.g. module.attendance validates a whole session's worth of student ids
     *  in one query rather than one round-trip per student. Throws if any id doesn't resolve to
     *  a non-deleted student, naming the first missing one. */
    @Transactional(readOnly = true)
    public List<Student> getStudentEntities(Collection<UUID> ids) {
        List<Student> students = studentRepository.findAllById(ids).stream()
                .filter(s -> !s.isDeleted())
                .toList();
        if (students.size() != ids.size()) {
            Set<UUID> found = students.stream().map(Student::getId).collect(Collectors.toSet());
            UUID missing = ids.stream().filter(id -> !found.contains(id)).findFirst().orElseThrow();
            throw new ResourceNotFoundException("Student", missing);
        }
        return students;
    }

    private Student getEntity(UUID id) {
        return studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }
}
