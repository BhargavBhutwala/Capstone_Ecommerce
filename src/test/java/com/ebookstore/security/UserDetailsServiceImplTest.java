package com.ebookstore.security;

import com.ebookstore.common.domain.UserRole;
import com.ebookstore.user.entity.User;
import com.ebookstore.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserDetailsServiceImpl}.
 *
 * <p>No Spring context — Mockito stubs {@link UserRepository}.
 */
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new UserDetailsServiceImpl(userRepository);
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    void loadUserByUsername_returnsAuthenticatedUserWithCorrectId() {
        User user = buildUser(7L, "alice@example.com", "$2a$10$hashedpassword", UserRole.CUSTOMER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        AuthenticatedUser result =
                (AuthenticatedUser) userDetailsService.loadUserByUsername("alice@example.com");

        assertThat(result.getId()).isEqualTo(7L);
    }

    @Test
    void loadUserByUsername_returnsAuthenticatedUserWithCorrectEmail() {
        User user = buildUser(7L, "alice@example.com", "$2a$10$hashedpassword", UserRole.CUSTOMER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        AuthenticatedUser result =
                (AuthenticatedUser) userDetailsService.loadUserByUsername("alice@example.com");

        assertThat(result.getUsername()).isEqualTo("alice@example.com");
    }

    @Test
    void loadUserByUsername_returnsAuthenticatedUserWithCorrectRole() {
        User user = buildUser(7L, "alice@example.com", "$2a$10$hashedpassword", UserRole.CUSTOMER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        AuthenticatedUser result =
                (AuthenticatedUser) userDetailsService.loadUserByUsername("alice@example.com");

        assertThat(result.getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void loadUserByUsername_returnsAuthenticatedUserWithPasswordHash() {
        User user = buildUser(7L, "alice@example.com", "$2a$10$hashedpassword", UserRole.CUSTOMER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        AuthenticatedUser result =
                (AuthenticatedUser) userDetailsService.loadUserByUsername("alice@example.com");

        assertThat(result.getPassword()).isEqualTo("$2a$10$hashedpassword");
    }

    @Test
    void loadUserByUsername_grantsRoleAuthority() {
        User user = buildUser(7L, "alice@example.com", "$2a$10$hashedpassword", UserRole.CUSTOMER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        AuthenticatedUser result =
                (AuthenticatedUser) userDetailsService.loadUserByUsername("alice@example.com");

        assertThat(result.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void loadUserByUsername_adminRoleGrantsAdminAuthority() {
        User user = buildUser(99L, "admin@example.com", "$2a$10$adminhash", UserRole.ADMIN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        AuthenticatedUser result =
                (AuthenticatedUser) userDetailsService.loadUserByUsername("admin@example.com");

        assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(result.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_ADMIN");
    }

    // =========================================================================
    // Missing user
    // =========================================================================

    @Test
    void loadUserByUsername_throwsUsernameNotFoundExceptionWhenUserMissing() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost@example.com");
    }

    // =========================================================================
    // toString does not expose password
    // =========================================================================

    @Test
    void authenticatedUser_toStringDoesNotContainPasswordHash() {
        User user = buildUser(7L, "alice@example.com", "$2a$10$hashedpassword", UserRole.CUSTOMER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        AuthenticatedUser result =
                (AuthenticatedUser) userDetailsService.loadUserByUsername("alice@example.com");

        assertThat(result.toString()).doesNotContain("$2a$10$hashedpassword");
        assertThat(result.toString()).doesNotContain("hashed");
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private User buildUser(Long id, String email, String passwordHash, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        return user;
    }
}
