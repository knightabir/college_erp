package com.college.erp.module.communication.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {

    List<TicketComment> findByTicketIdAndDeletedFalseOrderByCreatedAtAsc(UUID ticketId);
}
