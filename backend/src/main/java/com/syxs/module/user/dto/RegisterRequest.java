package com.syxs.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Nickname is required")
    private String nickname;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "OTP code is required")
    private String otpCode;

    @NotBlank(message = "Password is required")
    private String password;
}
