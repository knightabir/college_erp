package com.college.erp.module.admissions.domain;

import java.math.BigDecimal;

import com.college.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** INVENTED — see V015's header comment. A one-to-one extension of Application for candidates
 *  entering directly into a later year (e.g. diploma holders). */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lateral_entry_applications")
public class LateralEntryApplication extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Column(name = "diploma_institution", length = 200)
    private String diplomaInstitution;

    @Column(name = "diploma_score", precision = 5, scale = 2)
    private BigDecimal diplomaScore;

    @Column(name = "entry_year_number")
    private Integer entryYearNumber;
}
