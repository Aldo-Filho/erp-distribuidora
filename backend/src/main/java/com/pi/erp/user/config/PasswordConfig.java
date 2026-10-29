package com.pi.erp.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Salt: 16 bytes; hash: 32 bytes; parallelism: 1; memory: 64 MiB; iterations: 3.
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }
}
