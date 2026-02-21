package com.devhub.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyOtpRequest {
    @NotBlank(message = "OTP is required")
    public String otp;
    @NotBlank(message = "Email is required")
    public String email;
}
