package com.college.erp.module.examination.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HallTicketRepository extends JpaRepository<HallTicket, UUID> {

    Optional<HallTicket> findByExamIdAndStudentIdAndDeletedFalse(UUID examId, UUID studentId);

    List<HallTicket> findByExamIdAndDeletedFalse(UUID examId);

    boolean existsByHallTicketNumber(String hallTicketNumber);
}
