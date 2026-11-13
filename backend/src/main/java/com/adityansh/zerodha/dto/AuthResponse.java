package com.adityansh.zerodha.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String username;
    private String name;
    private double cashBalance;
}
