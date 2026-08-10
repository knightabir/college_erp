package com.college.erp.module.communication.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.module.communication.domain.Notice;
import com.college.erp.module.communication.domain.NoticeRepository;
import com.college.erp.module.communication.domain.NoticeTargetType;
import com.college.erp.module.communication.dto.CreateNoticeRequest;
import com.college.erp.module.communication.dto.NoticeDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 03-srs.md [COMM-001]. Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private AuditLogWriter auditLogWriter;

    private NoticeService noticeService;

    @Test
    @DisplayName("create() — publishes immediately (publishedAt is set at creation)")
    void create_publishesImmediately() {
        // ARRANGE
        noticeService = new NoticeService(noticeRepository, auditLogWriter);
        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        when(noticeRepository.save(captor.capture())).thenAnswer(inv -> {
            Notice notice = inv.getArgument(0);
            notice.setId(UUID.randomUUID());
            return notice;
        });

        CreateNoticeRequest request = new CreateNoticeRequest();
        request.setTitle("End-Term Examination Schedule");
        request.setBody("Exams run Nov 1-15");
        request.setTargetType(NoticeTargetType.ALL);

        // ACT
        NoticeDto dto = noticeService.create(request);

        // ASSERT
        assertThat(dto.getPublishedAt()).isNotNull();
        assertThat(captor.getValue().getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("getRelevant() — delegates to the repository's findRelevant with 'now' and the given scope")
    void getRelevant_delegatesToRepository() {
        // ARRANGE
        noticeService = new NoticeService(noticeRepository, auditLogWriter);
        UUID departmentId = UUID.randomUUID();
        Notice notice = Notice.builder().title("Notice").body("Body").targetType(NoticeTargetType.ALL)
                .publishedAt(Instant.now().minus(1, ChronoUnit.HOURS)).build();
        notice.setId(UUID.randomUUID());
        when(noticeRepository.findRelevant(any(), org.mockito.ArgumentMatchers.eq(departmentId), any()))
                .thenReturn(List.of(notice));

        // ACT
        List<NoticeDto> results = noticeService.getRelevant(departmentId, null);

        // ASSERT
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Notice");
    }
}
