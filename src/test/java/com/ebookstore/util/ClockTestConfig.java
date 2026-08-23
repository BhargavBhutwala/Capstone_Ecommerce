package com.ebookstore.util;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Test configuration that provides a fixed {@link Clock} bean.
 *
 * <p>Import this via {@code @Import(ClockTestConfig.class)} in test classes that
 * need deterministic time for cancellation deadline tests. Annotated with
 * {@code @Primary} so it overrides the production {@link com.ebookstore.config.ClockConfig} bean.
 *
 * <p>The fixed instant is set to {@code 2024-01-15T12:00:00Z} by default and
 * can be adjusted per test via the static mutator.
 *
 * <p>Usage in tests:
 * <pre>{@code
 * // Within-deadline test: use default fixed instant (order placed at same instant,
 * // so deadline is 48h later — cancellation is within window)
 *
 * // After-deadline test: override with an instant AFTER the order's cancellation_deadline
 * ClockTestConfig.setInstant(Instant.parse("2024-01-18T00:00:00Z"));
 * }</pre>
 */
@TestConfiguration
public class ClockTestConfig {

    /**
     * A known fixed instant used as the "current time" in tests.
     * Tests that need to advance past the cancellation deadline should update
     * the products' {@code cancellation_deadline} via JdbcTemplate instead of
     * changing this clock — that approach doesn't require static mutation.
     */
    public static final Instant FIXED_INSTANT = Instant.parse("2024-06-15T10:00:00Z");

    @Bean
    @Primary
    public Clock testClock() {
        return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    }
}
