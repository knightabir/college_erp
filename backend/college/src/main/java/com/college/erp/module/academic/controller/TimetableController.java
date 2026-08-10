package com.college.erp.module.academic.controller;

import java.util.List;
import java.util.UUID;

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
import com.college.erp.module.academic.dto.AssignFacultyRequest;
import com.college.erp.module.academic.dto.TimetableEntryDto;
import com.college.erp.module.academic.service.TimetableService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Not in 05-api-documentation.md's TOC — see TimetableEntryDto javadoc for what this covers
 *  given the documented schema has no scheduling/time-slot data. */
@RestController
@RequestMapping("/api/v1/academic/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @PostMapping("/assign-faculty")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD')")
    public ResponseEntity<ApiResponse<TimetableEntryDto>> assignFaculty(
            @Valid @RequestBody AssignFacultyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Faculty assigned", timetableService.assignFaculty(request)));
    }

    @GetMapping("/section/{sectionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TimetableEntryDto>>> forSection(@PathVariable UUID sectionId) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.getTimetableForSection(sectionId)));
    }

    @GetMapping("/faculty/{facultyUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TimetableEntryDto>>> forFaculty(@PathVariable UUID facultyUserId) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.getTimetableForFaculty(facultyUserId)));
    }
}
