package org.papercloud.de.pdfdatabase.config;

import org.papercloud.de.pdfdatabase.entity.RoleEntity;
import org.papercloud.de.pdfdatabase.repository.RoleRepository;
import org.springframework.context.annotation.Configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseInitializer {

    @Bean
    public CommandLineRunner initDatabase(RoleRepository roleRepository) {
        return args -> {
            // Initialize roles if they don't exist
            if (roleRepository.findByName("ROLE_USER").isEmpty()) {
                RoleEntity userRole = new RoleEntity();
                userRole.setName("ROLE_USER");
                roleRepository.save(userRole);
            }

            if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
                RoleEntity adminRole = new RoleEntity();
                adminRole.setName("ROLE_ADMIN");
                roleRepository.save(adminRole);
            }
        };
    }
}