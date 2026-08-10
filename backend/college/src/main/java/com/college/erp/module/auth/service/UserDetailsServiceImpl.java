package com.college.erp.module.auth.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.college.erp.module.auth.domain.RoleAssignmentRepository;
import com.college.erp.module.auth.domain.User;
import com.college.erp.module.auth.domain.UserRepository;
import com.college.erp.security.jwt.UserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * Loads roles fresh from role_assignments on every request (ADR-004) — never cached in the JWT.
 */
@Primary
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user with username: " + username));

        return toPrincipal(user);
    }

    public UserPrincipal toPrincipal(User user) {
        // findActiveByUserId already filters to active, non-expired assignments.
        Set<GrantedAuthority> authorities = roleAssignmentRepository.findActiveByUserId(user.getId()).stream()
                .map(ra -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + ra.getRole().name()))
                .collect(Collectors.toUnmodifiableSet());

        return UserPrincipal.create(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled(),
                !user.isLocked(),
                List.copyOf(authorities));
    }
}
