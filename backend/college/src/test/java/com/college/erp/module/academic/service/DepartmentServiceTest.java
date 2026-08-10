package com.college.erp.module.academic.service;

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
import com.college.erp.module.academic.domain.Department;
import com.college.erp.module.academic.domain.DepartmentRepository;
import com.college.erp.module.academic.dto.CreateDepartmentRequest;
import com.college.erp.module.academic.dto.DepartmentDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private AuditLogWriter auditLogWriter;

    private DepartmentService departmentService;

    private CreateDepartmentRequest buildRequest() {
        CreateDepartmentRequest request = new CreateDepartmentRequest();
        request.setName("Computer Science");
        request.setCode("cse");
        request.setContactEmail("cse@college.edu");
        return request;
    }

    @Test
    @DisplayName("create() — uppercases the code and saves a new department")
    void create_success_uppercasesCode() {
        // ARRANGE
        departmentService = new DepartmentService(departmentRepository, auditLogWriter);
        CreateDepartmentRequest request = buildRequest();

        when(departmentRepository.existsByCodeAndDeletedFalse("CSE")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
            Department d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        // ACT
        DepartmentDto result = departmentService.create(request);

        // ASSERT
        assertThat(result.getCode()).isEqualTo("CSE");
        assertThat(result.getName()).isEqualTo("Computer Science");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("create() — duplicate code (case-insensitive) is rejected")
    void create_duplicateCode_throws() {
        // ARRANGE
        departmentService = new DepartmentService(departmentRepository, auditLogWriter);
        when(departmentRepository.existsByCodeAndDeletedFalse("CSE")).thenReturn(true);

        // ACT / ASSERT
        assertThatThrownBy(() -> departmentService.create(buildRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CSE");
    }

    @Test
    @DisplayName("getById() — unknown id throws ResourceNotFoundException")
    void getById_notFound_throwsResourceNotFoundException() {
        // ARRANGE
        departmentService = new DepartmentService(departmentRepository, auditLogWriter);
        UUID unknownId = UUID.randomUUID();
        when(departmentRepository.findByIdAndDeletedFalse(unknownId)).thenReturn(Optional.empty());

        // ACT / ASSERT
        assertThatThrownBy(() -> departmentService.getById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department");
    }

    @Test
    @DisplayName("deactivate() — flips active to false and saves")
    void deactivate_setsActiveFalse() {
        // ARRANGE
        departmentService = new DepartmentService(departmentRepository, auditLogWriter);
        UUID id = UUID.randomUUID();
        Department department = Department.builder().name("CSE Dept").code("CSE").active(true).build();
        department.setId(id);
        when(departmentRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(department));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        departmentService.deactivate(id);

        // ASSERT
        assertThat(department.isActive()).isFalse();
    }

    @Test
    @DisplayName("update() — changing to a code already used by another department is rejected")
    void update_toExistingCode_throws() {
        // ARRANGE
        departmentService = new DepartmentService(departmentRepository, auditLogWriter);
        UUID id = UUID.randomUUID();
        Department department = Department.builder().name("CSE Dept").code("CSE").active(true).build();
        department.setId(id);
        when(departmentRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(department));
        when(departmentRepository.existsByCodeAndDeletedFalse("ECE")).thenReturn(true);

        CreateDepartmentRequest request = buildRequest();
        request.setCode("ece");

        // ACT / ASSERT
        assertThatThrownBy(() -> departmentService.update(id, request))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
