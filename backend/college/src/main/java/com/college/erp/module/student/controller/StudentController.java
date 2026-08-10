package com.college.erp.module.student.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.common.dto.PageResponse;
import com.college.erp.module.student.dto.CreateGuardianRequest;
import com.college.erp.module.student.dto.CreateStudentRequest;
import com.college.erp.module.student.dto.GuardianDto;
import com.college.erp.module.student.dto.StudentDto;
import com.college.erp.module.student.dto.StudentSummaryDto;
import com.college.erp.module.student.dto.UpdateStudentRequest;
import com.college.erp.module.student.service.GuardianService;
import com.college.erp.module.student.service.StudentService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §4. Guardian endpoints are nested here (no dedicated
 *  GuardianController in 08-system-architecture.md's file tree, and no guardian endpoints in the
 *  API doc's §4 either — same "follow §1 conventions by analogy" situation as academic sections). */
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final GuardianService guardianService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD')")
    public ResponseEntity<ApiResponse<StudentDto>> create(@Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student enrolled", studentService.create(request)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentDto>> getOwnProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getByUserId(principal.getId())));
    }

    @GetMapping("/enrollment/{number}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','FACULTY','HEAD_ACCOUNTANT','ACCOUNTANT',"
            + "'CHIEF_LIBRARIAN','LIBRARY_ASSISTANT')")
    public ResponseEntity<ApiResponse<StudentDto>> getByEnrollmentNumber(@PathVariable String number) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getByEnrollmentNumber(number)));
    }

    @GetMapping("/by-section/{sectionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','FACULTY','EXAM_CELL')")
    public ResponseEntity<ApiResponse<PageResponse<StudentSummaryDto>>> listBySection(
            @PathVariable UUID sectionId, Pageable pageable) {
        Page<StudentSummaryDto> page = studentService.listBySection(sectionId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/by-batch/{batchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','EXAM_CELL')")
    public ResponseEntity<ApiResponse<PageResponse<StudentSummaryDto>>> listByBatch(
            @PathVariable UUID batchId, Pageable pageable) {
        Page<StudentSummaryDto> page = studentService.listByBatch(batchId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','FACULTY') "
            + "or @studentSecurityService.isOwnProfile(#id, principal)")
    public ResponseEntity<ApiResponse<StudentDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD') "
            + "or @studentSecurityService.isOwnProfile(#id, principal)")
    public ResponseEntity<ApiResponse<StudentDto>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateStudentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(studentService.update(id, request)));
    }

    @PatchMapping("/{id}/graduate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> graduate(@PathVariable UUID id) {
        studentService.graduate(id);
        return ResponseEntity.ok(ApiResponse.success("Student marked as graduated", null));
    }

    @GetMapping("/{id}/guardians")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','FACULTY') "
            + "or @studentSecurityService.isOwnProfile(#id, principal)")
    public ResponseEntity<ApiResponse<List<GuardianDto>>> listGuardians(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(guardianService.listByStudent(id)));
    }

    @PostMapping("/{id}/guardians")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD')")
    public ResponseEntity<ApiResponse<GuardianDto>> addGuardian(
            @PathVariable UUID id, @Valid @RequestBody CreateGuardianRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Guardian added", guardianService.add(id, request)));
    }
}
