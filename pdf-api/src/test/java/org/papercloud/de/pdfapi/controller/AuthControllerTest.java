package org.papercloud.de.pdfapi.controller;


import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.papercloud.de.common.dto.auth.LoginRequest;
import org.papercloud.de.common.dto.auth.RegisterRequest;
import org.papercloud.de.pdfdatabase.entity.RoleEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.RoleRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Clean up existing test user if exists
        userRepository.findByUsername("integrationtestuser").ifPresent(user -> {
            userRepository.delete(user);
        });

        // Ensure roles exist
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            RoleEntity userRole = new RoleEntity();
            userRole.setName("ROLE_USER");
            roleRepository.save(userRole);
        }

        // Setup register request
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("integrationtestuser");
        registerRequest.setEmail("integrationtest@example.com");
        registerRequest.setPassword("Password123");
        registerRequest.setConfirmPassword("Password123");

        // Setup login request
        loginRequest = new LoginRequest();
        loginRequest.setUsername("integrationtestuser");
        loginRequest.setPassword("Password123");
    }

    @Test
    void register_WithValidRequest_ShouldReturnToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").exists())
                .andExpect(jsonPath("$.username").value("integrationtestuser"));
    }

    /**
    @Test
    @Ignore
    void register_WithExistingUsername_ShouldReturnBadRequest() throws Exception {

        // Create a user first
        createTestUser();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }
*/
    @Test
    void login_WithValidCredentials_ShouldReturnToken() throws Exception {
        // Create a user first
        createTestUser();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").exists())
                .andExpect(jsonPath("$.username").value("integrationtestuser"));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    private void createTestUser() {
        RoleEntity userRole = roleRepository.findByName("ROLE_USER").orElseThrow();

        UserEntity user = new UserEntity();
        user.setUsername("integrationtestuser");
        user.setEmail("integrationtest@example.com");
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setEnabled(true);

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);
    }
}
