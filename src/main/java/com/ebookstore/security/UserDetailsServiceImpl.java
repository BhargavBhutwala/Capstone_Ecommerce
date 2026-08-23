package com.ebookstore.security;

import com.ebookstore.user.entity.User;
import com.ebookstore.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a user from the database by email address for Spring Security.
 *
 * <p>Returns an {@link AuthenticatedUser} containing the database {@code id},
 * so controllers can retrieve the authenticated user's identity without
 * re-querying the database.
 *
 * <p>The password hash is never logged at any level.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the user by email (used as the JWT {@code sub} claim / Spring Security username).
     *
     * @throws UsernameNotFoundException if no user with the given email exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found for email: {}", email);
                    return new UsernameNotFoundException("User not found: " + email);
                });
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole());
    }
}
