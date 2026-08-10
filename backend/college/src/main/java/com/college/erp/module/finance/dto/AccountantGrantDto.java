package com.college.erp.module.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.finance.domain.AccountantPermissionGrant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountantGrantDto {

    private UUID id;
    private UUID granteeUserId;
    private UUID grantedByUserId;
    private String permissionKey;
    private UUID scopeDepartmentId;
    private UUID scopeBatchId;
    private String scopeFeeHeadCode;
    private BigDecimal amountThreshold;
    private Instant effectiveFrom;
    private Instant effectiveUntil;
    private Instant revokedAt;

    public static AccountantGrantDto from(AccountantPermissionGrant grant) {
        return AccountantGrantDto.builder()
                .id(grant.getId())
                .granteeUserId(grant.getGranteeUserId())
                .grantedByUserId(grant.getGrantedByUserId())
                .permissionKey(grant.getPermissionKey())
                .scopeDepartmentId(grant.getScopeDepartmentId())
                .scopeBatchId(grant.getScopeBatchId())
                .scopeFeeHeadCode(grant.getScopeFeeHeadCode())
                .amountThreshold(grant.getAmountThreshold())
                .effectiveFrom(grant.getEffectiveFrom())
                .effectiveUntil(grant.getEffectiveUntil())
                .revokedAt(grant.getRevokedAt())
                .build();
    }
}
