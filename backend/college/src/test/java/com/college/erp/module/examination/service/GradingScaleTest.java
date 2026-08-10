package com.college.erp.module.examination.service;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.college.erp.module.examination.service.GradingScale.GradeResult;

import static org.assertj.core.api.Assertions.assertThat;

class GradingScaleTest {

    private final GradingScale gradingScale = new GradingScale();

    @Test
    @DisplayName("gradeFor() — 90% matches the worked example in 05-api-documentation.md §6 (O / 10.0)")
    void gradeFor_ninetyPercent_returnsO() {
        GradeResult result = gradingScale.gradeFor(BigDecimal.valueOf(90));

        assertThat(result.grade()).isEqualTo("O");
        assertThat(result.gradePoints()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("gradeFor() — boundaries are inclusive at the lower edge of each band")
    void gradeFor_boundaries() {
        assertThat(gradingScale.gradeFor(BigDecimal.valueOf(80)).grade()).isEqualTo("A+");
        assertThat(gradingScale.gradeFor(BigDecimal.valueOf(70)).grade()).isEqualTo("A");
        assertThat(gradingScale.gradeFor(BigDecimal.valueOf(60)).grade()).isEqualTo("B+");
        assertThat(gradingScale.gradeFor(BigDecimal.valueOf(50)).grade()).isEqualTo("B");
        assertThat(gradingScale.gradeFor(BigDecimal.valueOf(45)).grade()).isEqualTo("C");
        assertThat(gradingScale.gradeFor(BigDecimal.valueOf(40)).grade()).isEqualTo("P");
    }

    @Test
    @DisplayName("gradeFor() — below 40% is F with zero grade points")
    void gradeFor_belowForty_returnsF() {
        GradeResult result = gradingScale.gradeFor(BigDecimal.valueOf(39.99));

        assertThat(result.grade()).isEqualTo("F");
        assertThat(result.gradePoints()).isEqualByComparingTo("0.00");
    }
}
