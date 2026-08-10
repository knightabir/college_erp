package com.college.erp.module.academic.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.common.dto.PageResponse;
import com.college.erp.module.academic.dto.CourseDto;
import com.college.erp.module.academic.dto.CreateCourseRequest;
import com.college.erp.module.academic.service.CourseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §3 "Courses". */
@RestController
@RequestMapping("/api/v1/academic/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<CourseDto>>> list(Pageable pageable) {
        Page<CourseDto> page = courseService.list(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CourseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(courseService.getById(id)));
    }

    @GetMapping("/by-department/{departmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CourseDto>>> listByDepartment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(ApiResponse.success(courseService.listByDepartment(departmentId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<CourseDto>> create(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Course created", courseService.create(request)));
    }
}
