package com.college.erp.module.communication.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.communication.domain.HelpdeskTicket;
import com.college.erp.module.communication.domain.HelpdeskTicketRepository;
import com.college.erp.module.communication.domain.TicketComment;
import com.college.erp.module.communication.domain.TicketCommentRepository;
import com.college.erp.module.communication.domain.TicketPriority;
import com.college.erp.module.communication.domain.TicketStatus;
import com.college.erp.module.communication.dto.AddCommentRequest;
import com.college.erp.module.communication.dto.HelpdeskTicketDto;
import com.college.erp.module.communication.dto.RaiseTicketRequest;
import com.college.erp.module.communication.dto.TicketCommentDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [COMM-002]: raise (any authenticated user) → assign → (optionally) start progress →
 * resolve → close, plus comments from the ticket owner or the assigned staff. "Assigned to the
 * relevant department" is realized as assignment to a specific staff user (assigned_to) — see the
 * module's CLAUDE.md deviations note for why (no department_id column on helpdesk_tickets).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelpdeskService {

    private static final String MODULE = "COMMUNICATION";

    private final HelpdeskTicketRepository helpdeskTicketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public HelpdeskTicketDto raise(RaiseTicketRequest request, UUID raisedByUserId) {
        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .raisedByUserId(raisedByUserId)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM)
                .status(TicketStatus.OPEN)
                .build();
        HelpdeskTicket saved = helpdeskTicketRepository.save(ticket);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "HelpdeskTicket", saved.getId().toString(),
                "Raised: " + saved.getTitle());
        log.info("Helpdesk ticket raised: [{}] {}", saved.getId(), saved.getTitle());

        return HelpdeskTicketDto.from(saved);
    }

    @Transactional
    public HelpdeskTicketDto assign(UUID id, UUID assigneeUserId) {
        HelpdeskTicket ticket = getEntity(id);
        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new BusinessRuleViolationException("Ticket is already " + ticket.getStatus());
        }
        ticket.assign(assigneeUserId);
        HelpdeskTicket saved = helpdeskTicketRepository.save(ticket);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "HelpdeskTicket", id.toString(),
                "Assigned to " + assigneeUserId);

        return HelpdeskTicketDto.from(saved);
    }

    @Transactional
    public HelpdeskTicketDto startProgress(UUID id) {
        HelpdeskTicket ticket = getEntity(id);
        if (ticket.getStatus() != TicketStatus.ASSIGNED) {
            throw new BusinessRuleViolationException("Only an assigned ticket can move to in-progress");
        }
        ticket.startProgress();
        HelpdeskTicket saved = helpdeskTicketRepository.save(ticket);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "HelpdeskTicket", id.toString(), "Started progress");

        return HelpdeskTicketDto.from(saved);
    }

    @Transactional
    public HelpdeskTicketDto resolve(UUID id, String resolution) {
        HelpdeskTicket ticket = getEntity(id);
        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new BusinessRuleViolationException("Ticket is already " + ticket.getStatus());
        }
        ticket.resolve(resolution);
        HelpdeskTicket saved = helpdeskTicketRepository.save(ticket);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "HelpdeskTicket", id.toString(), "Resolved");

        return HelpdeskTicketDto.from(saved);
    }

    @Transactional
    public HelpdeskTicketDto close(UUID id) {
        HelpdeskTicket ticket = getEntity(id);
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new BusinessRuleViolationException("Only a resolved ticket can be closed");
        }
        ticket.close();
        HelpdeskTicket saved = helpdeskTicketRepository.save(ticket);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "HelpdeskTicket", id.toString(), "Closed");

        return HelpdeskTicketDto.from(saved);
    }

    @Transactional
    public TicketCommentDto addComment(UUID ticketId, AddCommentRequest request, UUID commenterUserId) {
        HelpdeskTicket ticket = getEntity(ticketId);
        boolean isOwner = ticket.getRaisedByUserId().equals(commenterUserId);
        boolean isAssignee = commenterUserId.equals(ticket.getAssignedTo());
        if (!isOwner && !isAssignee) {
            throw new AccessDeniedException("Only the ticket owner or the assigned staff may comment");
        }

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .userId(commenterUserId)
                .comment(request.getComment())
                .build();
        TicketComment saved = ticketCommentRepository.save(comment);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "TicketComment", saved.getId().toString(),
                "Commented on ticket " + ticketId);

        return TicketCommentDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketCommentDto> listComments(UUID ticketId) {
        return ticketCommentRepository.findByTicketIdAndDeletedFalseOrderByCreatedAtAsc(ticketId).stream()
                .map(TicketCommentDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public HelpdeskTicketDto getById(UUID id) {
        return HelpdeskTicketDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<HelpdeskTicketDto> myTickets(UUID userId, TicketStatus status, Pageable pageable) {
        Page<HelpdeskTicket> page = status != null
                ? helpdeskTicketRepository.findByRaisedByUserIdAndStatusAndDeletedFalse(userId, status, pageable)
                : helpdeskTicketRepository.findByRaisedByUserIdAndDeletedFalse(userId, pageable);
        return page.map(HelpdeskTicketDto::from);
    }

    private HelpdeskTicket getEntity(UUID id) {
        return helpdeskTicketRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("HelpdeskTicket", id));
    }
}
