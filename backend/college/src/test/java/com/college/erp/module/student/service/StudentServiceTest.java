package com.college.erp.module.student.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.student.domain.Gender;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.domain.StudentRepository;
import com.college.erp.module.student.domain.StudentStatus;
import com.college.erp.module.student.dto.CreateStudentRequest;
import com.college.erp.module.student.dto.StudentDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private EnrollmentService enrollmentService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private StudentService studentService;

    private CreateStudentRequest buildRequest() {
        CreateStudentRequest request = new CreateStudentRequest();
        request.setEnrollmentNumber("2024cse001");
        request.setFirstName("Arjun");
        request.setLastName("Kumar");
        request.setPersonalEmail("arjun@gmail.com");
        request.setDateOfBirth(LocalDate.of(2004, 3, 15));
        request.setGender(Gender.MALE);
        request.setBatchId(UUID.randomUUID());
        request.setSectionId(UUID.randomUUID());
        return request;
    }

    @Test
    @DisplayName("create() — uppercases enrollment number, saves student, creates initial enrollment")
    void create_success_createsStudentAndEnrollment() {
        // ARRANGE
        studentService = new StudentService(studentRepository, enrollmentService, auditLogWriter);
        CreateStudentRequest request = buildRequest();

        when(studentRepository.existsByEnrollmentNumberAndDeletedFalse("2024CSE001")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        // ACT
        StudentDto result = studentService.create(request);

        // ASSERT
        assertThat(result.getEnrollmentNumber()).isEqualTo("2024CSE001");
        assertThat(result.getFullName()).isEqualTo("Arjun Kumar");
        verify(enrollmentService).createInitialEnrollment(
                any(Student.class), eq(request.getBatchId()), eq(request.getSectionId()));
    }

    @Test
    @DisplayName("create() — duplicate enrollment number is rejected")
    void create_duplicateEnrollmentNumber_throws() {
        // ARRANGE
        studentService = new StudentService(studentRepository, enrollmentService, auditLogWriter);
        when(studentRepository.existsByEnrollmentNumberAndDeletedFalse("2024CSE001")).thenReturn(true);

        // ACT / ASSERT
        assertThatThrownBy(() -> studentService.create(buildRequest()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("graduate() — sets status to GRADUATED and stamps today's graduation date")
    void graduate_setsStatusAndDate() {
        // ARRANGE
        studentService = new StudentService(studentRepository, enrollmentService, auditLogWriter);
        UUID id = UUID.randomUUID();
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun")
                .lastName("Kumar").status(StudentStatus.ACTIVE).build();
        student.setId(id);

        when(studentRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        studentService.graduate(id);

        // ASSERT
        assertThat(student.getStatus()).isEqualTo(StudentStatus.GRADUATED);
        assertThat(student.getGraduationDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("getById() — unknown id throws ResourceNotFoundException")
    void getById_notFound_throws() {
        // ARRANGE
        studentService = new StudentService(studentRepository, enrollmentService, auditLogWriter);
        UUID id = UUID.randomUUID();
        when(studentRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

        // ACT / ASSERT
        assertThatThrownBy(() -> studentService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
