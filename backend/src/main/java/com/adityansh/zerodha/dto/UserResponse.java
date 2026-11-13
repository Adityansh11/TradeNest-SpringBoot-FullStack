package com.adityansh.zerodha.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long userId;
    private String username;
    private String name;
    private double cashBalance;
}