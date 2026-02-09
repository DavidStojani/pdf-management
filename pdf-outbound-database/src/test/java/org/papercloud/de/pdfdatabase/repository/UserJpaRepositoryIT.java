package org.papercloud.de.pdfdatabase.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.papercloud.de.pdfdatabase.entity.RoleEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for UserJpaRepository using real PostgreSQL database via TestContainers.
 * Tests repository operations, query methods, unique constraints, and role relationships.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("UserJpaRepository Integration Tests")
class UserJpaRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    private RoleEntity userRole;
    private RoleEntity adminRole;

    @BeforeEach
    void setUp() {
        // Create roles for testing
        userRole = new RoleEntity();
        userRole.setName("ROLE_USER");
        userRole = entityManager.persist(userRole);

        adminRole = new RoleEntity();
        adminRole.setName("ROLE_ADMIN");
        adminRole = entityManager.persist(adminRole);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("Save and findById operations")
    class SaveAndFindByIdTests {

        @Test
        @DisplayName("should save user and retrieve by id")
        void saveAndFindById_validUser_shouldSucceed() {
            // Arrange
            UserEntity user = createTestUser("johndoe", "john@example.com");

            // Act
            UserEntity savedUser = userJpaRepository.save(user);
            entityManager.flush();
            entityManager.clear();
            Optional<UserEntity> foundUser = userJpaRepository.findById(savedUser.getId());

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
            assertThat(foundUser.get().getUsername()).isEqualTo("johndoe");
            assertThat(foundUser.get().getEmail()).isEqualTo("john@example.com");
            assertThat(foundUser.get().getPassword()).isEqualTo("hashedPassword123");
            assertThat(foundUser.get().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should save user with all fields populated")
        void save_userWithAllFields_shouldPersistAllData() {
            // Arrange
            Set<RoleEntity> roles = new HashSet<>();
            roles.add(userRole);
            roles.add(adminRole);

            UserEntity user = UserEntity.builder()
                    .username("completeuser")
                    .email("complete@example.com")
                    .password("password123")
                    .roles(roles)
                    .enabled(true)
                    .folderPath("/documents/completeuser")
                    .createdAt(LocalDateTime.of(2024, 1, 15, 10, 30))
                    .lastLoginAt(LocalDateTime.of(2024, 1, 16, 14, 45))
                    .build();

            // Act
            UserEntity savedUser = userJpaRepository.save(user);
            entityManager.flush();
            entityManager.clear();
            Optional<UserEntity> foundUser = userJpaRepository.findById(savedUser.getId());

            // Assert
            assertThat(foundUser).isPresent();
            UserEntity retrieved = foundUser.get();
            assertThat(retrieved.getUsername()).isEqualTo("completeuser");
            assertThat(retrieved.getEmail()).isEqualTo("complete@example.com");
            assertThat(retrieved.getFolderPath()).isEqualTo("/documents/completeuser");
            assertThat(retrieved.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30));
            assertThat(retrieved.getLastLoginAt()).isEqualTo(LocalDateTime.of(2024, 1, 16, 14, 45));
            assertThat(retrieved.getRoles()).hasSize(2);
            assertThat(retrieved.getRoles()).extracting(RoleEntity::getName)
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("should save user with minimal required fields")
        void save_userWithMinimalFields_shouldSucceed() {
            // Arrange
            UserEntity user = UserEntity.builder()
                    .username("minimaluser")
                    .password("password123")
                    .enabled(false)
                    .build();

            // Act
            UserEntity savedUser = userJpaRepository.save(user);
            entityManager.flush();
            entityManager.clear();
            Optional<UserEntity> foundUser = userJpaRepository.findById(savedUser.getId());

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getUsername()).isEqualTo("minimaluser");
            assertThat(foundUser.get().getEmail()).isNull();
        }

        @Test
        @DisplayName("should return empty optional for non-existent id")
        void findById_nonExistentId_shouldReturnEmpty() {
            // Act
            Optional<UserEntity> foundUser = userJpaRepository.findById(99999L);

            // Assert
            assertThat(foundUser).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEmail operations")
    class FindByEmailTests {

        @Test
        @DisplayName("should find user by email")
        void findByEmail_existingEmail_shouldReturnUser() {
            // Arrange
            createAndSaveUser("user1", "user1@example.com");

            // Act
            Optional<UserEntity> foundUser = userJpaRepository.findByEmail("user1@example.com");

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getEmail()).isEqualTo("user1@example.com");
            assertThat(foundUser.get().getUsername()).isEqualTo("user1");
        }

        @Test
        @DisplayName("should return empty optional for non-existent email")
        void findByEmail_nonExistentEmail_shouldReturnEmpty() {
            // Act
            Optional<UserEntity> foundUser = userJpaRepository.findByEmail("nonexistent@example.com");

            // Assert
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("should be case-sensitive for email")
        void findByEmail_caseSensitive_shouldNotMatchDifferentCase() {
            // Arrange
            createAndSaveUser("user1", "user1@example.com");

            // Act
            Optional<UserEntity> foundUser = userJpaRepository.findByEmail("USER1@EXAMPLE.COM");

            // Assert
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("should find user with null email when searching with null")
        void findByEmail_nullEmail_shouldFindUserWithNullEmail() {
            // Arrange
            UserEntity user = createTestUser("usernull", null);
            userJpaRepository.save(user);
            entityManager.flush();

            // Act - Spring Data JPA matches null values
            Optional<UserEntity> foundUser = userJpaRepository.findByEmail(null);

            // Assert - null parameter matches records with null email
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getUsername()).isEqualTo("usernull");
            assertThat(foundUser.get().getEmail()).isNull();
        }

        @Test
        @DisplayName("should handle special characters in email")
        void findByEmail_specialCharacters_shouldFindUser() {
            // Arrange
            createAndSaveUser("specialuser", "test.user+tag@sub-domain.example.com");

            // Act
            Optional<UserEntity> foundUser = userJpaRepository.findByEmail("test.user+tag@sub-domain.example.com");

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getEmail()).isEqualTo("test.user+tag@sub-domain.example.com");
        }
    }

    @Nested
    @DisplayName("findByUsername operations")
    class FindByUsernameTests {

        @Test
        @DisplayName("should find user by username")
        void findByUsername_existingUsername_shouldReturnUser() {
            // Arrange
            createAndSaveUser("testuser", "test@example.com");

            // Act
            Optional<UserEntity> foundUser = userJpaRepository.findByUsername("testuser");

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
            assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should return empty optional for non-existent username")
        void findByUsername_nonExistentUsername_shouldReturnEmpty() {
            // Act
            Optional<UserEntity> foundUser = userJpaRepository.findByUsername("nonexistent");

            // Assert
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("should be case-sensitive for username")
        void findByUsername_caseSensitive_shouldNotMatchDifferentCase() {
            // Arrange
            createAndSaveUser("testuser", "test@example.com");

            // Act
            Optional<UserEntity> foundUser = userJpaRepository.findByUsername("TestUser");

            // Assert
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("should handle username with special characters")
        void findByUsername_specialCharacters_shouldFindUser() {
            // Arrange
            createAndSaveUser("user.name-123", "user@example.com");

            // Act
            Optional<UserEntity> foundUser = userJpaRepository.findByUsername("user.name-123");

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getUsername()).isEqualTo("user.name-123");
        }
    }

    @Nested
    @DisplayName("existsByEmail operations")
    class ExistsByEmailTests {

        @Test
        @DisplayName("should return true when email exists")
        void existsByEmail_existingEmail_shouldReturnTrue() {
            // Arrange
            createAndSaveUser("user1", "existing@example.com");

            // Act
            boolean exists = userJpaRepository.existsByEmail("existing@example.com");

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when email does not exist")
        void existsByEmail_nonExistentEmail_shouldReturnFalse() {
            // Act
            boolean exists = userJpaRepository.existsByEmail("nonexistent@example.com");

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("should be case-sensitive")
        void existsByEmail_caseSensitive_shouldReturnFalseForDifferentCase() {
            // Arrange
            createAndSaveUser("user1", "test@example.com");

            // Act
            boolean exists = userJpaRepository.existsByEmail("TEST@EXAMPLE.COM");

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("should return false for null email")
        void existsByEmail_nullEmail_shouldReturnFalse() {
            // Act
            boolean exists = userJpaRepository.existsByEmail(null);

            // Assert
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByUsername operations")
    class ExistsByUsernameTests {

        @Test
        @DisplayName("should return true when username exists")
        void existsByUsername_existingUsername_shouldReturnTrue() {
            // Arrange
            createAndSaveUser("existinguser", "user@example.com");

            // Act
            boolean exists = userJpaRepository.existsByUsername("existinguser");

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when username does not exist")
        void existsByUsername_nonExistentUsername_shouldReturnFalse() {
            // Act
            boolean exists = userJpaRepository.existsByUsername("nonexistent");

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("should be case-sensitive")
        void existsByUsername_caseSensitive_shouldReturnFalseForDifferentCase() {
            // Arrange
            createAndSaveUser("testuser", "test@example.com");

            // Act
            boolean exists = userJpaRepository.existsByUsername("TestUser");

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("should return false for null username")
        void existsByUsername_nullUsername_shouldReturnFalse() {
            // Act
            boolean exists = userJpaRepository.existsByUsername(null);

            // Assert
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Unique constraint violations")
    class UniqueConstraintTests {

        @Test
        @DisplayName("should throw exception when saving user with duplicate username")
        void save_duplicateUsername_shouldThrowException() {
            // Arrange
            createAndSaveUser("duplicateuser", "user1@example.com");

            UserEntity duplicateUser = UserEntity.builder()
                    .username("duplicateuser")
                    .email("different@example.com")
                    .password("password123")
                    .enabled(true)
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> {
                userJpaRepository.save(duplicateUser);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("should throw exception when saving user with duplicate email")
        void save_duplicateEmail_shouldThrowException() {
            // Arrange
            createAndSaveUser("user1", "duplicate@example.com");

            UserEntity duplicateUser = UserEntity.builder()
                    .username("differentuser")
                    .email("duplicate@example.com")
                    .password("password123")
                    .enabled(true)
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> {
                userJpaRepository.save(duplicateUser);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("should allow multiple users with null email")
        void save_multipleUsersWithNullEmail_shouldSucceed() {
            // Arrange
            UserEntity user1 = createTestUser("user1", null);
            UserEntity user2 = createTestUser("user2", null);

            // Act
            userJpaRepository.save(user1);
            userJpaRepository.save(user2);
            entityManager.flush();

            // Assert
            assertThat(userJpaRepository.findByUsername("user1")).isPresent();
            assertThat(userJpaRepository.findByUsername("user2")).isPresent();
        }

        @Test
        @DisplayName("should allow updating user without triggering unique constraint")
        void update_existingUser_shouldNotTriggerUniqueConstraint() {
            // Arrange
            UserEntity user = createAndSaveUser("updateuser", "update@example.com");
            Long userId = user.getId();
            entityManager.clear();

            // Act
            UserEntity managedUser = userJpaRepository.findById(userId).orElseThrow();
            managedUser.setPassword("newPassword456");
            managedUser.setEnabled(false);
            userJpaRepository.save(managedUser);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<UserEntity> updatedUser = userJpaRepository.findById(userId);
            assertThat(updatedUser).isPresent();
            assertThat(updatedUser.get().getPassword()).isEqualTo("newPassword456");
            assertThat(updatedUser.get().isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Role relationship operations")
    class RoleRelationshipTests {

        @Test
        @DisplayName("should save user with roles")
        void save_userWithRoles_shouldPersistRoles() {
            // Arrange
            Set<RoleEntity> roles = new HashSet<>();
            roles.add(userRole);
            roles.add(adminRole);

            UserEntity user = createTestUser("roleuser", "role@example.com");
            user.setRoles(roles);

            // Act
            UserEntity savedUser = userJpaRepository.save(user);
            entityManager.flush();
            entityManager.clear();
            Optional<UserEntity> foundUser = userJpaRepository.findById(savedUser.getId());

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getRoles()).hasSize(2);
            assertThat(foundUser.get().getRoles()).extracting(RoleEntity::getName)
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("should save user without roles")
        void save_userWithoutRoles_shouldSucceed() {
            // Arrange
            UserEntity user = createTestUser("noroleuser", "norole@example.com");

            // Act
            UserEntity savedUser = userJpaRepository.save(user);
            entityManager.flush();
            entityManager.clear();
            Optional<UserEntity> foundUser = userJpaRepository.findById(savedUser.getId());

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getRoles()).isNullOrEmpty();
        }

        @Test
        @DisplayName("should eagerly load roles")
        void findById_user_shouldEagerlyLoadRoles() {
            // Arrange
            Set<RoleEntity> roles = new HashSet<>();
            roles.add(userRole);

            UserEntity user = createTestUser("eageruser", "eager@example.com");
            user.setRoles(roles);
            UserEntity savedUser = userJpaRepository.save(user);
            Long userId = savedUser.getId();
            entityManager.flush();
            entityManager.clear();

            // Act - fetch user (no explicit role fetch needed due to EAGER)
            Optional<UserEntity> foundUser = userJpaRepository.findById(userId);

            // Assert - roles should be loaded even after clearing entity manager
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getRoles()).hasSize(1);
            assertThat(foundUser.get().getRoles()).extracting(RoleEntity::getName)
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("should add roles to existing user")
        void update_addRoles_shouldPersistNewRoles() {
            // Arrange
            Set<RoleEntity> initialRoles = new HashSet<>();
            initialRoles.add(userRole);

            UserEntity user = createTestUser("addrolesuser", "addroles@example.com");
            user.setRoles(initialRoles);
            UserEntity savedUser = userJpaRepository.save(user);
            Long userId = savedUser.getId();
            entityManager.flush();
            entityManager.clear();

            // Act - add admin role
            UserEntity managedUser = userJpaRepository.findById(userId).orElseThrow();
            managedUser.getRoles().add(adminRole);
            userJpaRepository.save(managedUser);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<UserEntity> updatedUser = userJpaRepository.findById(userId);
            assertThat(updatedUser).isPresent();
            assertThat(updatedUser.get().getRoles()).hasSize(2);
            assertThat(updatedUser.get().getRoles()).extracting(RoleEntity::getName)
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("should remove roles from existing user")
        void update_removeRoles_shouldPersistChanges() {
            // Arrange
            Set<RoleEntity> initialRoles = new HashSet<>();
            initialRoles.add(userRole);
            initialRoles.add(adminRole);

            UserEntity user = createTestUser("removerolesuser", "removeroles@example.com");
            user.setRoles(initialRoles);
            UserEntity savedUser = userJpaRepository.save(user);
            Long userId = savedUser.getId();
            entityManager.flush();
            entityManager.clear();

            // Act - remove admin role
            UserEntity managedUser = userJpaRepository.findById(userId).orElseThrow();
            managedUser.getRoles().removeIf(role -> role.getName().equals("ROLE_ADMIN"));
            userJpaRepository.save(managedUser);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<UserEntity> updatedUser = userJpaRepository.findById(userId);
            assertThat(updatedUser).isPresent();
            assertThat(updatedUser.get().getRoles()).hasSize(1);
            assertThat(updatedUser.get().getRoles()).extracting(RoleEntity::getName)
                    .containsExactly("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("User state management")
    class UserStateTests {

        @Test
        @DisplayName("should update enabled status")
        void update_enabledStatus_shouldPersistChange() {
            // Arrange
            UserEntity user = createAndSaveUser("statususer", "status@example.com");
            user.setEnabled(true);
            user = userJpaRepository.save(user);
            Long userId = user.getId();
            entityManager.flush();
            entityManager.clear();

            // Act
            UserEntity managedUser = userJpaRepository.findById(userId).orElseThrow();
            managedUser.setEnabled(false);
            userJpaRepository.save(managedUser);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<UserEntity> updatedUser = userJpaRepository.findById(userId);
            assertThat(updatedUser).isPresent();
            assertThat(updatedUser.get().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should update last login timestamp")
        void update_lastLoginAt_shouldPersistTimestamp() {
            // Arrange
            UserEntity user = createAndSaveUser("loginuser", "login@example.com");
            Long userId = user.getId();
            entityManager.clear();

            LocalDateTime loginTime = LocalDateTime.of(2024, 2, 10, 15, 30);

            // Act
            UserEntity managedUser = userJpaRepository.findById(userId).orElseThrow();
            managedUser.setLastLoginAt(loginTime);
            userJpaRepository.save(managedUser);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<UserEntity> updatedUser = userJpaRepository.findById(userId);
            assertThat(updatedUser).isPresent();
            assertThat(updatedUser.get().getLastLoginAt()).isEqualTo(loginTime);
        }

        @Test
        @DisplayName("should update folder path")
        void update_folderPath_shouldPersistNewPath() {
            // Arrange
            UserEntity user = createAndSaveUser("pathuser", "path@example.com");
            Long userId = user.getId();
            entityManager.clear();

            // Act
            UserEntity managedUser = userJpaRepository.findById(userId).orElseThrow();
            managedUser.setFolderPath("/new/documents/path");
            userJpaRepository.save(managedUser);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<UserEntity> updatedUser = userJpaRepository.findById(userId);
            assertThat(updatedUser).isPresent();
            assertThat(updatedUser.get().getFolderPath()).isEqualTo("/new/documents/path");
        }
    }

    // Helper methods

    private UserEntity createTestUser(String username, String email) {
        return UserEntity.builder()
                .username(username)
                .email(email)
                .password("hashedPassword123")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UserEntity createAndSaveUser(String username, String email) {
        UserEntity user = createTestUser(username, email);
        UserEntity savedUser = userJpaRepository.save(user);
        entityManager.flush();
        entityManager.clear();
        return savedUser;
    }
}
