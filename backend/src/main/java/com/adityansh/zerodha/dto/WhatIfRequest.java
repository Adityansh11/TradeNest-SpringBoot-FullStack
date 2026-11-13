package com.adityansh.zerodha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WhatIfRequest(
        @NotBlank String symbol,
        @NotNull Double percentChange
) {}
