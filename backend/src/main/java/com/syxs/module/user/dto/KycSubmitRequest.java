package com.syxs.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KycSubmitRequest {

    @NotBlank(message = "Real name is required")
    private String realName;

    @NotBlank(message = "ID number is required")
    private String idNumber;
}
