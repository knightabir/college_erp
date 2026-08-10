package com.college.erp.module.communication.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 03-srs.md [COMM-001]: "Notices have a publish timestamp and optional expiry." */
class NoticeTest {

    @Test
    @DisplayName("isCurrentlyVisible() — not yet published: false")
    void isCurrentlyVisible_notYetPublished_isFalse() {
        Notice notice = Notice.builder().title("t").body("b").publishedAt(null).build();
        assertThat(notice.isCurrentlyVisible(Instant.now())).isFalse();
    }

    @Test
    @DisplayName("isCurrentlyVisible() — published, no expiry: true")
    void isCurrentlyVisible_publishedNoExpiry_isTrue() {
        Notice notice = Notice.builder().title("t").body("b")
                .publishedAt(Instant.now().minus(1, ChronoUnit.HOURS)).build();
        assertThat(notice.isCurrentlyVisible(Instant.now())).isTrue();
    }

    @Test
    @DisplayName("isCurrentlyVisible() — published but expired: false")
    void isCurrentlyVisible_expired_isFalse() {
        Notice notice = Notice.builder().title("t").body("b")
                .publishedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS)).build();
        assertThat(notice.isCurrentlyVisible(Instant.now())).isFalse();
    }

    @Test
    @DisplayName("isCurrentlyVisible() — published, expiry in the future: true")
    void isCurrentlyVisible_notYetExpired_isTrue() {
        Notice notice = Notice.builder().title("t").body("b")
                .publishedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS)).build();
        assertThat(notice.isCurrentlyVisible(Instant.now())).isTrue();
    }
}
