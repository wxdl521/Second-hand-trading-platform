package com.syxs.module.user.dto;

import com.syxs.common.enums.KycLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileDTO {

    private Long id;
    private String nickname;
    private String phone;
    private String city;
    private String avatar;
    private KycLevel kycLevel;
    private String bio;
    private String role;
    private Integer carbonPoints;
    private String token;
    private String accessToken;
    private String refreshToken;
}
