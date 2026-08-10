package com.college.erp.module.examination.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

/**
 * 03-srs.md [EXAM-005] calls for "grade (per institution-configured grading scale)" but no such
 * table exists anywhere in 04-database-schema.md — a configurable grading-scale table would be
 * new schema, not something inferrable from what's documented. This hardcodes the standard 10-
 * point scale used across Indian universities (matches the worked example in
 * 05-api-documentation.md §6: 90% → grade "O", 10.0 points). Swap this for a real
 * GradingScaleRepository-backed lookup if/when the docs specify one — every caller already goes
 * through this single class.
 */
@Component
public class GradingScale {

    public record GradeResult(String grade, BigDecimal gradePoints) {
    }

    public GradeResult gradeFor(BigDecimal percentage) {
        double pct = percentage.doubleValue();
        if (pct >= 90) {
            return new GradeResult("O", points(10.0));
        }
        if (pct >= 80) {
            return new GradeResult("A+", points(9.0));
        }
        if (pct >= 70) {
            return new GradeResult("A", points(8.0));
        }
        if (pct >= 60) {
            return new GradeResult("B+", points(7.0));
        }
        if (pct >= 50) {
            return new GradeResult("B", points(6.0));
        }
        if (pct >= 45) {
            return new GradeResult("C", points(5.0));
        }
        if (pct >= 40) {
            return new GradeResult("P", points(4.0));
        }
        return new GradeResult("F", points(0.0));
    }

    private BigDecimal points(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
