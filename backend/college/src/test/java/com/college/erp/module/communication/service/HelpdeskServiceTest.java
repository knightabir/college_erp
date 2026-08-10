package com.college.erp.module.communication.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.communication.domain.HelpdeskTicket;
import com.college.erp.module.communication.domain.HelpdeskTicketRepository;
import com.college.erp.module.communication.domain.TicketCategory;
import com.college.erp.module.communication.domain.TicketCommentRepository;
import com.college.erp.module.communication.domain.TicketPriority;
import com.college.erp.module.communication.domain.TicketStatus;
import com.college.erp.module.communication.dto.AddCommentRequest;
import com.college.erp.module.communication.dto.HelpdeskTicketDto;
import com.college.erp.module.communication.dto.RaiseTicketRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 03-srs.md [COMM-002]. Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class HelpdeskServiceTest {

    @Mock
    private HelpdeskTicketRepository helpdeskTicketRepository;
    @Mock
    private TicketCommentRepository ticketCommentRepository;
    @Mock
    private AuditLogWriter auditLogWriter;

    private HelpdeskService helpdeskService;

    private HelpdeskTicket buildTicket(UUID id, UUID raisedBy, TicketStatus status, UUID assignedTo) {
        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .raisedByUserId(raisedBy).title("Fee payment not reflecting")
                .description("Paid via UPI, portal still shows outstanding")
                .category(TicketCategory.FINANCIAL).priority(TicketPriority.HIGH)
                .status(status).assignedTo(assignedTo).build();
        ticket.setId(id);
        return ticket;
    }

    @Test
    @DisplayName("raise() — defaults priority to MEDIUM and status to OPEN")
    void raise_defaultsPriorityAndStatus() {
        // ARRANGE
        helpdeskService = new HelpdeskService(helpdeskTicketRepository, ticketCommentRepository, auditLogWriter);
        UUID userId = UUID.randomUUID();
        when(helpdeskTicketRepository.save(any(HelpdeskTicket.class))).thenAnswer(inv -> {
            HelpdeskTicket ticket = inv.getArgument(0);
            ticket.setId(UUID.randomUUID());
            return ticket;
        });

        RaiseTicketRequest request = new RaiseTicketRequest();
        request.setTitle("Login not working");
        request.setDescription("Cannot log in since this morning");
        request.setCategory(TicketCategory.IT);

        // ACT
        HelpdeskTicketDto dto = helpdeskService.raise(request, userId);

        // ASSERT
        assertThat(dto.getPriority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(dto.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(dto.getRaisedByUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("assign() — OPEN → ASSIGNED, records the assignee")
    void assign_movesToAssigned() {
        // ARRANGE
        helpdeskService = new HelpdeskService(helpdeskTicketRepository, ticketCommentRepository, auditLogWriter);
        UUID ticketId = UUID.randomUUID();
        HelpdeskTicket ticket = buildTicket(ticketId, UUID.randomUUID(), TicketStatus.OPEN, null);
        UUID assignee = UUID.randomUUID();
        when(helpdeskTicketRepository.findByIdAndDeletedFalse(ticketId)).thenReturn(Optional.of(ticket));
        when(helpdeskTicketRepository.save(any(HelpdeskTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        HelpdeskTicketDto dto = helpdeskService.assign(ticketId, assignee);

        // ASSERT
        assertThat(dto.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(dto.getAssignedTo()).isEqualTo(assignee);
    }

    @Test
    @DisplayName("resolve() — sets status RESOLVED, resolution text, resolvedAt")
    void resolve_setsResolutionFields() {
        // ARRANGE
        helpdeskService = new HelpdeskService(helpdeskTicketRepository, ticketCommentRepository, auditLogWriter);
        UUID ticketId = UUID.randomUUID();
        HelpdeskTicket ticket = buildTicket(ticketId, UUID.randomUUID(), TicketStatus.IN_PROGRESS, UUID.randomUUID());
        when(helpdeskTicketRepository.findByIdAndDeletedFalse(ticketId)).thenReturn(Optional.of(ticket));
        when(helpdeskTicketRepository.save(any(HelpdeskTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        HelpdeskTicketDto dto = helpdeskService.resolve(ticketId, "Reset the password");

        // ASSERT
        assertThat(dto.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(dto.getResolution()).isEqualTo("Reset the password");
        assertThat(dto.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("close() — only a RESOLVED ticket can be closed")
    void close_notResolved_throws() {
        // ARRANGE
        helpdeskService = new HelpdeskService(helpdeskTicketRepository, ticketCommentRepository, auditLogWriter);
        UUID ticketId = UUID.randomUUID();
        HelpdeskTicket ticket = buildTicket(ticketId, UUID.randomUUID(), TicketStatus.OPEN, null);
        when(helpdeskTicketRepository.findByIdAndDeletedFalse(ticketId)).thenReturn(Optional.of(ticket));

        // ACT / ASSERT
        assertThatThrownBy(() -> helpdeskService.close(ticketId)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("addComment() — the ticket owner may comment")
    void addComment_owner_succeeds() {
        // ARRANGE
        helpdeskService = new HelpdeskService(helpdeskTicketRepository, ticketCommentRepository, auditLogWriter);
        UUID ownerId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        HelpdeskTicket ticket = buildTicket(ticketId, ownerId, TicketStatus.ASSIGNED, UUID.randomUUID());
        when(helpdeskTicketRepository.findByIdAndDeletedFalse(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.save(any())).thenAnswer(inv -> {
            var comment = inv.getArgument(0, com.college.erp.module.communication.domain.TicketComment.class);
            comment.setId(UUID.randomUUID());
            return comment;
        });

        AddCommentRequest request = new AddCommentRequest();
        request.setComment("Any update?");

        // ACT / ASSERT — no exception
        org.assertj.core.api.Assertions.assertThatCode(() -> helpdeskService.addComment(ticketId, request, ownerId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("addComment() — a user who is neither owner nor assignee is rejected")
    void addComment_notOwnerOrAssignee_throws() {
        // ARRANGE
        helpdeskService = new HelpdeskService(helpdeskTicketRepository, ticketCommentRepository, auditLogWriter);
        UUID ticketId = UUID.randomUUID();
        HelpdeskTicket ticket = buildTicket(ticketId, UUID.randomUUID(), TicketStatus.ASSIGNED, UUID.randomUUID());
        when(helpdeskTicketRepository.findByIdAndDeletedFalse(ticketId)).thenReturn(Optional.of(ticket));

        AddCommentRequest request = new AddCommentRequest();
        request.setComment("Butting in");

        // ACT / ASSERT
        assertThatThrownBy(() -> helpdeskService.addComment(ticketId, request, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("addComment() — no assignee yet: only the owner may comment")
    void addComment_noAssigneeYet_ownerStillWorks() {
        // ARRANGE
        helpdeskService = new HelpdeskService(helpdeskTicketRepository, ticketCommentRepository, auditLogWriter);
        UUID ownerId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        HelpdeskTicket ticket = buildTicket(ticketId, ownerId, TicketStatus.OPEN, null);
        when(helpdeskTicketRepository.findByIdAndDeletedFalse(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.save(any())).thenAnswer(inv -> {
            var comment = inv.getArgument(0, com.college.erp.module.communication.domain.TicketComment.class);
            comment.setId(UUID.randomUUID());
            return comment;
        });

        AddCommentRequest request = new AddCommentRequest();
        request.setComment("Following up");

        // ACT / ASSERT — no NPE from a null assignedTo, no exception
        org.assertj.core.api.Assertions.assertThatCode(() -> helpdeskService.addComment(ticketId, request, ownerId))
                .doesNotThrowAnyException();
    }
}
