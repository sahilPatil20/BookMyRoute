package com.bookmyroute.config;

import com.bookmyroute.entity.User;
import com.bookmyroute.enums.Role;
import com.bookmyroute.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrapConfig {

    @Value("${app.admin.email:book.my.route2026@gmail.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@12345}")
    private String adminPassword;

    @Value("${app.admin.name:BookMyRoute Admin}")
    private String adminName;

    @Value("${app.admin.reset-password:true}")
    private boolean resetAdminPassword;

    @Bean
    public CommandLineRunner createDefaultAdmin(UserRepository userRepository,
                                                PasswordEncoder passwordEncoder) {
        return args -> userRepository.findByEmail(adminEmail).ifPresentOrElse(
                user -> {
                    boolean changed = false;
                    if (user.getRole() != Role.ADMIN || !user.getIsActive()) {
                        user.setRole(Role.ADMIN);
                        user.setIsActive(true);
                        changed = true;
                    }
                    if (resetAdminPassword && !passwordEncoder.matches(adminPassword, user.getPasswordHash())) {
                        user.setPasswordHash(passwordEncoder.encode(adminPassword));
                        changed = true;
                    }
                    if (changed) {
                        userRepository.save(user);
                    }
                },
                () -> {
                    User admin = User.builder()
                            .name(adminName)
                            .email(adminEmail)
                            .passwordHash(passwordEncoder.encode(adminPassword))
                            .role(Role.ADMIN)
                            .isActive(true)
                            .build();
                    userRepository.save(admin);
                });
    }
}
