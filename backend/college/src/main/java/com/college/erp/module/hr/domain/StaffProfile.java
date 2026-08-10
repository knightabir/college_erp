package com.college.erp.module.hr.domain;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.common.domain.BaseEntity;
import com.college.erp.module.academic.domain.Department;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 04-database-schema.md §13.2, 03-srs.md [HR-001]. `userId` is a plain UUID (not a managed
 * `User` entity relation) — matching the same pattern used everywhere else in this codebase for a
 * user reference that's peripheral to the owning module (LibraryCustodyLog.borrowerUserId,
 * Asset.assignedToUserId, MaintenanceTicket.reportedBy, ...); module.hr never needs to load the
 * full User entity, only compare/display the id.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "staff_profiles")
public class StaffProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "employee_number", nullable = false, unique = true, length = 20)
    private String employeeNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    private Designation designation;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", nullable = false, length = 20)
    private StaffType staffType;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "qualification", length = 200)
    private String qualification;

    @Column(name = "specialization", columnDefinition = "TEXT")
    private String specialization;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "ifsc_code", length = 11)
    private String ifscCode;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "pf_number", length = 20)
    private String pfNumber;
}
