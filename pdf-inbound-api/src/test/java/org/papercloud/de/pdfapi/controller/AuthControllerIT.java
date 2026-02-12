package org.papercloud.de.pdfapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.papercloud.de.pdfapi.PdfApiApplication;
import org.papercloud.de.pdfdatabase.entity.RoleEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.RoleRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = PdfApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
public class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        ensureRoleExists("ROLE_USER");
    }

    @Nested
    class Register {

        @Test
        void validRequest_returns200WithToken() throws Exception {
            Map<String, String> request = Map.of(
                    "username", "newuser",
                    "email", "newuser@test.com",
                    "password", "password123",
                    "confirmPassword", "password123"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jwtToken").exists())
                    .andExpect(jsonPath("$.username").value("newuser"))
                    .andExpect(jsonPath("$.email").value("newuser@test.com"))
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.roles").isArray());
        }

        @Test
        void passwordMismatch_returns400() throws Exception {
            Map<String, String> request = Map.of(
                    "username", "newuser",
                    "email", "newuser@test.com",
                    "password", "password123",
                    "confirmPassword", "differentPassword"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        void duplicateUsername_returns400() throws Exception {
            createUser("existinguser", "existing@test.com");

            Map<String, String> request = Map.of(
                    "username", "existinguser",
                    "email", "new@test.com",
                    "password", "password123",
                    "confirmPassword", "password123"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        void duplicateEmail_returns400() throws Exception {
            createUser("existinguser", "taken@test.com");

            Map<String, String> request = Map.of(
                    "username", "newuser",
                    "email", "taken@test.com",
                    "password", "password123",
                    "confirmPassword", "password123"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        void missingEmail_returns400() throws Exception {
            Map<String, String> request = Map.of(
                    "username", "newuser",
                    "password", "password123",
                    "confirmPassword", "password123"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Login {

        @Test
        void validCredentials_returns200WithToken() throws Exception {
            createUser("loginuser", "login@test.com");

            Map<String, String> request = Map.of(
                    "email", "login@test.com",
                    "password", "password123"
            );

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jwtToken").exists())
                    .andExpect(jsonPath("$.username").value("loginuser"))
                    .andExpect(jsonPath("$.email").value("login@test.com"))
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.roles").isArray());
        }

        @Test
        void wrongPassword_returns401() throws Exception {
            createUser("loginuser", "login@test.com");

            Map<String, String> request = Map.of(
                    "email", "login@test.com",
                    "password", "wrongpassword"
            );

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void nonExistentUser_returns401() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "ghost@test.com",
                    "password", "password123"
            );

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void invalidEmailFormat_returns400() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "not-an-email",
                    "password", "password123"
            );

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    // --- Test helpers ---

    private void ensureRoleExists(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            RoleEntity role = new RoleEntity();
            role.setName(roleName);
            roleRepository.save(role);
        }
    }

    private UserEntity createUser(String username, String email) {
        RoleEntity userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

        UserEntity user = UserEntity.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .roles(Set.of(userRole))
                .build();
        return userRepository.save(user);
    }
}
