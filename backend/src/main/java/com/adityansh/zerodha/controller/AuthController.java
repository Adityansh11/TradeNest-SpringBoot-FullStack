package com.adityansh.zerodha.controller;

import com.adityansh.zerodha.dto.AuthRequest;
import com.adityansh.zerodha.dto.AuthResponse;
import com.adityansh.zerodha.entity.User;
import com.adityansh.zerodha.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRequest request) {
        User user = service.register(request);
        return response(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        User user = service.login(request);
        return response(user);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(
                service.token(user),
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getCashBalance()
        );
    }
}
