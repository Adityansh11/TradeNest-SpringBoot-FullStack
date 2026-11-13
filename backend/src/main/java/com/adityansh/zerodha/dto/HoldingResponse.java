package com.adityansh.zerodha.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HoldingResponse {

    private Long id;
    private String symbol;
    private String company;
    private int quantity;
    private double averagePrice;
    private double currentPrice;
}