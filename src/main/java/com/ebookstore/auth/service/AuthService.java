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
import com.ebookstore.user.UserMapper;
import com.ebookstore.user.dto.UserResponse;
import com.ebookstore.user.entity.User;
import com.ebookstore.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for registration and login.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    public AuthService(UserRepository userRepository,
                       CartRepository cartRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Registers a new customer.
     *
     * <p>Within a single transaction:
     * <ol>
     *   <li>Rejects duplicate email with 409.</li>
     *   <li>Hashes the password with BCrypt.</li>
     *   <li>Persists a {@link User} with role CUSTOMER and status ACTIVE.</li>
     *   <li>Creates exactly one empty {@link Cart} linked to the new user.</li>
     * </ol>
     *
     * @return {@link UserResponse} for the newly created user
     * @throws BusinessRuleViolationException if the email is already registered
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleViolationException(
                    "Email address is already registered: " + request.getEmail());
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setStatus(CartStatus.ACTIVE);
        cartRepository.save(cart);

        log.info("Registered new user id={} email={}", user.getId(), user.getEmail());
        return UserMapper.toResponse(user);
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid
     */
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(principal);
        long expiresInSeconds = jwtExpirationMs / 1000;

        log.info("User logged in: email={}", principal.getUsername());
        return new LoginResponse(token, "Bearer", expiresInSeconds, UserMapper.toResponse(
                // Reload entity to return full UserResponse (principal only holds id/email/role)
                userRepository.findById(principal.getId())
                        .orElseThrow()));
    }
}
