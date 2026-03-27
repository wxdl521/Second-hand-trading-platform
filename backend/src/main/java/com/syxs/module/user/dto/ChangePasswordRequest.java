package com.syxs.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 64, message = "New password length must be between 6 and 64")
    private String nextPassword;
}
