package com.college.erp.module.academic.dto;

import java.util.UUID;

import com.college.erp.module.academic.domain.FacultySubjectMapping;

import lombok.Builder;
import lombok.Getter;

/**
 * One "who teaches what to which section this term" row. There is no day/time-slot data in the
 * documented schema (see FacultySubjectMapping javadoc) — this is a subject/faculty/section
 * assignment list, not a scheduled-slot grid.
 */
@Getter
@Builder
public class TimetableEntryDto {

    private UUID mappingId;
    private UUID sectionId;
    private String sectionName;
    private UUID offeringId;
    private UUID subjectId;
    private String subjectName;
    private UUID termId;
    private UUID facultyUserId;
    private boolean primary;

    public static TimetableEntryDto from(FacultySubjectMapping mapping) {
        return TimetableEntryDto.builder()
                .mappingId(mapping.getId())
                .sectionId(mapping.getSection().getId())
                .sectionName(mapping.getSection().getName())
                .offeringId(mapping.getOffering().getId())
                .subjectId(mapping.getOffering().getSubject().getId())
                .subjectName(mapping.getOffering().getSubject().getName())
                .termId(mapping.getOffering().getTerm().getId())
                .facultyUserId(mapping.getFacultyUserId())
                .primary(mapping.isPrimary())
                .build();
    }
}
