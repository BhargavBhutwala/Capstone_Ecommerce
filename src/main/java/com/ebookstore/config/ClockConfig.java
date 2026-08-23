package com.ebookstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Clock configuration.
 *
 * <p>Provides a UTC {@link Clock} bean for production use.
 * Tests substitute a fixed {@link Clock} to control time-dependent behavior
 * without {@code Thread.sleep()}.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
