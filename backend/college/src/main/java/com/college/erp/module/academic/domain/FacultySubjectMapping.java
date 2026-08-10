package com.college.erp.module.academic.domain;

import java.util.UUID;

import com.college.erp.common.domain.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 04-database-schema.md §6.9 — "this faculty teaches this offering to this section". This is the
 * closest thing to a "timetable" the documented schema supports: it has no day/time-slot columns
 * at all, so TimetableService (see the service layer) exposes this as a subject/section/faculty
 * assignment list, not a scheduled-slot grid — see the class javadoc on ConflictDetectionService
 * for what "conflict" means without that data.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "faculty_subject_mappings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"offering_id", "section_id", "faculty_user_id"})
)
public class FacultySubjectMapping extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private SubjectOffering offering;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "faculty_user_id", nullable = false)
    private UUID facultyUserId;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private boolean primary = true;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
