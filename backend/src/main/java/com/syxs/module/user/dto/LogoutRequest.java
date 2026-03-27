package com.syxs.module.user.dto;

import lombok.Data;

@Data
public class LogoutRequest {

    private String refreshToken;
}
