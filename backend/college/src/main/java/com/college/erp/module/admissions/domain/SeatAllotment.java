package com.college.erp.module.admissions.domain;

import java.time.Instant;

import com.college.erp.common.domain.BaseEntity;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Course;

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

/** INVENTED — see V015's header comment. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "seat_allotments")
public class SeatAllotment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merit_list_id")
    private MeritList meritList;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeatAllotmentStatus status = SeatAllotmentStatus.OFFERED;

    @Builder.Default
    @Column(name = "offered_at", nullable = false)
    private Instant offeredAt = Instant.now();

    @Column(name = "response_deadline")
    private Instant responseDeadline;

    @Column(name = "responded_at")
    private Instant respondedAt;

    public void accept() {
        this.status = SeatAllotmentStatus.ACCEPTED;
        this.respondedAt = Instant.now();
    }

    public void decline() {
        this.status = SeatAllotmentStatus.DECLINED;
        this.respondedAt = Instant.now();
    }
}
