package com.ebookstore.config;

import com.ebookstore.security.AccessDeniedHandlerImpl;
import com.ebookstore.security.AuthEntryPoint;
import com.ebookstore.security.JwtAuthenticationFilter;
import com.ebookstore.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the E-Bookstore backend.
 *
 * <p>Policy:
 * <ul>
 *   <li>Stateless JWT — no HTTP session created or used</li>
 *   <li>CSRF disabled (safe for stateless REST + Bearer token APIs)</li>
 *   <li>Public paths: POST /auth/register, POST /auth/login,
 *       GET /categories/**, GET /brands/**, GET /products/**, GET /actuator/health</li>
 *   <li>All other paths require a valid Bearer token</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthEntryPoint authEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          AuthEntryPoint authEntryPoint,
                          AccessDeniedHandlerImpl accessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authEntryPoint = authEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public authentication endpoints
                .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                // Public catalog endpoints (OpenAPI security: [])
                .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/brands/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                // Actuator health check — public
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                // Springdoc / Swagger UI — documentation tooling, not a business endpoint
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
