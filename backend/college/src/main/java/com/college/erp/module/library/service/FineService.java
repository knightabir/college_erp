package com.college.erp.module.library.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.library.domain.FineStatus;
import com.college.erp.module.library.domain.LibraryFine;
import com.college.erp.module.library.domain.LibraryFineRepository;
import com.college.erp.module.library.dto.FineDto;
import com.college.erp.security.jwt.UserPrincipal;
import com.college.erp.security.rbac.GrantResolver;
import com.college.erp.security.rbac.PermissionScope;
import com.college.erp.security.rbac.RoleTemplate;

import lombok.RequiredArgsConstructor;

/**
 * 03-srs.md [LIB-005]/[LIB-006]. Waiver authority is amount-scoped for Library Assistants, which
 * the generic {@code hasPermission(...)} SpEL form can't express (it has no way to carry this
 * specific fine's amount as the PermissionScope) — so this re-runs the same Layer-1/Layer-2 logic
 * PermissionEvaluator uses, directly, with a scope built from the fine being waived. Chief
 * Librarians get unconditional authority via RoleTemplate (see its javadoc); everyone else needs
 * an active LIBRARY.FINE_WAIVER permission_grant whose amount_threshold covers this fine.
 */
@Service
@RequiredArgsConstructor
public class FineService {

    private static final String MODULE = "LIBRARY";
    private static final String WAIVER_PERMISSION = "LIBRARY.FINE_WAIVER";

    private final LibraryFineRepository libraryFineRepository;
    private final RoleTemplate roleTemplate;
    private final GrantResolver grantResolver;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public FineDto payFine(UUID id, BigDecimal amount) {
        LibraryFine fine = getEntity(id);
        if (fine.getStatus() == FineStatus.PAID || fine.getStatus() == FineStatus.WAIVED) {
            throw new BusinessRuleViolationException("Fine is already " + fine.getStatus());
        }
        if (amount.compareTo(fine.balanceDue()) > 0) {
            throw new BusinessRuleViolationException("Amount exceeds the outstanding balance of " + fine.balanceDue());
        }

        fine.setPaidAmount(fine.getPaidAmount().add(amount));
        fine.setStatus(fine.getPaidAmount().compareTo(fine.getAmount()) >= 0 ? FineStatus.PAID : FineStatus.PARTIAL);
        LibraryFine saved = libraryFineRepository.save(fine);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "LibraryFine", id.toString(),
                "Paid " + amount + " toward fine");

        return FineDto.from(saved);
    }

    @Transactional
    public FineDto waive(UUID id, String reason) {
        LibraryFine fine = getEntity(id);
        if (fine.getStatus() == FineStatus.PAID || fine.getStatus() == FineStatus.WAIVED) {
            throw new BusinessRuleViolationException("Fine is already " + fine.getStatus());
        }

        assertWaiverAuthority(fine.balanceDue());

        UserPrincipal principal = currentPrincipal();
        fine.setStatus(FineStatus.WAIVED);
        fine.setWaivedByUserId(principal.getId());
        fine.setWaiverReason(reason);
        LibraryFine saved = libraryFineRepository.save(fine);

        auditLogWriter.write(AuditAction.FINE_WAIVED, MODULE, "LibraryFine", id.toString(),
                "Waived: " + reason);

        return FineDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FineDto> listByUser(UUID userId) {
        return libraryFineRepository.findByUserIdAndDeletedFalse(userId).stream()
                .map(FineDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FineDto> listOutstandingByUser(UUID userId) {
        return libraryFineRepository.findByUserIdAndStatusInAndDeletedFalse(
                        userId, List.of(FineStatus.UNPAID, FineStatus.PARTIAL))
                .stream()
                .map(FineDto::from)
                .toList();
    }

    private void assertWaiverAuthority(BigDecimal fineAmount) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("Not authenticated");
        }

        boolean unconditional = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .anyMatch(role -> roleTemplate.grants(role, WAIVER_PERMISSION));
        if (unconditional) {
            return;
        }

        PermissionScope scope = new PermissionScope(null, null, null, fineAmount);
        if (!grantResolver.hasActiveGrant(principal.getId(), WAIVER_PERMISSION, scope)) {
            throw new AccessDeniedException(
                    "You are not authorized to waive a fine of this amount — ask the Chief Librarian for a grant");
        }
    }

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private LibraryFine getEntity(UUID id) {
        return libraryFineRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("LibraryFine", id));
    }
}
