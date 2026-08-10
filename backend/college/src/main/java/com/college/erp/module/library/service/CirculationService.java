package com.college.erp.module.library.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.library.domain.BookCopy;
import com.college.erp.module.library.domain.BookCopyRepository;
import com.college.erp.module.library.domain.BookTitle;
import com.college.erp.module.library.domain.BookTitleRepository;
import com.college.erp.module.library.domain.CopyStatus;
import com.college.erp.module.library.domain.LibraryCustodyLog;
import com.college.erp.module.library.domain.LibraryCustodyLogRepository;
import com.college.erp.module.library.domain.LibraryFine;
import com.college.erp.module.library.domain.LibraryFineRepository;
import com.college.erp.module.library.domain.LoanPolicy;
import com.college.erp.module.library.domain.LoanPolicyRepository;
import com.college.erp.module.library.dto.CustodyLogDto;
import com.college.erp.module.library.dto.IssueBookDto;
import com.college.erp.module.library.dto.RenewBookRequest;
import com.college.erp.module.library.dto.ReturnBookDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [LIB-003]/[LIB-004]/[LIB-005]/[LIB-008], 05-api-documentation.md §8 "Circulation".
 *
 * NOT ENFORCED: loan_policies.renewals_allowed. library_custody_log has no renewal-count column
 * anywhere in 04-database-schema.md, and a renewal extends due_at on the existing row rather than
 * creating a new one (matching the API doc's own response shape — "LibraryCustodyLog object with
 * updated due_at") — there's nowhere in the documented schema to persist how many times a given
 * loan has already been renewed. Revisit if the schema ever adds that column.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CirculationService {

    private static final String MODULE = "LIBRARY";

    private final LibraryCustodyLogRepository libraryCustodyLogRepository;
    private final LibraryFineRepository libraryFineRepository;
    private final LoanPolicyRepository loanPolicyRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookTitleRepository bookTitleRepository;
    private final CatalogService catalogService;
    private final ReservationService reservationService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public CustodyLogDto issue(IssueBookDto request, UUID issuedByUserId) {
        BookCopy copy = catalogService.getCopyEntityByBarcode(request.getBarcode());
        if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new BusinessRuleViolationException("Copy is not available (status: " + copy.getStatus() + ")");
        }

        LoanPolicy policy = getPolicy(request.getBorrowerType());

        if (!libraryCustodyLogRepository.findOverdueByBorrower(request.getBorrowerUserId()).isEmpty()) {
            throw new BusinessRuleViolationException("Borrower has overdue books");
        }
        if (libraryCustodyLogRepository.countActiveLoans(request.getBorrowerUserId()) >= policy.getMaxBooks()) {
            throw new BusinessRuleViolationException("Borrower has reached their loan limit ("
                    + policy.getMaxBooks() + ")");
        }

        Instant issuedAt = Instant.now();
        LibraryCustodyLog custodyLog = LibraryCustodyLog.builder()
                .copy(copy)
                .borrowerUserId(request.getBorrowerUserId())
                .borrowerName(request.getBorrowerName())
                .borrowerType(request.getBorrowerType())
                .issuedByUserId(issuedByUserId)
                .issuedAt(issuedAt)
                .dueAt(issuedAt.plus(policy.getLoanDays(), ChronoUnit.DAYS))
                .conditionOnIssue(copy.getCondition())
                .build();
        LibraryCustodyLog saved = libraryCustodyLogRepository.save(custodyLog);

        copy.setStatus(CopyStatus.ISSUED);
        bookCopyRepository.save(copy);
        adjustAvailableCopies(copy.getTitle(), -1);

        auditLogWriter.write(AuditAction.BOOK_ISSUED, MODULE, "LibraryCustodyLog", saved.getId().toString(),
                "Issued " + copy.getBarcode() + " to " + request.getBorrowerName());
        log.info("Book issued: [{}] to [{}]", copy.getBarcode(), request.getBorrowerName());

        return CustodyLogDto.from(saved);
    }

    @Transactional
    public CustodyLogDto returnBook(ReturnBookDto request, UUID returnedByUserId) {
        BookCopy copy = catalogService.getCopyEntityByBarcode(request.getBarcode());
        LibraryCustodyLog custodyLog = libraryCustodyLogRepository.findActiveByCopyId(copy.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No active issue found for copy " + copy.getBarcode()));

        Instant returnedAt = Instant.now();
        BigDecimal fineAmount = calculateFine(custodyLog, returnedAt);
        custodyLog.recordReturn(returnedByUserId, returnedAt, request.getConditionOnReturn(), fineAmount);
        libraryCustodyLogRepository.save(custodyLog);

        adjustAvailableCopies(copy.getTitle(), 1);
        // A reservation held for the next person in queue takes the copy off the AVAILABLE shelf.
        boolean heldForReservation = reservationService.notifyNextInQueue(copy.getTitle().getId());
        copy.setStatus(heldForReservation ? CopyStatus.RESERVED : CopyStatus.AVAILABLE);
        copy.setCondition(request.getConditionOnReturn());
        bookCopyRepository.save(copy);

        if (fineAmount != null && fineAmount.signum() > 0) {
            LibraryFine fine = LibraryFine.builder()
                    .userId(custodyLog.getBorrowerUserId())
                    .custodyLog(custodyLog)
                    .amount(fineAmount)
                    .build();
            libraryFineRepository.save(fine);
        }

        auditLogWriter.write(AuditAction.BOOK_RETURNED, MODULE, "LibraryCustodyLog", custodyLog.getId().toString(),
                "Returned " + copy.getBarcode() + (fineAmount != null ? ", fine: " + fineAmount : ""));

        return CustodyLogDto.from(custodyLog);
    }

    @Transactional
    public CustodyLogDto renew(RenewBookRequest request, UUID requestingUserId) {
        BookCopy copy = catalogService.getCopyEntityByBarcode(request.getBarcode());
        LibraryCustodyLog custodyLog = libraryCustodyLogRepository.findActiveByCopyId(copy.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No active issue found for copy " + copy.getBarcode()));

        if (custodyLog.isOverdue(Instant.now())) {
            throw new BusinessRuleViolationException("Book is overdue — cannot be renewed");
        }
        if (reservationService.hasActiveReservation(copy.getTitle().getId())) {
            throw new BusinessRuleViolationException("This title has an active reservation — cannot be renewed");
        }

        LoanPolicy policy = getPolicy(custodyLog.getBorrowerType());
        custodyLog.extendDueDate(custodyLog.getDueAt().plus(policy.getLoanDays(), ChronoUnit.DAYS));
        libraryCustodyLogRepository.save(custodyLog);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "LibraryCustodyLog", custodyLog.getId().toString(),
                "Renewed " + copy.getBarcode() + " — new due date " + custodyLog.getDueAt());

        return CustodyLogDto.from(custodyLog);
    }

    @Transactional(readOnly = true)
    public List<CustodyLogDto> custodyChain(UUID copyId) {
        return libraryCustodyLogRepository.findByCopyIdOrderByIssuedAtDesc(copyId).stream()
                .map(CustodyLogDto::from)
                .toList();
    }

    /** For LibraryOverdueScanJob — every overdue loan system-wide, as managed entities (the job
     *  needs custodyLog.getCopy().getTitle(), not just a DTO). Kept here rather than the job
     *  injecting LibraryCustodyLogRepository directly (07-coding-standards.md §4.4: no
     *  cross-module repository access). */
    @Transactional(readOnly = true)
    public List<LibraryCustodyLog> listAllOverdueEntities() {
        return libraryCustodyLogRepository.findAllOverdue();
    }

    /** 03-srs.md [LIB-005]: days_late × fine_per_day, capped at max_fine if configured. */
    // Package-private (not private) so FineAccrualTest can exercise it directly.
    BigDecimal calculateFine(LibraryCustodyLog custodyLog, Instant returnedAt) {
        if (!returnedAt.isAfter(custodyLog.getDueAt())) {
            return null;
        }
        LoanPolicy policy = getPolicy(custodyLog.getBorrowerType());
        long daysLate = ChronoUnit.DAYS.between(custodyLog.getDueAt(), returnedAt);
        if (daysLate <= 0) {
            return null;
        }

        BigDecimal fine = policy.getFinePerDay().multiply(BigDecimal.valueOf(daysLate));
        if (policy.getMaxFine() != null && fine.compareTo(policy.getMaxFine()) > 0) {
            return policy.getMaxFine();
        }
        return fine;
    }

    private LoanPolicy getPolicy(com.college.erp.module.library.domain.BorrowerType borrowerType) {
        return loanPolicyRepository.findByBorrowerTypeAndDeletedFalse(borrowerType)
                .orElseThrow(() -> new ResourceNotFoundException("LoanPolicy for borrower type", borrowerType));
    }

    private void adjustAvailableCopies(BookTitle title, int delta) {
        title.adjustAvailableCopies(delta);
        bookTitleRepository.save(title);
    }
}
