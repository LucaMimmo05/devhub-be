package com.devhub.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class VerifyEmailRequest {
    @NotBlank(message = "Verification code is required")
    @Email(message = "Invalid email format")
    public String email;
}
