package com.adityansh.zerodha.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String symbol;
    private String company;
    private String type;
    private int quantity;
    private double price;
    private double totalAmount;
    private LocalDateTime createdAt;
}