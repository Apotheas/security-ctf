package com.ps.securityctf.config;

import com.ps.securityctf.model.User;
import com.ps.securityctf.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {

            // Check if regular user exists, create if not
            if (userRepository.findById(1L).isEmpty()) {
                User regularUser = new User(
                        1L,
                        "user",
                        passwordEncoder.encode("password"),
                        "USER",
                        "Regular User",
                        "user@example.com",
                        "+33 9 87 65 43 21",
                        "456 User Avenue, User Town",
                        "Nothing interesting here"
                );
                userRepository.save(regularUser);
            }

            // Check if alice exists, create if not
            if (userRepository.findById(2L).isEmpty()) {
                User alice = new User(
                        2L,
                        "alice",
                        passwordEncoder.encode("wonderland"),
                        "USER",
                        "Alice Wonder",
                        "alice@example.com",
                        "+33 7 12 34 56 78",
                        "789 Wonderland Lane, Fantasy City",
                        "Loves adventures"
                );
                userRepository.save(alice);
            }

            // Check if bob exists, create if not
            if (userRepository.findById(3L).isEmpty()) {
                User bob = new User(
                        3L,
                        "bob",
                        passwordEncoder.encode("builder123"),
                        "USER",
                        "Bob Builder",
                        "bob@example.com",
                        "+33 7 98 76 54 32",
                        "321 Construction Ave, Builder Town",
                        "Can we fix it? Yes, we can!"
                );
                userRepository.save(bob);
            }

            // Check if charlie exists, create if not
            if (userRepository.findById(4L).isEmpty()) {
                User charlie = new User(
                        4L,
                        "charlie",
                        passwordEncoder.encode("chocolate"),
                        "USER",
                        "Charlie Brown",
                        "charlie@example.com",
                        "+33 7 45 67 89 01",
                        "567 Chocolate Factory Road, Sweet City",
                        "Golden ticket winner"
                );
                userRepository.save(charlie);
            }

            // Check if john exists, create if not
            if (userRepository.findById(5L).isEmpty()) {
                User regularUser = new User(
                        5L,
                        "john",
                        passwordEncoder.encode("nobody"),
                        "USER",
                        "John doe",
                        "jdoe@example.com",
                        "+33 6 45 13 84 52",
                        "65 Tower Street, New York City",
                        "Created by userId=42 (admin)"
                );
                userRepository.save(regularUser);
            }


            // Check if admin user exists, create if not
            if (userRepository.findById(42L).isEmpty()) {
                User admin = new User(
                        42L,
                        "admin",
                        passwordEncoder.encode("adminPassword"),
                        "ADMIN",
                        "System Administrator",
                        "admin@example.com",
                        "+33 1 23 45 67 89",
                        "123 Admin Street, Admin City",
                        "FLAG{OWASP_TOP_10_BROKEN_OBJECT_LEVEL_AUTHORIZATION}"
                );
                userRepository.save(admin);
            }

        };
    }
}
