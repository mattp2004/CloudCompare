package org.cloudcompare.backend.auth;

import org.cloudcompare.backend.db.account.AccountRepo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

@Configuration
public class UserDetailsServiceConfig {
    @Bean
    public UserDetailsService userDetailsService(AccountRepo accountRepo) {
        return username -> {
            var user = accountRepo.retrieveByUsername(username);
            if (user == null) throw new UsernameNotFoundException(username);

            return org.springframework.security.core.userdetails.User.withUsername(user.getUsername()).password(user.getHashedPassword()).authorities(Collections.emptyList()).build();
        };
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}