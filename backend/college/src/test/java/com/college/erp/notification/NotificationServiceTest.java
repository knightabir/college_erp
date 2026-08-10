package com.college.erp.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.notification.channel.EmailChannel;
import com.college.erp.notification.channel.SmsChannel;
import com.college.erp.notification.sse.SseNotificationPusher;
import com.college.erp.notification.template.TemplateRenderer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 06-design-document.md §8.2. Named to match the kernel's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRecordRepository notificationRecordRepository;
    @Mock
    private NotificationTemplateRepository notificationTemplateRepository;
    @Mock
    private SseNotificationPusher sseNotificationPusher;
    @Mock
    private EmailChannel emailChannel;
    @Mock
    private SmsChannel smsChannel;

    private final TemplateRenderer templateRenderer = new TemplateRenderer();
    private NotificationService notificationService;

    @Test
    @DisplayName("send() — persists a NotificationRecord and pushes it over SSE, "
            + "even with both external channels disabled")
    void send_persistsAndPushes_channelsDisabled() {
        // ARRANGE
        notificationService = new NotificationService(notificationRecordRepository,
                notificationTemplateRepository, templateRenderer, sseNotificationPusher, emailChannel, smsChannel);
        UUID userId = UUID.randomUUID();
        when(notificationRecordRepository.save(any(NotificationRecord.class))).thenAnswer(inv -> {
            NotificationRecord record = inv.getArgument(0);
            return record.toBuilder().id(UUID.randomUUID()).build();
        });
        when(emailChannel.isEnabled()).thenReturn(false);
        when(smsChannel.isEnabled()).thenReturn(false);

        NotificationRequest request = NotificationRequest.builder()
                .userId(userId).title("Test Title").body("Test Body").build();

        // ACT
        notificationService.send(request);

        // ASSERT
        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getTitle()).isEqualTo("Test Title");
        assertThat(captor.getValue().getChannel()).isEqualTo(NotificationChannelType.IN_APP);
        verify(sseNotificationPusher).push(any(NotificationRecord.class));
        verify(emailChannel, never()).send(any());
        verify(smsChannel, never()).send(any());
    }

    @Test
    @DisplayName("send() — enabled channels are invoked, in addition to the always-persisted in-app record")
    void send_enabledChannels_areInvoked() {
        // ARRANGE
        notificationService = new NotificationService(notificationRecordRepository,
                notificationTemplateRepository, templateRenderer, sseNotificationPusher, emailChannel, smsChannel);
        when(notificationRecordRepository.save(any(NotificationRecord.class))).thenAnswer(inv -> {
            NotificationRecord record = inv.getArgument(0);
            return record.toBuilder().id(UUID.randomUUID()).build();
        });
        when(emailChannel.isEnabled()).thenReturn(true);
        when(smsChannel.isEnabled()).thenReturn(true);

        NotificationRequest request = NotificationRequest.builder()
                .userId(UUID.randomUUID()).title("Fee Reminder").body("Pay up").build();

        // ACT
        notificationService.send(request);

        // ASSERT
        verify(emailChannel, times(1)).send(any(NotificationRecord.class));
        verify(smsChannel, times(1)).send(any(NotificationRecord.class));
    }

    @Test
    @DisplayName("send() — a template key renders subject/body from the NotificationTemplate, not the raw request")
    void send_withTemplate_rendersFromTemplate() {
        // ARRANGE
        notificationService = new NotificationService(notificationRecordRepository,
                notificationTemplateRepository, templateRenderer, sseNotificationPusher, emailChannel, smsChannel);
        NotificationTemplate template = NotificationTemplate.builder()
                .templateKey("FEE_REMINDER").subject("Reminder: {{invoiceNumber}}")
                .bodyText("Balance due: {{amount}}").active(true).build();
        when(notificationTemplateRepository.findByTemplateKeyAndActiveTrueAndDeletedFalse("FEE_REMINDER"))
                .thenReturn(Optional.of(template));
        when(notificationRecordRepository.save(any(NotificationRecord.class))).thenAnswer(inv -> {
            NotificationRecord record = inv.getArgument(0);
            return record.toBuilder().id(UUID.randomUUID()).build();
        });
        when(emailChannel.isEnabled()).thenReturn(false);
        when(smsChannel.isEnabled()).thenReturn(false);

        NotificationRequest request = NotificationRequest.builder()
                .userId(UUID.randomUUID()).templateKey("FEE_REMINDER")
                .variables(java.util.Map.of("invoiceNumber", "INV-001", "amount", "5000"))
                .title("ignored").body("ignored")
                .build();

        // ACT
        notificationService.send(request);

        // ASSERT
        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Reminder: INV-001");
        assertThat(captor.getValue().getBody()).isEqualTo("Balance due: 5000");
    }

    @Test
    @DisplayName("markAllRead() — marks every unread record and returns the count")
    void markAllRead_marksAllAndReturnsCount() {
        // ARRANGE
        notificationService = new NotificationService(notificationRecordRepository,
                notificationTemplateRepository, templateRenderer, sseNotificationPusher, emailChannel, smsChannel);
        UUID userId = UUID.randomUUID();
        NotificationRecord n1 = NotificationRecord.builder().userId(userId).title("A").body("a").build();
        NotificationRecord n2 = NotificationRecord.builder().userId(userId).title("B").body("b").build();
        when(notificationRecordRepository.findByUserIdAndReadFalse(userId)).thenReturn(List.of(n1, n2));

        // ACT
        int count = notificationService.markAllRead(userId);

        // ASSERT
        assertThat(count).isEqualTo(2);
        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
        verify(notificationRecordRepository).saveAll(List.of(n1, n2));
    }
}
