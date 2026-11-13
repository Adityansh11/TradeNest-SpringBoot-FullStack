package com.adityansh.zerodha.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BacktestRequest(
        @NotBlank String symbol,
        @NotBlank String strategy,
        @Min(1000) double initialCapital,
        @Min(30) @Max(252) int lookbackDays
) {}
