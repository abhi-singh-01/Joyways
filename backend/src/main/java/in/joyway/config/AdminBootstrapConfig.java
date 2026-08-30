package in.joyway.config;

import in.joyway.domain.entity.Role;
import in.joyway.domain.entity.User;
import in.joyway.domain.repository.RoleRepository;
import in.joyway.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapConfig {

    private final AdminBootstrapProperties bootstrapProperties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Bean
    public CommandLineRunner bootstrapAdmin() {
        return args -> {
            log.info("Admin bootstrap diagnostic: properties.enabled={}, env.ADMIN_BOOTSTRAP_ENABLED={}, "
                    + "spring.ADMIN_BOOTSTRAP_ENABLED={}, spring.joyway.admin.bootstrap.enabled={}, email={}",
                    bootstrapProperties.isEnabled(),
                    System.getenv("ADMIN_BOOTSTRAP_ENABLED"),
                    environment.getProperty("ADMIN_BOOTSTRAP_ENABLED"),
                    environment.getProperty("joyway.admin.bootstrap.enabled"),
                    bootstrapProperties.getEmail());

            if (!bootstrapProperties.isEnabled()) {
                log.info("Admin bootstrap disabled.");
                return;
            }

            String email = bootstrapProperties.getEmail();
            if (userRepository.existsByEmail(email)) {
                log.info("Admin user already exists ({}). Skipping bootstrap.", email);
                return;
            }

            String password = bootstrapProperties.getPassword();
            if (password == null || password.isBlank()) {
                throw new IllegalStateException(
                        "ADMIN_BOOTSTRAP_PASSWORD is required when ADMIN_BOOTSTRAP_ENABLED=true.");
            }

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found in database."));

            User admin = User.builder()
                    .name(bootstrapProperties.getName())
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .enabled(true)
                    .roles(Set.of(adminRole))
                    .build();

            userRepository.save(admin);
            log.info("Bootstrap admin user created: {}", email);
        };
    }
}
