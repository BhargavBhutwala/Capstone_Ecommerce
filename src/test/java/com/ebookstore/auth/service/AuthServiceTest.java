package com.ebookstore.auth.service;

import com.ebookstore.auth.dto.LoginRequest;
import com.ebookstore.auth.dto.LoginResponse;
import com.ebookstore.auth.dto.RegisterRequest;
import com.ebookstore.cart.entity.Cart;
import com.ebookstore.cart.repository.CartRepository;
import com.ebookstore.common.domain.CartStatus;
import com.ebookstore.common.domain.UserRole;
import com.ebookstore.common.domain.UserStatus;
import com.ebookstore.common.exception.BusinessRuleViolationException;
import com.ebookstore.security.AuthenticatedUser;
import com.ebookstore.security.JwtTokenProvider;
import com.ebookstore.user.dto.UserResponse;
import com.ebookstore.user.entity.User;
import com.ebookstore.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}.
 * No Spring context — all dependencies are mocked.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CartRepository cartRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, cartRepository, passwordEncoder,
                authenticationManager, jwtTokenProvider);
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 3_600_000L);
    }

    // =========================================================================
    // Registration
    // =========================================================================

    @Test
    void register_returnsUserResponse_onSuccess() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("$hashed$");
        User saved = buildUser(1L, "Alice", "Smith", "alice@example.com");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(cartRepository.save(any(Cart.class))).thenReturn(new Cart());

        UserResponse result = authService.register(buildRegisterRequest());

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void register_setsRoleToCustomer() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$hashed$");
        User saved = buildUser(1L, "Alice", "Smith", "alice@example.com");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(cartRepository.save(any(Cart.class))).thenReturn(new Cart());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        authService.register(buildRegisterRequest());
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void register_setsStatusToActive() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$hashed$");
        User saved = buildUser(1L, "Alice", "Smith", "alice@example.com");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(cartRepository.save(any(Cart.class))).thenReturn(new Cart());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        authService.register(buildRegisterRequest());
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void register_createsExactlyOneCart() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$hashed$");
        User saved = buildUser(1L, "Alice", "Smith", "alice@example.com");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(cartRepository.save(any(Cart.class))).thenReturn(new Cart());

        authService.register(buildRegisterRequest());

        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void register_cartStatusIsActive() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$hashed$");
        User saved = buildUser(1L, "Alice", "Smith", "alice@example.com");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        when(cartRepository.save(cartCaptor.capture())).thenReturn(new Cart());

        authService.register(buildRegisterRequest());

        assertThat(cartCaptor.getValue().getStatus()).isEqualTo(CartStatus.ACTIVE);
    }

    @Test
    void register_throwsBusinessRuleViolation_onDuplicateEmail() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(buildRegisterRequest()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any(User.class));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    // =========================================================================
    // Login
    // =========================================================================

    @Test
    void login_returnsLoginResponse_onValidCredentials() {
        AuthenticatedUser principal = new AuthenticatedUser(
                5L, "alice@example.com", "$hashed$", UserRole.CUSTOMER);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateToken(principal)).thenReturn("jwt.token.here");
        User user = buildUser(5L, "Alice", "Smith", "alice@example.com");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        LoginResponse result = authService.login(new LoginRequest() {{
            setEmail("alice@example.com");
            setPassword("password1");
        }});

        assertThat(result.getAccessToken()).isEqualTo("jwt.token.here");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(3600L);
        assertThat(result.getUser().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void login_throwsAuthenticationException_onInvalidCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest() {{
            setEmail("alice@example.com");
            setPassword("wrongpassword");
        }})).isInstanceOf(BadCredentialsException.class);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Alice");
        req.setLastName("Smith");
        req.setEmail("alice@example.com");
        req.setPassword("password1");
        return req;
    }

    private User buildUser(Long id, String first, String last, String email) {
        User user = new User();
        user.setId(id);
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
