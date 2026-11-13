package com.adityansh.zerodha.dto; import jakarta.validation.constraints.*; public record TradeRequest(@NotBlank String symbol,@Min(1) int quantity){}
