package com.college.erp.module.communication.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HelpdeskTicketRepository extends JpaRepository<HelpdeskTicket, UUID> {

    Optional<HelpdeskTicket> findByIdAndDeletedFalse(UUID id);

    Page<HelpdeskTicket> findByRaisedByUserIdAndDeletedFalse(UUID userId, Pageable pageable);

    Page<HelpdeskTicket> findByRaisedByUserIdAndStatusAndDeletedFalse(
            UUID userId, TicketStatus status, Pageable pageable);

    List<HelpdeskTicket> findByStatusAndDeletedFalse(TicketStatus status);
}
