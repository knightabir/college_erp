package com.college.erp.module.placement.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.college.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** INVENTED — see V016's header comment. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "placement_offers")
public class PlacementOffer extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drive_application_id", nullable = false, unique = true)
    private DriveApplication driveApplication;

    @Column(name = "ctc", nullable = false, precision = 12, scale = 2)
    private BigDecimal ctc;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "offer_date", nullable = false)
    private LocalDate offerDate;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OfferStatus status = OfferStatus.OFFERED;

    public void accept() {
        this.status = OfferStatus.ACCEPTED;
    }

    public void decline() {
        this.status = OfferStatus.DECLINED;
    }
}
