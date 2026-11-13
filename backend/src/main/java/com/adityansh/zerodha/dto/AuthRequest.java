package com.adityansh.zerodha.dto; import jakarta.validation.constraints.*; public record AuthRequest(@NotBlank String username,@NotBlank String password,String name){}
