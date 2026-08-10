package com.college.erp.security.jwt;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Fallback so the kernel is startable on its own (no user can ever authenticate). module.auth's
 * UserDetailsServiceImpl is marked @Primary and supersedes this once that module exists.
 */
@Component
class NoOpUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) {
        throw new UsernameNotFoundException("No auth module registered yet: " + usernameOrEmail);
    }
}
