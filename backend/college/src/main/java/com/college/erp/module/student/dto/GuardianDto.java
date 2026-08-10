package com.college.erp.module.student.dto;

import java.util.UUID;

import com.college.erp.module.student.domain.Guardian;
import com.college.erp.module.student.domain.GuardianRelationship;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuardianDto {

    private UUID id;
    private UUID studentId;
    /** Nullable — most guardians don't have their own login (04-database-schema.md §6.2's
     *  guardians.user_id is optional); populated only when a guardian has been given a PARENT
     *  account. Used by AttendanceShortfallScanJob to resolve who to notify. */
    private UUID userId;
    private String name;
    private GuardianRelationship relationship;
    private String phone;
    private String email;
    private String occupation;
    private Long annualIncome;
    private boolean primary;

    public static GuardianDto from(Guardian guardian) {
        return GuardianDto.builder()
                .id(guardian.getId())
                .studentId(guardian.getStudent().getId())
                .userId(guardian.getUserId())
                .name(guardian.getName())
                .relationship(guardian.getRelationship())
                .phone(guardian.getPhone())
                .email(guardian.getEmail())
                .occupation(guardian.getOccupation())
                .annualIncome(guardian.getAnnualIncome())
                .primary(guardian.isPrimary())
                .build();
    }
}
