package org.papercloud.de.pdfsecurity.service;

import jakarta.annotation.PostConstruct;
import org.papercloud.de.core.dto.auth.AuthResponse;
import org.papercloud.de.core.dto.auth.LoginRequest;
import org.papercloud.de.core.dto.auth.RegisterRequest;
import org.papercloud.de.pdfdatabase.entity.RoleEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.RoleRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.core.ports.inbound.AuthenticationService;
import org.papercloud.de.pdfsecurity.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthenticationJWTServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthenticationJWTServiceImpl(
            UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authenticationManager,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    @PostConstruct

    public void init() {
        System.out.println("AuthenticationJWTServiceImpl initialized");
    }


    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate request
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords don't match");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new user
        UserEntity user = UserEntity.builder().username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
        // Set default role
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found")));
        user.setRoles(roles);

        UserEntity savedUser = userRepository.save(user);

        // Generate verification token
        String verificationToken = generateVerificationToken(savedUser);

        // Send verification email
        //emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);

        // Generate JWT token
        UserDetails userDetails = new User(
                savedUser.getUsername(), savedUser.getPassword(),
                savedUser.isEnabled(), true, true, true,
                savedUser.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList()));

        String token = jwtUtil.generateToken(userDetails);

        return buildAuthResponse(savedUser, token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        org.springframework.security.core.userdetails.User principal =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        UserEntity user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Update last login time
        //user.setLastLogin(new Date());
        userRepository.save(user);

        String token = jwtUtil.generateToken(principal);

        return buildAuthResponse(user, token);
    }

    private AuthResponse buildAuthResponse(UserEntity user, String token) {
        return AuthResponse.builder()
                .jwtToken(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(RoleEntity::getName)
                        .toArray(String[]::new))
                .build();
    }

    private String generateVerificationToken(UserEntity user) {
        // Implementation of token generation
        // This would create a verification token and store it in the database
        // ...
        return "token-value";
    }
}
