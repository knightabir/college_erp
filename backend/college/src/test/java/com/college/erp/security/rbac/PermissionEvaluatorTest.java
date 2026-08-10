package com.college.erp.security.rbac;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.college.erp.security.jwt.UserPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionEvaluatorTest {

    @Mock
    private RoleTemplate roleTemplate;

    @Mock
    private GrantResolver grantResolver;

    @InjectMocks
    private PermissionEvaluator permissionEvaluator;

    private Authentication authenticationFor(String role) {
        UserPrincipal principal = UserPrincipal.create(UUID.randomUUID(), UUID.randomUUID(), "jdoe",
                "jdoe@college.edu", "hash", true, true, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("hasPermission() — role template grants unconditionally: layer 1 short-circuits")
    void hasPermission_roleTemplateGrants_returnsTrue() {
        // ARRANGE
        when(roleTemplate.grants("SUPER_ADMIN", "FINANCE.FEE_COLLECTION")).thenReturn(true);

        // ACT
        boolean result = permissionEvaluator.hasPermission(
                authenticationFor("SUPER_ADMIN"), null, "FINANCE.FEE_COLLECTION");

        // ASSERT
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasPermission() — no role template match but an active scoped grant exists: layer 2 grants")
    void hasPermission_noRoleTemplateButActiveGrant_returnsTrue() {
        // ARRANGE
        when(roleTemplate.grants(anyString(), anyString())).thenReturn(false);
        when(grantResolver.hasActiveGrant(any(UUID.class), eq("FINANCE.FEE_COLLECTION"), any()))
                .thenReturn(true);

        // ACT
        boolean result = permissionEvaluator.hasPermission(
                authenticationFor("ACCOUNTANT"), null, "FINANCE.FEE_COLLECTION");

        // ASSERT
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasPermission() — neither role template nor an active grant matches: layer 3 denies")
    void hasPermission_noMatch_returnsFalse() {
        // ARRANGE
        when(roleTemplate.grants(anyString(), anyString())).thenReturn(false);
        when(grantResolver.hasActiveGrant(any(UUID.class), anyString(), any())).thenReturn(false);

        // ACT
        boolean result = permissionEvaluator.hasPermission(
                authenticationFor("STUDENT"), null, "FINANCE.FEE_COLLECTION");

        // ASSERT
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("hasPermission() — unauthenticated principal is always denied")
    void hasPermission_unauthenticated_returnsFalse() {
        // ARRANGE
        UsernamePasswordAuthenticationToken unauthenticated =
                new UsernamePasswordAuthenticationToken("anonymous", null);

        // ACT
        boolean result = permissionEvaluator.hasPermission(unauthenticated, null, "FINANCE.FEE_COLLECTION");

        // ASSERT
        assertThat(result).isFalse();
    }
}
