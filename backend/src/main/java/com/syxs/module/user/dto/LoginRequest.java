package com.syxs.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "OTP code is required")
    private String otpCode;
}
