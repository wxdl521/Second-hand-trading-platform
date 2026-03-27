package com.syxs.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpSendRequest {

    @NotBlank(message = "Phone is required")
    private String phone;
}
