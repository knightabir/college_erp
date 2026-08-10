package com.college.erp.module.admissions.domain;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.common.domain.BaseEntity;

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

/** INVENTED — see V015's header comment. Mirrors module.student's StudentDocument shape. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "application_documents")
public class ApplicationDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "file_id", length = 36)
    private String fileId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private DocumentVerificationStatus verificationStatus = DocumentVerificationStatus.PENDING;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    public void verify(UUID verifierUserId) {
        this.verificationStatus = DocumentVerificationStatus.VERIFIED;
        this.verifiedBy = verifierUserId;
        this.verifiedAt = Instant.now();
    }

    public void reject(UUID verifierUserId) {
        this.verificationStatus = DocumentVerificationStatus.REJECTED;
        this.verifiedBy = verifierUserId;
        this.verifiedAt = Instant.now();
    }
}
