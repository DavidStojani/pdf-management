package org.papercloud.de.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Core domain object representing a user.
 * This is a pure domain object with no infrastructure dependencies.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;
    private String username;
    private String email;
    private String password;

    @Builder.Default
    private Set<String> roles = new HashSet<>();

    private boolean enabled;
    private String folderPath;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
