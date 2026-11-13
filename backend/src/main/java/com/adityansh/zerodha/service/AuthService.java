package com.adityansh.zerodha.service;

import com.adityansh.zerodha.dto.AuthRequest;
import com.adityansh.zerodha.entity.User;
import com.adityansh.zerodha.repository.UserRepository;
import com.adityansh.zerodha.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UserRepository repo, PasswordEncoder encoder, JwtService jwtService) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public User register(AuthRequest request) {
        if (repo.findByUsername(request.username()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        return repo.save(new User(
                null,
                request.username().trim(),
                encoder.encode(request.password()),
                request.name() == null || request.name().isBlank()
                        ? request.username().trim()
                        : request.name().trim(),
                100000
        ));
    }

    public User login(AuthRequest request) {
        User user = repo.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!encoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        return user;
    }

    public String token(User user) {
        return jwtService.generateToken(user.getUsername());
    }
}
