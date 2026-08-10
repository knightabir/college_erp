package com.college.erp.module.asset.domain;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 04-database-schema.md §12.3, 03-srs.md [AST-002]. Unlike library's LibraryCustodyLog (partially
 * immutable — recordReturn()/extendDueDate() may still touch return fields), AssetCustodyLog has
 * no mutators at all: every field is fixed at construction, mirroring
 * trg_acl_no_update/trg_acl_no_delete which reject ANY change once a row exists.
 */
class AssetCustodyLogTest {

    @Test
    @DisplayName("builder — every field is populated from construction and nothing else can set them")
    void allFieldsSetAtConstruction() {
        // ARRANGE
        Asset asset = Asset.builder().assetTag("COM-000001").name("Projector").build();
        UUID fromDept = UUID.randomUUID();
        UUID toDept = UUID.randomUUID();
        UUID fromUser = UUID.randomUUID();
        UUID toUser = UUID.randomUUID();
        UUID transferredBy = UUID.randomUUID();
        Instant transferredAt = Instant.now();

        // ACT
        AssetCustodyLog log = AssetCustodyLog.builder()
                .asset(asset)
                .fromDepartmentId(fromDept)
                .toDepartmentId(toDept)
                .fromUserId(fromUser)
                .assignedToUserId(toUser)
                .transferredByUserId(transferredBy)
                .transferredAt(transferredAt)
                .reason("New faculty assignment")
                .fromLocation("Room 101")
                .toLocation("Room 305, Block B")
                .build();

        // ASSERT — no setters exist on this class (compile-time guarantee); just verify wiring
        assertThat(log.getAsset()).isEqualTo(asset);
        assertThat(log.getFromDepartmentId()).isEqualTo(fromDept);
        assertThat(log.getToDepartmentId()).isEqualTo(toDept);
        assertThat(log.getFromUserId()).isEqualTo(fromUser);
        assertThat(log.getAssignedToUserId()).isEqualTo(toUser);
        assertThat(log.getTransferredByUserId()).isEqualTo(transferredBy);
        assertThat(log.getTransferredAt()).isEqualTo(transferredAt);
        assertThat(log.getReason()).isEqualTo("New faculty assignment");
        assertThat(log.getFromLocation()).isEqualTo("Room 101");
        assertThat(log.getToLocation()).isEqualTo("Room 305, Block B");
    }

    @Test
    @DisplayName("builder — an asset's very first custody row has a null fromDepartmentId/fromUserId")
    void firstTransfer_fromFieldsAreNull() {
        // ARRANGE
        Asset asset = Asset.builder().assetTag("COM-000002").name("Laptop").build();

        // ACT
        AssetCustodyLog log = AssetCustodyLog.builder()
                .asset(asset)
                .toDepartmentId(UUID.randomUUID())
                .transferredByUserId(UUID.randomUUID())
                .transferredAt(Instant.now())
                .build();

        // ASSERT
        assertThat(log.getFromDepartmentId()).isNull();
        assertThat(log.getFromUserId()).isNull();
    }
}
