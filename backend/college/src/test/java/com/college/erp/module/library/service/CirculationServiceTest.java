package com.college.erp.module.library.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.library.domain.BookCopy;
import com.college.erp.module.library.domain.BookCopyRepository;
import com.college.erp.module.library.domain.BookTitle;
import com.college.erp.module.library.domain.BookTitleRepository;
import com.college.erp.module.library.domain.BorrowerType;
import com.college.erp.module.library.domain.CopyCondition;
import com.college.erp.module.library.domain.CopyStatus;
import com.college.erp.module.library.domain.LibraryCustodyLog;
import com.college.erp.module.library.domain.LibraryCustodyLogRepository;
import com.college.erp.module.library.domain.LibraryFineRepository;
import com.college.erp.module.library.domain.LoanPolicy;
import com.college.erp.module.library.domain.LoanPolicyRepository;
import com.college.erp.module.library.dto.CustodyLogDto;
import com.college.erp.module.library.dto.IssueBookDto;
import com.college.erp.module.library.dto.RenewBookRequest;
import com.college.erp.module.library.dto.ReturnBookDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 03-srs.md [LIB-004]/[LIB-008]. Named to match 08-system-architecture.md's test tree. */
@ExtendWith(MockitoExtension.class)
class CirculationServiceTest {

    @Mock
    private LibraryCustodyLogRepository libraryCustodyLogRepository;
    @Mock
    private LibraryFineRepository libraryFineRepository;
    @Mock
    private LoanPolicyRepository loanPolicyRepository;
    @Mock
    private BookCopyRepository bookCopyRepository;
    @Mock
    private BookTitleRepository bookTitleRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private ReservationService reservationService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private CirculationService circulationService;

    private BookTitle buildTitle() {
        BookTitle title = BookTitle.builder().title("Clean Code").author("Robert C. Martin")
                .totalCopies(1).availableCopies(1).build();
        title.setId(UUID.randomUUID());
        return title;
    }

    private BookCopy buildCopy(BookTitle title, CopyStatus status) {
        BookCopy copy = BookCopy.builder().title(title).barcode("LIB-2026-000001")
                .accessionNumber("2026-000001").status(status).condition(CopyCondition.GOOD).build();
        copy.setId(UUID.randomUUID());
        return copy;
    }

    private LoanPolicy buildPolicy(int maxBooks) {
        return LoanPolicy.builder().borrowerType(BorrowerType.STUDENT).maxBooks(maxBooks)
                .loanDays(14).finePerDay(BigDecimal.valueOf(2)).maxFine(BigDecimal.valueOf(200)).build();
    }

    private IssueBookDto buildIssueRequest(String barcode, UUID borrowerId) {
        IssueBookDto request = new IssueBookDto();
        request.setBarcode(barcode);
        request.setBorrowerUserId(borrowerId);
        request.setBorrowerName("Arjun Kumar");
        request.setBorrowerType(BorrowerType.STUDENT);
        return request;
    }

    @Test
    @DisplayName("issue() — a copy that isn't AVAILABLE is rejected")
    void issue_copyNotAvailable_throws() {
        // ARRANGE
        circulationService = new CirculationService(libraryCustodyLogRepository, libraryFineRepository,
                loanPolicyRepository, bookCopyRepository, bookTitleRepository, catalogService,
                reservationService, auditLogWriter);
        BookTitle title = buildTitle();
        BookCopy copy = buildCopy(title, CopyStatus.ISSUED);
        when(catalogService.getCopyEntityByBarcode(copy.getBarcode())).thenReturn(copy);

        // ACT / ASSERT
        assertThatThrownBy(() -> circulationService.issue(
                buildIssueRequest(copy.getBarcode(), UUID.randomUUID()), UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("issue() — a borrower with overdue books is rejected")
    void issue_borrowerHasOverdue_throwsBusinessRuleViolation() {
        // ARRANGE
        circulationService = new CirculationService(libraryCustodyLogRepository, libraryFineRepository,
                loanPolicyRepository, bookCopyRepository, bookTitleRepository, catalogService,
                reservationService, auditLogWriter);
        BookTitle title = buildTitle();
        BookCopy copy = buildCopy(title, CopyStatus.AVAILABLE);
        UUID borrowerId = UUID.randomUUID();
        when(catalogService.getCopyEntityByBarcode(copy.getBarcode())).thenReturn(copy);
        when(loanPolicyRepository.findByBorrowerTypeAndDeletedFalse(BorrowerType.STUDENT))
                .thenReturn(Optional.of(buildPolicy(3)));
        when(libraryCustodyLogRepository.findOverdueByBorrower(borrowerId))
                .thenReturn(List.of(LibraryCustodyLog.builder().build()));

        // ACT / ASSERT
        assertThatThrownBy(() -> circulationService.issue(
                buildIssueRequest(copy.getBarcode(), borrowerId), UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("issue() — a borrower at their loan limit is rejected")
    void issue_loanLimitReached_throws() {
        // ARRANGE
        circulationService = new CirculationService(libraryCustodyLogRepository, libraryFineRepository,
                loanPolicyRepository, bookCopyRepository, bookTitleRepository, catalogService,
                reservationService, auditLogWriter);
        BookTitle title = buildTitle();
        BookCopy copy = buildCopy(title, CopyStatus.AVAILABLE);
        UUID borrowerId = UUID.randomUUID();
        when(catalogService.getCopyEntityByBarcode(copy.getBarcode())).thenReturn(copy);
        when(loanPolicyRepository.findByBorrowerTypeAndDeletedFalse(BorrowerType.STUDENT))
                .thenReturn(Optional.of(buildPolicy(3)));
        when(libraryCustodyLogRepository.findOverdueByBorrower(borrowerId)).thenReturn(List.of());
        when(libraryCustodyLogRepository.countActiveLoans(borrowerId)).thenReturn(3L);

        // ACT / ASSERT
        assertThatThrownBy(() -> circulationService.issue(
                buildIssueRequest(copy.getBarcode(), borrowerId), UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("issue() — passes all checks: copy moves to ISSUED, title's available count drops")
    void issue_allChecksPass_issuesBook() {
        // ARRANGE
        circulationService = new CirculationService(libraryCustodyLogRepository, libraryFineRepository,
                loanPolicyRepository, bookCopyRepository, bookTitleRepository, catalogService,
                reservationService, auditLogWriter);
        BookTitle title = buildTitle();
        BookCopy copy = buildCopy(title, CopyStatus.AVAILABLE);
        UUID borrowerId = UUID.randomUUID();
        when(catalogService.getCopyEntityByBarcode(copy.getBarcode())).thenReturn(copy);
        when(loanPolicyRepository.findByBorrowerTypeAndDeletedFalse(BorrowerType.STUDENT))
                .thenReturn(Optional.of(buildPolicy(3)));
        when(libraryCustodyLogRepository.findOverdueByBorrower(borrowerId)).thenReturn(List.of());
        when(libraryCustodyLogRepository.countActiveLoans(borrowerId)).thenReturn(0L);
        when(libraryCustodyLogRepository.save(any(LibraryCustodyLog.class))).thenAnswer(inv -> {
            LibraryCustodyLog l = inv.getArgument(0);
            return LibraryCustodyLog.builder().id(UUID.randomUUID()).copy(l.getCopy())
                    .borrowerUserId(l.getBorrowerUserId()).borrowerName(l.getBorrowerName())
                    .borrowerType(l.getBorrowerType()).issuedByUserId(l.getIssuedByUserId())
                    .issuedAt(l.getIssuedAt()).dueAt(l.getDueAt()).conditionOnIssue(l.getConditionOnIssue())
                    .build();
        });

        // ACT
        CustodyLogDto result = circulationService.issue(
                buildIssueRequest(copy.getBarcode(), borrowerId), UUID.randomUUID());

        // ASSERT
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.ISSUED);
        assertThat(title.getAvailableCopies()).isZero();
        assertThat(result.getDueAt()).isEqualTo(result.getIssuedAt().plus(14, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("renew() — an overdue book cannot be renewed")
    void renew_overdue_throwsBusinessRuleViolation() {
        // ARRANGE
        circulationService = new CirculationService(libraryCustodyLogRepository, libraryFineRepository,
                loanPolicyRepository, bookCopyRepository, bookTitleRepository, catalogService,
                reservationService, auditLogWriter);
        BookTitle title = buildTitle();
        BookCopy copy = buildCopy(title, CopyStatus.ISSUED);
        LibraryCustodyLog log = LibraryCustodyLog.builder().copy(copy).borrowerType(BorrowerType.STUDENT)
                .issuedAt(Instant.now().minus(30, ChronoUnit.DAYS)).dueAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        when(catalogService.getCopyEntityByBarcode(copy.getBarcode())).thenReturn(copy);
        when(libraryCustodyLogRepository.findActiveByCopyId(copy.getId())).thenReturn(Optional.of(log));

        RenewBookRequest request = new RenewBookRequest();
        request.setBarcode(copy.getBarcode());

        // ACT / ASSERT
        assertThatThrownBy(() -> circulationService.renew(request, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("renew() — a title with an active reservation cannot be renewed")
    void renew_titleHasReservation_throws() {
        // ARRANGE
        circulationService = new CirculationService(libraryCustodyLogRepository, libraryFineRepository,
                loanPolicyRepository, bookCopyRepository, bookTitleRepository, catalogService,
                reservationService, auditLogWriter);
        BookTitle title = buildTitle();
        BookCopy copy = buildCopy(title, CopyStatus.ISSUED);
        LibraryCustodyLog log = LibraryCustodyLog.builder().copy(copy).borrowerType(BorrowerType.STUDENT)
                .issuedAt(Instant.now().minus(5, ChronoUnit.DAYS)).dueAt(Instant.now().plus(9, ChronoUnit.DAYS))
                .build();

        when(catalogService.getCopyEntityByBarcode(copy.getBarcode())).thenReturn(copy);
        when(libraryCustodyLogRepository.findActiveByCopyId(copy.getId())).thenReturn(Optional.of(log));
        when(reservationService.hasActiveReservation(title.getId())).thenReturn(true);

        RenewBookRequest request = new RenewBookRequest();
        request.setBarcode(copy.getBarcode());

        // ACT / ASSERT
        assertThatThrownBy(() -> circulationService.renew(request, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("renew() — eligible loan extends due_at by the standard loan period")
    void renew_eligible_extendsDueDate() {
        // ARRANGE
        circulationService = new CirculationService(libraryCustodyLogRepository, libraryFineRepository,
                loanPolicyRepository, bookCopyRepository, bookTitleRepository, catalogService,
                reservationService, auditLogWriter);
        BookTitle title = buildTitle();
        BookCopy copy = buildCopy(title, CopyStatus.ISSUED);
        Instant originalDueAt = Instant.now().plus(2, ChronoUnit.DAYS);
        LibraryCustodyLog log = LibraryCustodyLog.builder().id(UUID.randomUUID()).copy(copy)
                .borrowerType(BorrowerType.STUDENT)
                .issuedAt(Instant.now().minus(12, ChronoUnit.DAYS)).dueAt(originalDueAt).build();

        when(catalogService.getCopyEntityByBarcode(copy.getBarcode())).thenReturn(copy);
        when(libraryCustodyLogRepository.findActiveByCopyId(copy.getId())).thenReturn(Optional.of(log));
        when(reservationService.hasActiveReservation(title.getId())).thenReturn(false);
        when(loanPolicyRepository.findByBorrowerTypeAndDeletedFalse(BorrowerType.STUDENT))
                .thenReturn(Optional.of(buildPolicy(3)));
        when(libraryCustodyLogRepository.save(any(LibraryCustodyLog.class))).thenAnswer(inv -> inv.getArgument(0));

        RenewBookRequest request = new RenewBookRequest();
        request.setBarcode(copy.getBarcode());

        // ACT
        CustodyLogDto result = circulationService.renew(request, UUID.randomUUID());

        // ASSERT
        assertThat(result.getDueAt()).isEqualTo(originalDueAt.plus(14, ChronoUnit.DAYS));
    }
}
