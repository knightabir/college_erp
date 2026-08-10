package com.college.erp.module.student.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.module.student.domain.StudentRepository;
import com.college.erp.security.jwt.UserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * @PreAuthorize helper (07-coding-standards.md §5.1 pattern:
 * "hasAnyRole(...) or @studentSecurityService.isOwnProfile(#id, principal)"). Bean name
 * "studentSecurityService" is the default (uncapitalized class name) Spring gives this bean —
 * that's what the SpEL expression in controllers references.
 */
@Service
@RequiredArgsConstructor
public class StudentSecurityService {

    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public boolean isOwnProfile(UUID studentId, UserPrincipal principal) {
        if (principal == null) {
            return false;
        }
        return studentRepository.findByIdAndDeletedFalse(studentId)
                .map(student -> principal.getId().equals(student.getUserId()))
                .orElse(false);
    }
}
