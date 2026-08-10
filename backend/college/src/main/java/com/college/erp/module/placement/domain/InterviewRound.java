package com.college.erp.module.placement.domain;

import java.time.Instant;

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

/** INVENTED — see V016's header comment. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "interview_rounds")
public class InterviewRound extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drive_application_id", nullable = false)
    private DriveApplication driveApplication;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "round_type", length = 30)
    private String roundType;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private InterviewResult result = InterviewResult.PENDING;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "interviewer_name", length = 100)
    private String interviewerName;

    public void recordResult(InterviewResult result, String feedback) {
        this.result = result;
        this.feedback = feedback;
    }
}
