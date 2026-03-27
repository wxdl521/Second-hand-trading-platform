package com.syxs.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    @NotBlank(message = "Nickname is required")
    private String nickname;

    @NotBlank(message = "City is required")
    private String city;

    private String bio;

    private String avatar;
}
